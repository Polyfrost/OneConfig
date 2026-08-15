use aide::{
	OperationInput, OperationIo,
	axum::{ApiRouter, routing::get_with},
	openapi::SecurityRequirement,
	transform::TransformOperation,
};
use axum::{
	Json,
	extract::{FromRequestParts, Query, State},
	http::{StatusCode, request::Parts},
	response::{IntoResponse, Response},
};
use chrono::{Days, NaiveDate, Utc};
use entities::{
	daily_playtime, monthly_active_login, player_owned_cosmetic, prelude::*,
	sea_orm_active_enums::PlayerRole, user,
};
use schemars::JsonSchema;
use sea_orm::{
	ColumnTrait as _, EntityTrait, FromQueryResult, PaginatorTrait as _, QueryFilter,
	QuerySelect, Select,
	prelude::DateTimeWithTimeZone,
	sea_query::{Alias, Expr, Func},
};
use serde::{Deserialize, Serialize};

use crate::{
	api::{
		ApiState,
		account::{AuthenticatedPlayer, OPENAPI_SECURITY_NAME, role_at_least},
	},
	database::current_utc_month,
};

#[derive(Debug)]
pub struct PrivateAnalyticsAuth;

#[derive(Debug, PartialEq, Eq)]
enum PrivateAuthCredential<'a> {
	AdminPassword,
	Bearer(&'a str),
	MissingOrInvalid,
}

fn classify_authorization_header<'a>(
	header: Option<&'a str>,
	admin_password: &str,
) -> PrivateAuthCredential<'a> {
	match header {
		Some(value) if value == admin_password => PrivateAuthCredential::AdminPassword,
		Some(value) => value
			.strip_prefix("Bearer ")
			.map(PrivateAuthCredential::Bearer)
			.unwrap_or(PrivateAuthCredential::MissingOrInvalid),
		None => PrivateAuthCredential::MissingOrInvalid,
	}
}

fn is_admin_role(role: &PlayerRole) -> bool {
	role_at_least(role, &PlayerRole::Admin)
}

impl OperationInput for PrivateAnalyticsAuth {
	fn operation_input(
		_ctx: &mut aide::generate::GenContext,
		operation: &mut aide::openapi::Operation,
	) {
		operation.security.extend([
			SecurityRequirement::from([("Admin Password".to_string(), Vec::new())]),
			SecurityRequirement::from([(OPENAPI_SECURITY_NAME.to_string(), Vec::new())]),
		]);
	}
}

impl FromRequestParts<ApiState> for PrivateAnalyticsAuth {
	type Rejection = Response;

	async fn from_request_parts(
		parts: &mut Parts,
		state: &ApiState,
	) -> Result<Self, Self::Rejection> {
		let auth_header = parts
			.headers
			.get("Authorization")
			.and_then(|h| h.to_str().ok());

		match classify_authorization_header(auth_header, &state.admin_password) {
			PrivateAuthCredential::AdminPassword => Ok(Self),
			PrivateAuthCredential::Bearer(_) => {
				let player = AuthenticatedPlayer::from_request_parts(parts, state)
					.await?
					.0;
				if is_admin_role(&player.role) {
					Ok(Self)
				} else {
					Err((
						StatusCode::FORBIDDEN,
						"Authenticated player does not have permission",
					)
						.into_response())
				}
			}
			PrivateAuthCredential::MissingOrInvalid => Err((
				StatusCode::UNAUTHORIZED,
				"Invalid or missing analytics authorization",
			)
				.into_response()),
		}
	}
}

#[derive(thiserror::Error, Debug, OperationIo)]
pub enum AnalyticsError {
	#[error("Unable to query analytics data: {0}")]
	Database(#[from] sea_orm::error::DbErr),
	#[error("`start` ({start}) is after `end` ({end})")]
	InvalidPeriod { start: NaiveDate, end: NaiveDate },
}

impl IntoResponse for AnalyticsError {
	fn into_response(self) -> axum::response::Response {
		(
			match self {
				Self::Database(_) => StatusCode::INTERNAL_SERVER_ERROR,
				Self::InvalidPeriod { .. } => StatusCode::BAD_REQUEST,
			},
			self.to_string(),
		)
			.into_response()
	}
}

#[derive(Debug, Default, Clone, Copy, Deserialize, Serialize, JsonSchema)]
pub struct AnalyticsPeriod {
	start: Option<NaiveDate>,
	end: Option<NaiveDate>,
}

impl AnalyticsPeriod {
	fn validate(self) -> Result<Self, AnalyticsError> {
		match (self.start, self.end) {
			(Some(start), Some(end)) if start > end => {
				Err(AnalyticsError::InvalidPeriod { start, end })
			}
			_ => Ok(self),
		}
	}

