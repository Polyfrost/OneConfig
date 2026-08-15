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
	#[error("The requested cosmetic does not exist")]
	MissingCosmetic,
	#[error("Database error: {0}")]
	Database(#[from] sea_orm::error::DbErr),
}

impl IntoResponse for DeleteError {
	fn into_response(self) -> axum::response::Response {
		(
			match self {
				Self::MissingCosmetic => StatusCode::NOT_FOUND,
				Self::Database(_) => StatusCode::INTERNAL_SERVER_ERROR,
			},
			self.to_string(),
		)
			.into_response()
	}
}

#[derive(Debug, Deserialize, JsonSchema)]
struct DeleteRequest {
	/// The id of the cosmetic (or any of its variants) to delete.
	cosmetic_id: i32,
}

fn endpoint_doc(op: TransformOperation) -> TransformOperation {
	op.id("deleteCosmetic")
		.summary("Delete a cosmetic")
		.description(
			"Soft-deletes a cosmetic by disabling it. For a grouped cosmetic the \
			 whole group is disabled. Rows, assets, and Stripe products are left \
			 intact so the change is reversible. Admin password required.",
		)
		.tag("cosmetics")
		.response_with::<{ StatusCode::NO_CONTENT.as_u16() }, (), _>(|res| {
			res.description("The cosmetic was disabled")
		})
		.response_with::<{ StatusCode::NOT_FOUND.as_u16() }, String, _>(|res| {
			res.description("No cosmetic exists with the given id")
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
	use entities::{cosmetic_group, prelude::*};

	let Some(cosmetic) = Cosmetic::find_by_id(body.cosmetic_id)
		.one(&state.database)
		.await?
	else {
		return Err(DeleteError::MissingCosmetic);
	};

	// Grouped cosmetics disable at the group level; ungrouped ones on the row.
	match cosmetic.group_id {
		Some(group_id) => {
			if let Some(group) = CosmeticGroup::find_by_id(group_id)
				.one(&state.database)
				.await?
			{
				let mut active: cosmetic_group::ActiveModel = group.into();
				active.enabled = Set(false);
				active.update(&state.database).await?;
			}
		}
		None => {
			if cosmetic.enabled {
				let mut active: entities::cosmetic::ActiveModel = cosmetic.into();
				active.enabled = Set(false);
				active.update(&state.database).await?;
			}
		}
	}

	Ok(StatusCode::NO_CONTENT)
}
