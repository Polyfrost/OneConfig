use aide::{
	OperationIo,
	axum::{ApiRouter, routing::get_with},
	transform::TransformOperation,
};
use axum::{Json, extract::State, http::StatusCode, response::IntoResponse};
use schemars::JsonSchema;
use sea_orm::{ColumnTrait, EntityTrait, QueryFilter};
use serde::Serialize;

use crate::api::{
	ApiState,
	cosmetics::{CosmeticInfo, group_cosmetics, load_groups},
};

#[derive(thiserror::Error, Debug, OperationIo)]
pub enum ResponseError {
	#[error("Unable to fetch user data from database: {0}")]
	DatabaseFetch(#[from] sea_orm::error::DbErr),
	#[error("Unable to presign S3 URLs: {0}")]
	S3Presign(#[from] s3::error::S3Error),
}

fn endpoint_doc(op: TransformOperation) -> TransformOperation {
	op.id("listCosmetics")
		.summary("List all cosmetics")
		.description("Lists all cosmetics, including their URLs and unique IDs")
		.tag("cosmetics")
		.response_with::<{ StatusCode::INTERNAL_SERVER_ERROR.as_u16() }, String, _>(
			|res| {
				res.description(
					"An internal server error occurred while trying to fetch cosmetics",
				)
			},
		)
}

impl IntoResponse for ResponseError {
	fn into_response(self) -> axum::response::Response {
		(
			match self {
				ResponseError::S3Presign(_) => StatusCode::INTERNAL_SERVER_ERROR,
				ResponseError::DatabaseFetch(_) => StatusCode::INTERNAL_SERVER_ERROR,
			},
			self.to_string(),
		)
			.into_response()
	}
}

/// Information about the player's cosmetics
#[derive(Debug, Default, Serialize, JsonSchema)]
pub struct Response {
	cosmetics: Vec<CosmeticInfo>,
}

pub(super) fn router() -> ApiRouter<ApiState> {
	ApiRouter::new().api_route("/cosmetics", get_with(self::endpoint, self::endpoint_doc))
}

#[tracing::instrument(level = "debug", skip(state))]
async fn endpoint(
	State(state): State<ApiState>,
) -> Result<Json<Response>, ResponseError> {
	let mut response = Response::default();

	{
		use entities::{cosmetic, prelude::*};

		let cosmetics = Cosmetic::find()
			.filter(cosmetic::Column::Enabled.eq(true))
			.find_with_related(CosmeticAllowedSlot)
			.all(&state.database)
			.await?;

		let mut rows = Vec::with_capacity(cosmetics.len());
		for (cosmetic, allowed) in cosmetics {
			let asset = match cosmetic.asset_id {
				Some(asset_id) => {
					Asset::find_by_id(asset_id).one(&state.database).await?
				}
				None => None,
			};
			let cover_asset = match cosmetic.cover_asset_id {
				Some(asset_id) => {
					Asset::find_by_id(asset_id).one(&state.database).await?
				}
				None => None,
			};
			let allowed_slots = allowed.into_iter().map(|s| s.slot).collect();
			rows.push((cosmetic, asset, cover_asset, allowed_slots));
		}

		let groups = load_groups(&state.database).await?;
		response.cosmetics = group_cosmetics(
			rows,
			groups,
			state.asset_cache.clone(),
			state.s3_bucket.clone(),
			true,
		)
		.await?;
	};

	Ok(Json(response))
}
