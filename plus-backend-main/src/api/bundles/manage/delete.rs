use aide::{
	OperationIo,
	axum::{ApiRouter, routing::post_with},
	transform::TransformOperation,
};
use axum::{Json, extract::State, http::StatusCode, response::IntoResponse};
use schemars::JsonSchema;
use sea_orm::{ActiveModelTrait, EntityTrait, Set};
use serde::Deserialize;

use crate::api::{ApiState, admin_auth::AdminAuthenticationExtractor};

#[derive(thiserror::Error, Debug, OperationIo)]
pub enum DeleteError {
	#[error("The requested bundle does not exist")]
	MissingBundle,
	#[error("Database error: {0}")]
	Database(#[from] sea_orm::error::DbErr),
}

impl IntoResponse for DeleteError {
	fn into_response(self) -> axum::response::Response {
		(
			match self {
				Self::MissingBundle => StatusCode::NOT_FOUND,
				Self::Database(_) => StatusCode::INTERNAL_SERVER_ERROR,
			},
			self.to_string(),
		)
			.into_response()
	}
}

#[derive(Debug, Deserialize, JsonSchema)]
struct DeleteRequest {
	/// The id of the bundle to delete.
	bundle_id: i32,
}

fn endpoint_doc(op: TransformOperation) -> TransformOperation {
	op.id("deleteBundle")
		.summary("Delete a bundle")
		.description(
			"Soft-deletes a bundle by disabling it. Rows, assets, and Stripe products \
			 are left intact so the change is reversible. Admin password required.",
		)
		.tag("bundles")
		.response_with::<{ StatusCode::NO_CONTENT.as_u16() }, (), _>(|res| {
			res.description("The bundle was disabled")
		})
		.response_with::<{ StatusCode::NOT_FOUND.as_u16() }, String, _>(|res| {
			res.description("No bundle exists with the given id")
		})
		.response_with::<{ StatusCode::UNAUTHORIZED.as_u16() }, String, _>(|res| {
			res.description("Invalid or missing admin password")
		})
}

pub(super) fn router() -> ApiRouter<ApiState> {
	ApiRouter::new().api_route("/delete", post_with(self::endpoint, self::endpoint_doc))
}

#[tracing::instrument(level = "debug", skip(state, _auth))]
async fn endpoint(
	State(state): State<ApiState>,
	_auth: AdminAuthenticationExtractor,
	Json(body): Json<DeleteRequest>,
) -> Result<StatusCode, DeleteError> {
	use entities::{bundles, prelude::*};

	let Some(bundle) = Bundles::find_by_id(body.bundle_id)
		.one(&state.database)
		.await?
	else {
		return Err(DeleteError::MissingBundle);
	};

	if bundle.enabled {
		let mut active: bundles::ActiveModel = bundle.into();
		active.enabled = Set(false);
		active.update(&state.database).await?;
	}

	Ok(StatusCode::NO_CONTENT)
}
