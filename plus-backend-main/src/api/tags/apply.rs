use aide::{
	OperationIo,
	axum::{ApiRouter, routing::post_with},
	transform::TransformOperation,
};
use axum::{Json, extract::State, http::StatusCode, response::IntoResponse};
use schemars::JsonSchema;
use sea_orm::{EntityTrait, Set, TransactionTrait};
use serde::Deserialize;

use crate::api::{ApiState, admin_auth::AdminAuthenticationExtractor};

#[derive(thiserror::Error, Debug, OperationIo)]
pub enum ApplyError {
	#[error("The requested tag does not exist")]
	MissingTag,
	#[error("The requested cosmetic does not exist")]
	MissingCosmetic,
	#[error("Unable to query database: {0}")]
	Database(#[from] sea_orm::error::DbErr),
}

impl IntoResponse for ApplyError {
	fn into_response(self) -> axum::response::Response {
		(
			match self {
				Self::MissingTag | Self::MissingCosmetic => StatusCode::NOT_FOUND,
				Self::Database(_) => StatusCode::INTERNAL_SERVER_ERROR,
			},
			self.to_string(),
		)
			.into_response()
	}
}

#[derive(Debug, Deserialize, JsonSchema)]
struct ApplyRequest {
	tag_id: i32,
	cosmetic_ids: Vec<i32>,
}

fn endpoint_doc(op: TransformOperation) -> TransformOperation {
	op.id("applyTag")
		.summary("Apply a tag to cosmetics")
		.description(
			"Applies a tag to each of the given cosmetics. A cosmetic that belongs \
			 to a group tags every variant of that group, matching how grants and \
			 the catalog treat groups. Already-tagged cosmetics are left alone. \
			 Admin password required.",
		)
		.tag("tags")
}

pub(super) fn router() -> ApiRouter<ApiState> {
	ApiRouter::new().api_route("/apply", post_with(self::endpoint, self::endpoint_doc))
}

#[tracing::instrument(level = "debug", skip(state, _auth))]
async fn endpoint(
	State(state): State<ApiState>,
	_auth: AdminAuthenticationExtractor,
	Json(body): Json<ApplyRequest>,
) -> Result<StatusCode, ApplyError> {
	use entities::{prelude::*, tags_cosmetic};

	if body.cosmetic_ids.is_empty() {
		return Ok(StatusCode::NO_CONTENT);
	}

	let txn = state.database.begin().await?;
	if Tags::find_by_id(body.tag_id).one(&txn).await?.is_none() {
		return Err(ApplyError::MissingTag);
	}

	let Some(cosmetic_ids) = super::expand_groups(&txn, &body.cosmetic_ids).await? else {
		return Err(ApplyError::MissingCosmetic);
	};

	TagsCosmetic::insert_many(cosmetic_ids.into_iter().map(|cosmetic_id| {
		tags_cosmetic::ActiveModel {
			tag_id: Set(body.tag_id),
			cosmetic_id: Set(cosmetic_id),
		}
	}))
	.on_conflict_do_nothing()
	.exec(&txn)
	.await?;
	txn.commit().await?;

	Ok(StatusCode::NO_CONTENT)
}