	fn is_unbounded(self) -> bool {
		self.start.is_none() && self.end.is_none()
	}

	fn start_timestamp(self) -> Option<DateTimeWithTimeZone> {
		self.start.map(start_of_day)
	}

	fn end_timestamp_exclusive(self) -> Option<DateTimeWithTimeZone> {
		self.end.and_then(|end| end.succ_opt()).map(start_of_day)
	}
}

fn start_of_day(day: NaiveDate) -> DateTimeWithTimeZone {
	day.and_hms_opt(0, 0, 0)
		.expect("midnight is a valid time of day")
		.and_utc()
		.fixed_offset()
}

fn filter_timestamp_period<E: EntityTrait>(
	mut select: Select<E>,
	column: E::Column,
	period: AnalyticsPeriod,
) -> Select<E> {
	if let Some(start) = period.start_timestamp() {
		select = select.filter(column.gte(start));
	}
	if let Some(end) = period.end_timestamp_exclusive() {
		select = select.filter(column.lt(end));
	}
	select
}

fn filter_date_period<E: EntityTrait>(
	mut select: Select<E>,
	column: E::Column,
	period: AnalyticsPeriod,
) -> Select<E> {
	if let Some(start) = period.start {
		select = select.filter(column.gte(start));
	}
	if let Some(end) = period.end {
		select = select.filter(column.lte(end));
	}
	select
}

fn endpoint_doc(op: TransformOperation) -> TransformOperation {
	op.id("getAnalyticsOverview")
		.summary("Get private analytics overview")
		.description(
			"Returns private aggregate analytics for users, MAU, owned items, and \
			 playtime.\n\nBoth `start` and `end` are optional inclusive `YYYY-MM-DD` UTC \
			 days, and either can be given on its own. Cumulative metrics (`total_users`, \
			 owned items and their distribution) are snapshots of the state at the end of \
			 the period, while flow metrics (`new_users`, `items_acquired`, playtime and \
			 sessions) only count activity inside the period. Without either parameter \
			 the response is the all-time overview.",
		)
		.tag("analytics")
}

#[derive(Debug, Serialize, JsonSchema)]
struct AnalyticsOverviewResponse {
	period: AnalyticsPeriod,
	total_users: i64,
	new_users: i64,
	monthly_active_users: i64,
	owned_items_per_user: OwnedItemsPerUser,
	playtime: Playtime,
}

#[derive(Debug, Serialize, JsonSchema)]
struct Playtime {
	total_seconds: i64,
	average_seconds_per_user: f64,
	last_30d_seconds: i64,
	total_sessions: i64,
}

#[derive(Debug, Serialize, JsonSchema)]
struct OwnedItemsPerUser {
	total_owned_items: i64,
	average_per_user: f64,
	users_with_any: i64,
	items_acquired: i64,
	distribution: OwnedItemsDistribution,
}

#[derive(Debug, Serialize, JsonSchema)]
struct OwnedItemsDistribution {
	#[serde(rename = "0")]
	zero: i64,
	#[serde(rename = "1")]
	one: i64,
	#[serde(rename = "2_5")]
	two_5: i64,
	#[serde(rename = "6_10")]
	six_10: i64,
	#[serde(rename = "11_plus")]
	eleven_plus: i64,
}

#[derive(Debug, Default, FromQueryResult)]
struct DistinctCount {
	count: i64,
}

#[derive(Debug, FromQueryResult)]
struct OwnedItemsCounts {
	total_owned_items: i64,
	users_with_any: i64,
	zero: i64,
	one: i64,
	two_5: i64,
	six_10: i64,
	eleven_plus: i64,
}

#[derive(Debug, FromQueryResult)]
struct PlayerOwnedCount {
	player_id: i32,
	count: i64,
}

#[derive(Debug, Default, FromQueryResult)]
struct PlaytimeAggregate {
	total_seconds: Option<i64>,
	total_sessions: Option<i64>,
}

impl OwnedItemsCounts {
	fn from_player_counts(
		total_users: i64,
		player_counts: impl IntoIterator<Item = i64>,
	) -> Self {
		let mut counts = Self {
			total_owned_items: 0,
			users_with_any: 0,
			zero: total_users,
			one: 0,
			two_5: 0,
			six_10: 0,
			eleven_plus: 0,
		};

		for owned_count in player_counts {
			counts.total_owned_items += owned_count;

			if owned_count > 0 {
				counts.users_with_any += 1;
				counts.zero -= 1;
			}

			match owned_count {
				1 => counts.one += 1,
				2..=5 => counts.two_5 += 1,
				6..=10 => counts.six_10 += 1,
				11.. => counts.eleven_plus += 1,
				_ => {}
			}
		}

		counts
	}
}

pub(super) async fn setup_router() -> ApiRouter<ApiState> {
	ApiRouter::new().api_route(
		"/analytics/overview",
		get_with(self::endpoint, self::endpoint_doc),
	)
}

#[tracing::instrument(level = "debug", skip(state))]
async fn endpoint(
	State(state): State<ApiState>,
	_auth: PrivateAnalyticsAuth,
	Query(period): Query<AnalyticsPeriod>,
) -> Result<Json<AnalyticsOverviewResponse>, AnalyticsError> {
	let period = period.validate()?;
	let up_to_end = AnalyticsPeriod {
		start: None,
		end: period.end,
	};

	let total_users =
		filter_timestamp_period(User::find(), user::Column::CreatedAt, up_to_end)
			.count(&state.database)
			.await? as i64;
	let new_users = filter_timestamp_period(User::find(), user::Column::CreatedAt, period)
		.count(&state.database)
		.await? as i64;

	let monthly_active_users = if period.is_unbounded() {
		MonthlyActiveLogin::find()
			.filter(monthly_active_login::Column::Month.eq(current_utc_month()))
			.count(&state.database)
			.await? as i64
	} else {
		let mut query = MonthlyActiveLogin::find().select_only().column_as(
			Expr::expr(Func::count_distinct(Expr::col(
				monthly_active_login::Column::PlayerId,
			))),
			"count",
		);
		if let Some(start) = period.start_timestamp() {
			query = query.filter(monthly_active_login::Column::LastLoginAt.gte(start));
		}
		if let Some(end) = period.end_timestamp_exclusive() {
			query = query.filter(monthly_active_login::Column::FirstLoginAt.lt(end));
		}
		query
			.into_model::<DistinctCount>()
			.one(&state.database)
			.await?
			.unwrap_or_default()
			.count
	};

	let cosmetic_counts = filter_timestamp_period(
		PlayerOwnedCosmetic::find(),
		player_owned_cosmetic::Column::AcquiredAt,
		up_to_end,
	)
	.select_only()
	.column(player_owned_cosmetic::Column::PlayerId)
	.column_as(player_owned_cosmetic::Column::CosmeticId.count(), "count")
	.group_by(player_owned_cosmetic::Column::PlayerId)
	.into_model::<PlayerOwnedCount>()
	.all(&state.database)
	.await?;

	let mut owned_by_player = std::collections::HashMap::new();
	for count in cosmetic_counts {
		*owned_by_player.entry(count.player_id).or_insert(0) += count.count;
	}
	let owned_counts =
		OwnedItemsCounts::from_player_counts(total_users, owned_by_player.into_values());

	let items_acquired = filter_timestamp_period(
		PlayerOwnedCosmetic::find(),
		player_owned_cosmetic::Column::AcquiredAt,
		period,
	)
	.count(&state.database)
	.await? as i64;

	let average_per_user = if total_users == 0 {
		0.0
	} else {
		owned_counts.total_owned_items as f64 / total_users as f64
	};

	let playtime_totals =
		filter_date_period(DailyPlaytime::find(), daily_playtime::Column::Day, period)
			.select_only()
			.column_as(
				daily_playtime::Column::TotalSeconds
					.sum()
					.cast_as(Alias::new("bigint")),
				"total_seconds",
			)
			.column_as(
				daily_playtime::Column::SessionCount
					.sum()
					.cast_as(Alias::new("bigint")),
				"total_sessions",
			)
			.into_model::<PlaytimeAggregate>()
			.one(&state.database)
			.await?
			.unwrap_or_default();

	let thirty_days_ago = (Utc::now() - Days::new(30)).date_naive();
	let last_30d_seconds = DailyPlaytime::find()
		.select_only()
		.column_as(
			daily_playtime::Column::TotalSeconds
				.sum()
				.cast_as(Alias::new("bigint")),
			"total_seconds",
		)
		.filter(daily_playtime::Column::Day.gte(thirty_days_ago))
		.into_model::<PlaytimeAggregate>()
		.one(&state.database)
		.await?
		.unwrap_or_default()
		.total_seconds
		.unwrap_or(0);

	let total_playtime_seconds = playtime_totals.total_seconds.unwrap_or(0);
	let average_seconds_per_user = if total_users == 0 {
		0.0
	} else {
		total_playtime_seconds as f64 / total_users as f64
	};

	Ok(Json(AnalyticsOverviewResponse {
		period,
		total_users,
		new_users,
		monthly_active_users,
		playtime: Playtime {
			total_seconds: total_playtime_seconds,
			average_seconds_per_user,
			last_30d_seconds,
			total_sessions: playtime_totals.total_sessions.unwrap_or(0),
		},
		owned_items_per_user: OwnedItemsPerUser {
			total_owned_items: owned_counts.total_owned_items,
			average_per_user,
			users_with_any: owned_counts.users_with_any,
			items_acquired,
			distribution: OwnedItemsDistribution {
				zero: owned_counts.zero,
				one: owned_counts.one,
				two_5: owned_counts.two_5,
				six_10: owned_counts.six_10,
				eleven_plus: owned_counts.eleven_plus,
			},
		},
	}))
}

#[cfg(test)]
mod tests {
	use chrono::NaiveDate;
	use entities::sea_orm_active_enums::PlayerRole;

