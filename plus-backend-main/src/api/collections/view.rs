use aide::{
	OperationIo,
	axum::{ApiRouter, routing::get_with},
	transform::TransformOperation,
};
use axum::{Json, extract::State, http::StatusCode, response::IntoResponse};
use chrono::{DateTime, FixedOffset};
use schemars::JsonSchema;
use sea_orm::EntityTrait;
use serde::Serialize;

use crate::api::ApiState;

#[derive(thiserror::Error, Debug, OperationIo)]
pub enum ViewError {
	#[error("Unable to query database: {0}")]
	Database(#[from] sea_orm::error::DbErr),
	#[error("Couldn't find this collection")]
	NotFound,
}

impl IntoResponse for ViewError {
	fn into_response(self) -> axum::response::Response {
		(
			match self {
				Self::Database(_) => StatusCode::INTERNAL_SERVER_ERROR,
				Self::NotFound => StatusCode::NOT_FOUND,
			},
			self.to_string(),
		)
			.into_response()
	}
}

/// A single collection that cosmetics may belong to.
#[derive(Debug, Serialize, JsonSchema)]
struct CollectionInfo {
	id: i32,
	name: String,
	description: Option<String>,
	asset_id: Option<i32>,
	created_at: DateTime<FixedOffset>,
}

impl CollectionInfo {
	fn from_collection(collection: entities::collections::Model) -> Self {
		CollectionInfo {
			id: collection.id,
			name: collection.name,
			description: collection.description,
			asset_id: collection.asset_id,
			created_at: collection.created_at,
		}
	}
}

fn endpoint_doc(op: TransformOperation) -> TransformOperation {
	op.id("viewCollection")
		.summary("View a collections")
		.description("View a collection by its ID.")
		.tag("collections")
}

pub(super) fn router() -> ApiRouter<ApiState> {
	ApiRouter::new().api_route("/view/{id}", get_with(self::endpoint, self::endpoint_doc))
}

#[tracing::instrument(level = "debug", skip(state))]
async fn endpoint(
	State(state): State<ApiState>,
	axum::extract::Path(id): axum::extract::Path<i32>,
) -> Result<Json<CollectionInfo>, ViewError> {
	use entities::prelude::*;

	let collection = Collections::find_by_id(id)
		.one(&state.database)
		.await?
		.map(CollectionInfo::from_collection)
		.ok_or(ViewError::NotFound)?;

	Ok(Json(collection))
}
