use aide::{
	OperationIo,
	axum::{ApiRouter, routing::get_with},
	transform::TransformOperation,
};
use axum::{Json, extract::State, http::StatusCode, response::IntoResponse};
use chrono::{DateTime, FixedOffset};
use entities::sea_orm_active_enums::TagType;
use schemars::JsonSchema;
use sea_orm::{ColumnTrait, EntityTrait, QueryFilter};
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

/// A single tag that may be applied to cosmetics.
#[derive(Debug, Serialize, JsonSchema)]
struct TagInfo {
	id: i32,
	name: String,
	display_name: Option<String>,
	description: Option<String>,
	tag_type: TagType,
	created_at: DateTime<FixedOffset>,
}

impl TagInfo {
	fn from_tag(tag: entities::tags::Model) -> Self {
		TagInfo {
			id: tag.id,
			name: tag.name,
			display_name: tag.display_name,
			description: tag.description,
			tag_type: tag.tag_type,
			created_at: tag.created_at,
		}
	}
}

#[derive(Debug, Serialize, JsonSchema)]
pub struct ListResponse {
	tags: Vec<TagInfo>,
}

fn endpoint_doc(op: TransformOperation) -> TransformOperation {
	op.id("listCategory")
		.summary("List all categories")
		.description("Lists every category.")
		.tag("tags")
}

pub(super) fn router() -> ApiRouter<ApiState> {
	ApiRouter::new().api_route("/list", get_with(self::endpoint, self::endpoint_doc))
}

#[tracing::instrument(level = "debug", skip(state))]
async fn endpoint(
	State(state): State<ApiState>,
) -> Result<Json<ListResponse>, ListError> {
	use entities::prelude::*;

	let tags = Tags::find()
		.filter(entities::tags::Column::TagType.eq(TagType::Category))
		.all(&state.database)
		.await?
		.into_iter()
		.map(TagInfo::from_tag)
		.collect();

	Ok(Json(ListResponse { tags }))
}
