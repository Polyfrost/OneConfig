use aide::{
	OperationInput, OperationIo,
	axum::{
		ApiRouter,
		routing::{delete_with, get_with, post_with},
	},
	transform::TransformOperation,
};
use axum::{
	Json,
	extract::{FromRequestParts, Path, State},
	http::{StatusCode, header, request::Parts},
	response::{IntoResponse, Redirect, Response},
};
use axum_client_ip::ClientIp;
use chrono::{DateTime, FixedOffset};
use schemars::JsonSchema;
use sea_orm::{
	ActiveModelTrait, ColumnTrait, EntityTrait, QueryFilter, QueryOrder, Set,
	TransactionTrait, TryInsertResult, sea_query::Expr,
};
use serde::{Deserialize, Serialize};
use sha2::{Digest, Sha256};

use crate::api::{ApiState, admin_auth::AdminAuthenticationExtractor};

#[derive(thiserror::Error, Debug, OperationIo)]
pub enum LinksError {
	#[error("No tracked link with that slug exists")]
	NotFound,
	#[error("A tracked link with that slug already exists")]
	SlugTaken,
	#[error("Slug must be non-empty and contain only a-z, 0-9, '-' or '_'")]
	InvalidSlug,
	#[error("Target url must be an absolute http(s) url")]
	InvalidUrl,
	#[error("Unable to query database: {0}")]
	Database(#[from] sea_orm::error::DbErr),
}

impl IntoResponse for LinksError {
	fn into_response(self) -> Response {
		(
			match self {
				Self::NotFound => StatusCode::NOT_FOUND,
				Self::SlugTaken => StatusCode::CONFLICT,
				Self::InvalidSlug | Self::InvalidUrl => StatusCode::BAD_REQUEST,
				Self::Database(_) => StatusCode::INTERNAL_SERVER_ERROR,
			},
			self.to_string(),
		)
			.into_response()
	}
}

struct VisitorId {
	ip: std::net::IpAddr,
	user_agent: String,
}

impl OperationInput for VisitorId {
	fn operation_input(
		_ctx: &mut aide::generate::GenContext,
		_operation: &mut aide::openapi::Operation,
	) {
	}
}

impl FromRequestParts<ApiState> for VisitorId {
	type Rejection = Response;

