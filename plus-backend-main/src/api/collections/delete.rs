use aide::{
	OperationIo,
	axum::{ApiRouter, routing::delete_with},
	transform::TransformOperation,
};
use axum::{
	extract::{Path, State},
	http::StatusCode,
	response::IntoResponse,
};
use sea_orm::EntityTrait;

use crate::api::{ApiState, admin_auth::AdminAuthenticationExtractor};

#[derive(thiserror::Error, Debug, OperationIo)]
pub enum DeleteError {
	#[error("No collection with that id")]
	NotFound,
	#[error("Database error: {0}")]
	Database(#[from] sea_orm::error::DbErr),
}

impl IntoResponse for DeleteError {
	fn into_response(self) -> axum::response::Response {
		(
			match self {
				Self::NotFound => StatusCode::NOT_FOUND,
				Self::Database(_) => StatusCode::INTERNAL_SERVER_ERROR,
			},
			self.to_string(),
		)
			.into_response()
	}
}

fn endpoint_doc(op: TransformOperation) -> TransformOperation {
	op.id("deleteCollection")
		.summary("Delete a collection")
		.description(
			"Deletes a collection. Cosmetics and bundles referencing it have their \
			 collection cleared. The collection's asset is left untouched. Admin role \
			 required.",
		)
		.tag("collections")
}

pub(super) fn router() -> ApiRouter<ApiState> {
	ApiRouter::new().api_route(
		"/delete/{id}",
		delete_with(self::endpoint, self::endpoint_doc),
	)
}

#[tracing::instrument(level = "debug", skip(state, _auth))]
async fn endpoint(
	State(state): State<ApiState>,
	_auth: AdminAuthenticationExtractor,
	Path(id): Path<i32>,
) -> Result<StatusCode, DeleteError> {
	use entities::prelude::*;

	let result = Collections::delete_by_id(id).exec(&state.database).await?;

	if result.rows_affected == 0 {
		return Err(DeleteError::NotFound);
	}

	Ok(StatusCode::NO_CONTENT)
}