	use super::{
		AnalyticsError, AnalyticsPeriod, PrivateAuthCredential,
		classify_authorization_header, is_admin_role,
	};

	fn date(year: i32, month: u32, day: u32) -> NaiveDate {
		NaiveDate::from_ymd_opt(year, month, day).expect("valid date")
	}

	fn period(start: Option<NaiveDate>, end: Option<NaiveDate>) -> AnalyticsPeriod {
		AnalyticsPeriod { start, end }
	}

	#[test]
	fn period_rejects_start_after_end() {
		let invalid = period(Some(date(2026, 7, 2)), Some(date(2026, 7, 1)));
		assert!(matches!(
			invalid.validate(),
			Err(AnalyticsError::InvalidPeriod { .. })
		));

		for valid in [
			period(Some(date(2026, 7, 1)), Some(date(2026, 7, 1))),
			period(Some(date(2026, 7, 1)), None),
			period(None, Some(date(2026, 7, 1))),
			period(None, None),
		] {
			assert!(valid.validate().is_ok());
		}
	}

	#[test]
	fn only_a_period_without_bounds_is_unbounded() {
		assert!(period(None, None).is_unbounded());
		assert!(!period(Some(date(2026, 7, 1)), None).is_unbounded());
		assert!(!period(None, Some(date(2026, 7, 1))).is_unbounded());
	}