	async fn from_request_parts(
		parts: &mut Parts,
		state: &ApiState,
	) -> Result<Self, Self::Rejection> {
		let ClientIp(ip) = ClientIp::from_request_parts(parts, state)
			.await
			.map_err(IntoResponse::into_response)?;
		let user_agent = parts
			.headers
			.get(header::USER_AGENT)
			.and_then(|value| value.to_str().ok())
			.unwrap_or_default()
			.to_owned();
		Ok(Self { ip, user_agent })
	}
}

impl VisitorId {
	fn hash(&self, salt: &str, slug: &str) -> String {
		let mut hasher = Sha256::new();
		hasher.update(salt.as_bytes());
		hasher.update([0]);
		hasher.update(self.ip.to_string().as_bytes());
		hasher.update([0]);
		hasher.update(self.user_agent.as_bytes());
		hasher.update([0]);
		hasher.update(slug.as_bytes());
		hasher
			.finalize()
			.iter()
			.map(|byte| format!("{byte:02x}"))
			.collect()
	}
}

#[derive(Debug, Serialize, JsonSchema)]
pub struct LinkInfo {
	slug: String,
	target_url: String,
	clicks: i64,
	created_at: DateTime<FixedOffset>,
}

impl LinkInfo {
	fn from_model(link: entities::tracked_links::Model) -> Self {
		Self {
			slug: link.slug,
			target_url: link.target_url,
			clicks: link.clicks,
			created_at: link.created_at,
		}
	}
}

#[derive(Debug, Deserialize, JsonSchema)]
struct CreateRequest {
	slug: String,
	target_url: String,
}

#[derive(Debug, Serialize, JsonSchema)]
pub struct ListResponse {
	links: Vec<LinkInfo>,
}

fn valid_slug(slug: &str) -> bool {
	!slug.is_empty()
		&& slug.len() <= 128
		&& slug
			.chars()
			.all(|c| c.is_ascii_lowercase() || c.is_ascii_digit() || c == '-' || c == '_')
}

fn valid_target(url: &str) -> bool {
	(url.starts_with("https://") || url.starts_with("http://")) && url.len() <= 2048
}

const BOT_UA_MARKERS: &[&str] = &[
	"bot",
	"crawler",
	"spider",
	"slurp",
	"preview",
	"unfurl",
	"embed",
	"facebookexternalhit",
	"discord",
	"twitter",
	"telegram",
	"whatsapp",
	"slack",
	"skype",
	"linkedin",
	"pinterest",
	"redditbot",
	"mastodon",
	"curl",
	"wget",
	"python-requests",
	"go-http-client",
	"headless",
];

fn is_bot(user_agent: &str) -> bool {
	if user_agent.trim().is_empty() {
		return true;
	}
	let ua = user_agent.to_ascii_lowercase();
	BOT_UA_MARKERS.iter().any(|marker| ua.contains(marker))
}

fn redirect_doc(op: TransformOperation) -> TransformOperation {
	op.id("followTrackedLink")
		.summary("Follow a tracked link")
		.description(
			"Redirects to the link's target url, counting the visit if it is the \
			 visitor's first for this slug (deduplicated by a salted hash of ip + \
			 user-agent). Public. Returns 404 when no link with that slug exists.",
		)
		.tag("links")
}

fn create_doc(op: TransformOperation) -> TransformOperation {
	op.id("createTrackedLink")
		.summary("Create a tracked link")
		.description(
			"Creates a tracked short link redirecting `/go/{slug}` to an arbitrary \
			 absolute http(s) url. Admin password required.",
		)
		.tag("links")
}

fn list_doc(op: TransformOperation) -> TransformOperation {
	op.id("listTrackedLinks")
		.summary("List tracked links")
		.description(
			"Lists every tracked link with its unique-visit count. Admin password \
			 required.",
		)
		.tag("links")
}

fn delete_doc(op: TransformOperation) -> TransformOperation {
	op.id("deleteTrackedLink")
		.summary("Delete a tracked link")
		.description(
			"Deletes the tracked link with the given slug. Admin password required.",
		)
		.tag("links")
}

pub(super) async fn setup_router() -> ApiRouter<ApiState> {
	ApiRouter::new()
		.api_route("/go/{slug}", get_with(self::follow, self::redirect_doc))
		.api_route("/links", post_with(self::create, self::create_doc))
		.api_route("/links", get_with(self::list, self::list_doc))
		.api_route("/links/{slug}", delete_with(self::delete, self::delete_doc))
}

#[tracing::instrument(level = "debug", skip(state, visitor))]
async fn follow(
	State(state): State<ApiState>,
	Path(slug): Path<String>,
	visitor: VisitorId,
) -> Result<Redirect, LinksError> {
	use entities::{prelude::*, tracked_link_hits, tracked_links};

	let link = TrackedLinks::find_by_id(&slug)
		.one(&state.database)
		.await?
		.ok_or(LinksError::NotFound)?;

	if !is_bot(&visitor.user_agent) {
		let visitor_hash = visitor.hash(&state.admin_password, &slug);
		let txn = state.database.begin().await?;

		let inserted = TrackedLinkHits::insert(tracked_link_hits::ActiveModel {
			slug: Set(slug.clone()),
			visitor_hash: Set(visitor_hash),
			..Default::default()
		})
		.on_conflict_do_nothing()
		.exec(&txn)
		.await?;

		if matches!(inserted, TryInsertResult::Inserted(_)) {
			TrackedLinks::update_many()
				.col_expr(
					tracked_links::Column::Clicks,
					Expr::col(tracked_links::Column::Clicks).add(1),
				)
				.filter(tracked_links::Column::Slug.eq(&slug))
				.exec(&txn)
				.await?;
		}

		txn.commit().await?;
	}

	Ok(Redirect::temporary(&link.target_url))
}

#[tracing::instrument(level = "debug", skip(state, _auth))]
async fn create(
	State(state): State<ApiState>,
	_auth: AdminAuthenticationExtractor,
	Json(body): Json<CreateRequest>,
) -> Result<(StatusCode, Json<LinkInfo>), LinksError> {
	use entities::{prelude::*, tracked_links};

	if !valid_slug(&body.slug) {
		return Err(LinksError::InvalidSlug);
	}
	if !valid_target(&body.target_url) {
		return Err(LinksError::InvalidUrl);
	}

	if TrackedLinks::find_by_id(&body.slug)
		.one(&state.database)
		.await?
		.is_some()
	{
		return Err(LinksError::SlugTaken);
	}

	let link = tracked_links::ActiveModel {
		slug: Set(body.slug),
		target_url: Set(body.target_url),
		clicks: Set(0),
		..Default::default()
	}
	.insert(&state.database)
	.await?;

	Ok((StatusCode::CREATED, Json(LinkInfo::from_model(link))))
}

#[tracing::instrument(level = "debug", skip(state, _auth))]
async fn list(
	State(state): State<ApiState>,
	_auth: AdminAuthenticationExtractor,
) -> Result<Json<ListResponse>, LinksError> {
	use entities::{prelude::*, tracked_links};

	let links = TrackedLinks::find()
		.order_by_desc(tracked_links::Column::Clicks)
		.all(&state.database)
		.await?
		.into_iter()
		.map(LinkInfo::from_model)
		.collect();

	Ok(Json(ListResponse { links }))
}

#[tracing::instrument(level = "debug", skip(state, _auth))]
async fn delete(
	State(state): State<ApiState>,
	_auth: AdminAuthenticationExtractor,
	Path(slug): Path<String>,
) -> Result<StatusCode, LinksError> {
	use entities::prelude::*;

	let result = TrackedLinks::delete_by_id(slug)
		.exec(&state.database)
		.await?;

	if result.rows_affected == 0 {
		return Err(LinksError::NotFound);
	}

	Ok(StatusCode::NO_CONTENT)
}

#[cfg(test)]
mod tests {
	use std::net::IpAddr;

