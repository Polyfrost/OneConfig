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
pub enum ListError {
	#[error("Unable to query database: {0}")]
	Database(#[from] sea_orm::error::DbErr),
}

impl IntoResponse for ListError {
	fn into_response(self) -> axum::response::Response {
		(
			match self {
				Self::Database(_) => StatusCode::INTERNAL_SERVER_ERROR,
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

#[derive(Debug, Serialize, JsonSchema)]
pub struct ListResponse {
	collections: Vec<CollectionInfo>,
}

fn endpoint_doc(op: TransformOperation) -> TransformOperation {
	op.id("listCollections")
		.summary("List all collections")
		.description("Lists every collection.")
		.tag("collections")
}

pub(super) fn router() -> ApiRouter<ApiState> {
	ApiRouter::new().api_route("/list", get_with(self::endpoint, self::endpoint_doc))
}

#[tracing::instrument(level = "debug", skip(state))]
async fn endpoint(
	State(state): State<ApiState>,
) -> Result<Json<ListResponse>, ListError> {
	use entities::prelude::*;

	let collections = Collections::find()
		.all(&state.database)
		.await?
		.into_iter()
		.map(CollectionInfo::from_collection)
		.collect();

	Ok(Json(ListResponse { collections }))
}