	#[test]
	fn period_bounds_cover_whole_days() {
		let period = period(Some(date(2026, 7, 1)), Some(date(2026, 7, 31)));

		assert_eq!(
			period.start_timestamp().map(|ts| ts.to_rfc3339()),
			Some("2026-07-01T00:00:00+00:00".to_string())
		);
		assert_eq!(
			period.end_timestamp_exclusive().map(|ts| ts.to_rfc3339()),
			Some("2026-08-01T00:00:00+00:00".to_string())
		);
	}

	#[test]
	fn missing_period_bounds_produce_no_timestamps() {
		let period = period(None, None);

		assert!(period.start_timestamp().is_none());
		assert!(period.end_timestamp_exclusive().is_none());
	}

	#[test]
	fn auth_header_accepts_admin_password() {
		assert_eq!(
			classify_authorization_header(Some("secret"), "secret"),
			PrivateAuthCredential::AdminPassword
		);
	}

	#[test]
	fn auth_header_accepts_bearer_token() {
		assert_eq!(
			classify_authorization_header(Some("Bearer token"), "secret"),
			PrivateAuthCredential::Bearer("token")
		);
	}

	#[test]
	fn auth_header_rejects_missing_or_invalid_values() {
		assert_eq!(
			classify_authorization_header(None, "secret"),
			PrivateAuthCredential::MissingOrInvalid
		);
		assert_eq!(
			classify_authorization_header(Some("token"), "secret"),
			PrivateAuthCredential::MissingOrInvalid
		);
	}

	#[test]
	fn only_admin_role_can_use_bearer_analytics_auth() {
		assert!(is_admin_role(&PlayerRole::Admin));
		assert!(!is_admin_role(&PlayerRole::Moderator));
		assert!(!is_admin_role(&PlayerRole::Player));
	}
}
