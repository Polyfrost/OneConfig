use chrono::{DateTime, Datelike, Days, NaiveDate, Utc};
use entities::{
	daily_playtime, monthly_active_login,
	prelude::*,
	sea_orm_active_enums::{TransactionProvider, TransactionStatus},
	transaction, user,
};
use sea_orm::{
	ActiveValue, DbErr, EntityTrait, QueryFilter, Set,
	prelude::*,
	sea_query::{Expr, OnConflict},
};
use uuid::Uuid;

pub(crate) trait DatabaseUserExt {
	/// Gets a [user::Model] given a specific Minecraft UUID, or else inserts a
	/// new user into the database.
	async fn get_or_create(
		db: &impl ConnectionTrait,
		minecraft_uuid: Uuid,
	) -> Result<user::Model, DbErr>;
}

pub(crate) trait DatabaseTransactionExt {
	async fn get_or_create_stripe(
		db: &impl ConnectionTrait,
		player_id: i32,
		buyer_id: Option<i32>,
		transaction_id: &str,
		raw_metadata: serde_json::Value,
	) -> Result<transaction::Model, DbErr>;
}

impl DatabaseUserExt for User {
	async fn get_or_create(
		db: &impl ConnectionTrait,
		minecraft_uuid: Uuid,
	) -> Result<user::Model, DbErr> {
		let existing = User::find()
			.filter(user::Column::MinecraftUuid.eq(minecraft_uuid))
			.one(db)
			.await?;

		Ok(match existing {
			Some(model) => model,
			None => {
				User::insert(user::ActiveModel {
					minecraft_uuid: ActiveValue::Set(minecraft_uuid),
					..Default::default()
				})
				.exec_with_returning(db)
				.await?
			}
		})
	}
}

impl DatabaseTransactionExt for Transaction {
	async fn get_or_create_stripe(
		db: &impl ConnectionTrait,
		player_id: i32,
		buyer_id: Option<i32>,
		stripe_payment_id: &str,
		raw_metadata: serde_json::Value,
	) -> Result<transaction::Model, DbErr> {
		if let Some(existing) = Transaction::find()
			.filter(transaction::Column::Provider.eq(TransactionProvider::Stripe))
			.filter(transaction::Column::StripePaymentId.eq(stripe_payment_id))
			.one(db)
			.await?
		{
			return Ok(existing);
		}

		Transaction::insert(transaction::ActiveModel {
			player_id: ActiveValue::Set(player_id),
			provider: ActiveValue::Set(TransactionProvider::Stripe),
			stripe_payment_id: ActiveValue::Set(Some(stripe_payment_id.to_string())),
			status: ActiveValue::Set(TransactionStatus::Completed),
			buyer: ActiveValue::Set(buyer_id),
			raw_metadata: ActiveValue::Set(raw_metadata),
			..Default::default()
		})
		.exec_with_returning(db)
		.await
	}
}

pub(crate) fn current_utc_month() -> Date {
	Utc::now()
		.date_naive()
		.with_day(1)
		.expect("every month has a first day")
}

pub(crate) async fn record_monthly_active_login(
	db: &impl ConnectionTrait,
	player_id: i32,
) -> Result<(), DbErr> {
	MonthlyActiveLogin::insert(monthly_active_login::ActiveModel {
		player_id: Set(player_id),
		month: Set(current_utc_month()),
		first_login_at: ActiveValue::NotSet,
		last_login_at: ActiveValue::NotSet,
		login_count: Set(1),
	})
	.on_conflict(
		OnConflict::columns([
			monthly_active_login::Column::PlayerId,
			monthly_active_login::Column::Month,
		])
		.value(
			monthly_active_login::Column::LastLoginAt,
			Expr::current_timestamp(),
		)
		.value(
			monthly_active_login::Column::LoginCount,
			Expr::col((
				monthly_active_login::Entity,
				monthly_active_login::Column::LoginCount,
			))
			.add(1),
		)
		.to_owned(),
	)
	.exec_without_returning(db)
	.await?;

	Ok(())
}

pub(crate) async fn accrue_playtime(
	db: &impl ConnectionTrait,
	player_id: i32,
	from: DateTime<Utc>,
	to: DateTime<Utc>,
	end_session: bool,
) -> Result<(), DbErr> {
	let mut cursor = from;
	while cursor < to {
		let day = cursor.date_naive();
		let next_midnight = (day + Days::new(1))
			.and_hms_opt(0, 0, 0)
			.expect("midnight is a valid time")
			.and_utc();
		let segment_end = next_midnight.min(to);
		let seconds = (segment_end - cursor).num_seconds().max(0);
		let is_final = segment_end >= to;
		upsert_daily_playtime(db, player_id, day, seconds, end_session && is_final)
			.await?;
		cursor = segment_end;
	}

	if end_session && from >= to {
		upsert_daily_playtime(db, player_id, to.date_naive(), 0, true).await?;
	}

	Ok(())
}

async fn upsert_daily_playtime(
	db: &impl ConnectionTrait,
	player_id: i32,
	day: NaiveDate,
	seconds: i64,
	increment_session: bool,
) -> Result<(), DbErr> {
	let session_delta = i32::from(increment_session);

	DailyPlaytime::insert(daily_playtime::ActiveModel {
		player_id: Set(player_id),
		day: Set(day),
		total_seconds: Set(seconds),
		session_count: Set(session_delta),
	})
	.on_conflict(
		OnConflict::columns([
			daily_playtime::Column::PlayerId,
			daily_playtime::Column::Day,
		])
		.value(
			daily_playtime::Column::TotalSeconds,
			Expr::col((daily_playtime::Entity, daily_playtime::Column::TotalSeconds))
				.add(seconds),
		)
		.value(
			daily_playtime::Column::SessionCount,
			Expr::col((daily_playtime::Entity, daily_playtime::Column::SessionCount))
				.add(session_delta),
		)
		.to_owned(),
	)
	.exec_without_returning(db)
	.await?;

	Ok(())
}
