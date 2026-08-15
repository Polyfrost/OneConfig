use aide::{
	OperationIo,
	axum::{ApiRouter, routing::post_with},
	transform::TransformOperation,
};
use axum::{Json, extract::State, http::StatusCode, response::IntoResponse};
use chrono::{DateTime, FixedOffset};
use entities::sea_orm_active_enums::TagType;
use schemars::JsonSchema;
use sea_orm::{ActiveModelTrait, Set};
use serde::{Deserialize, Serialize};

use crate::api::{ApiState, admin_auth::AdminAuthenticationExtractor};

#[derive(thiserror::Error, Debug, OperationIo)]
pub enum CreateError {
	#[error("Unable to query database: {0}")]
	Database(#[from] sea_orm::error::DbErr),
}

impl IntoResponse for CreateError {
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

#[derive(Debug, Deserialize, JsonSchema)]
struct CreateRequest {
	name: String,
	display_name: Option<String>,
	description: Option<String>,
	tag_type: TagType,
}

/// The tag created by the request.
#[derive(Debug, Serialize, JsonSchema)]
pub struct CreateResponse {
	id: i32,
	name: String,
	display_name: Option<String>,
	description: Option<String>,
	tag_type: TagType,
	created_at: DateTime<FixedOffset>,
}

fn endpoint_doc(op: TransformOperation) -> TransformOperation {
	op.id("createTag")
		.summary("Create a tag")
		.description("Creates a new color or custom tag. Also use to create categories with type = category. Admin password required.")
		.tag("tags")
}

pub(super) fn router() -> ApiRouter<ApiState> {
	ApiRouter::new().api_route("/create", post_with(self::endpoint, self::endpoint_doc))
}

#[tracing::instrument(level = "debug", skip(state, _auth))]
async fn endpoint(
	State(state): State<ApiState>,
	_auth: AdminAuthenticationExtractor,
	Json(body): Json<CreateRequest>,
) -> Result<(StatusCode, Json<CreateResponse>), CreateError> {
	use entities::tags;

	let tag = tags::ActiveModel {
		name: Set(body.name),
		display_name: Set(body.display_name),
		description: Set(body.description),
		tag_type: Set(body.tag_type),
		..Default::default()
	}
	.insert(&state.database)
	.await?;

	Ok((
		StatusCode::CREATED,
		Json(CreateResponse {
			id: tag.id,
			name: tag.name,
			display_name: tag.display_name,
			description: tag.description,
			tag_type: tag.tag_type,
			created_at: tag.created_at,
		}),
	))
}