	use super::{VisitorId, is_bot, valid_slug, valid_target};

	fn visitor(ip: &str, user_agent: &str) -> VisitorId {
		VisitorId {
			ip: ip.parse::<IpAddr>().unwrap(),
			user_agent: user_agent.to_owned(),
		}
	}

	#[test]
	fn accepts_reasonable_slugs() {
		assert!(valid_slug("oneclient"));
		assert!(valid_slug("oneclient-twitter"));
		assert!(valid_slug("promo_2026"));
	}

	#[test]
	fn rejects_bad_slugs() {
		assert!(!valid_slug(""));
		assert!(!valid_slug("Has Space"));
		assert!(!valid_slug("UPPER"));
		assert!(!valid_slug("emoji-\u{1f600}"));
		assert!(!valid_slug(&"x".repeat(129)));
	}

	#[test]
	fn only_absolute_http_targets_allowed() {
		assert!(valid_target("https://polyfrost.org/projects/oneclient"));
		assert!(valid_target("http://example.com"));
		assert!(!valid_target("/projects/oneclient"));
		assert!(!valid_target("javascript:alert(1)"));
		assert!(!valid_target("data:text/html,x"));
		assert!(!valid_target(&format!(
			"https://x.com/{}",
			"a".repeat(2048)
		)));
	}

	#[test]
	fn browser_user_agents_are_not_bots() {
		let chrome = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) \
			AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";
		let firefox =
			"Mozilla/5.0 (X11; Linux x86_64; rv:121.0) Gecko/20100101 Firefox/121.0";
		assert!(!is_bot(chrome));
		assert!(!is_bot(firefox));
	}

	#[test]
	fn unfurl_and_scripted_agents_are_bots() {
		assert!(is_bot("")); // empty ua
		assert!(is_bot("   "));
		assert!(is_bot("Discordbot/2.0 (+https://discordapp.com)"));
		assert!(is_bot("Twitterbot/1.0"));
		assert!(is_bot("facebookexternalhit/1.1"));
		assert!(is_bot("TelegramBot (like TwitterBot)"));
		assert!(is_bot("Slackbot-LinkExpanding 1.0"));
		assert!(is_bot("curl/8.4.0"));
		assert!(is_bot("python-requests/2.31.0"));
	}

	#[test]
	fn hash_is_stable_for_same_visitor_and_slug() {
		let v = visitor("203.0.113.7", "Chrome");
		assert_eq!(
			v.hash("salt", "oneclient"),
			v.hash("salt", "oneclient"),
			"same visitor + slug must hash identically so repeats dedupe",
		);
	}

	#[test]
	fn hash_differs_across_visitor_slug_and_salt() {
		let a = visitor("203.0.113.7", "Chrome");
		let b = visitor("203.0.113.8", "Chrome"); // different ip
		let c = visitor("203.0.113.7", "Firefox"); // different ua

		assert_ne!(a.hash("salt", "oneclient"), b.hash("salt", "oneclient"));
		assert_ne!(a.hash("salt", "oneclient"), c.hash("salt", "oneclient"));
		assert_ne!(a.hash("salt", "oneclient"), a.hash("salt", "oneconfig"));
		assert_ne!(a.hash("salt", "oneclient"), a.hash("other", "oneclient"));
	}

	#[test]
	fn hash_is_hex_sha256() {
		let h = visitor("203.0.113.7", "Chrome").hash("salt", "oneclient");
		assert_eq!(h.len(), 64);
		assert!(h.chars().all(|c| c.is_ascii_hexdigit()));
	}
}
