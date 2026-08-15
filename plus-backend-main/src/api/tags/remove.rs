use aide::{
	OperationIo,
	axum::{ApiRouter, routing::post_with},
	transform::TransformOperation,
};
use axum::{Json, extract::State, http::StatusCode, response::IntoResponse};
use schemars::JsonSchema;
use sea_orm::{ColumnTrait, EntityTrait, QueryFilter, TransactionTrait};
use serde::Deserialize;

use crate::api::{ApiState, admin_auth::AdminAuthenticationExtractor};

#[derive(thiserror::Error, Debug, OperationIo)]
pub enum RemoveError {
	#[error("The requested tag does not exist")]
	MissingTag,
	#[error("The requested cosmetic does not exist")]
	MissingCosmetic,
	#[error("Unable to query database: {0}")]
	Database(#[from] sea_orm::error::DbErr),
}

impl IntoResponse for RemoveError {
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
struct RemoveRequest {
	tag_id: i32,
	cosmetic_ids: Vec<i32>,
}

fn endpoint_doc(op: TransformOperation) -> TransformOperation {
	op.id("removeTag")
		.summary("Remove a tag from cosmetics")
		.description(
			"Removes a tag from each of the given cosmetics. A cosmetic that \
			 belongs to a group untags every variant of that group, mirroring how \
			 the tag was applied. Cosmetics that do not carry the tag are left \
			 alone. Admin password required.",
		)
		.tag("tags")
}

pub(super) fn router() -> ApiRouter<ApiState> {
	ApiRouter::new().api_route("/remove", post_with(self::endpoint, self::endpoint_doc))
}

#[tracing::instrument(level = "debug", skip(state, _auth))]
async fn endpoint(
	State(state): State<ApiState>,
	_auth: AdminAuthenticationExtractor,
	Json(body): Json<RemoveRequest>,
) -> Result<StatusCode, RemoveError> {
	use entities::{prelude::*, tags_cosmetic};

	if body.cosmetic_ids.is_empty() {
		return Ok(StatusCode::NO_CONTENT);
	}

	let txn = state.database.begin().await?;
	if Tags::find_by_id(body.tag_id).one(&txn).await?.is_none() {
		return Err(RemoveError::MissingTag);
	}

	let Some(cosmetic_ids) = super::expand_groups(&txn, &body.cosmetic_ids).await? else {
		return Err(RemoveError::MissingCosmetic);
	};

	TagsCosmetic::delete_many()
		.filter(tags_cosmetic::Column::TagId.eq(body.tag_id))
		.filter(tags_cosmetic::Column::CosmeticId.is_in(cosmetic_ids))
		.exec(&txn)
		.await?;
	txn.commit().await?;

	Ok(StatusCode::NO_CONTENT)
}
