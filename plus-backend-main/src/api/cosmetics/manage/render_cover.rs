use aide::{
	OperationIo,
	axum::{ApiRouter, routing::post_with},
	transform::TransformOperation,
};
use axum::{Json, extract::State, http::StatusCode, response::IntoResponse};
use entities::sea_orm_active_enums::{AssetKind, BodySlot};
use schemars::JsonSchema;
use sea_orm::{ActiveModelTrait, ColumnTrait, EntityTrait, QueryFilter, Set};
use serde::{Deserialize, Serialize};

use crate::api::{ApiState, admin_auth::AdminAuthenticationExtractor};

#[derive(thiserror::Error, Debug, OperationIo)]
pub enum RenderCoverError {
	#[error("The requested cosmetic does not exist")]
	MissingCosmetic,
	#[error("The cosmetic has no source asset to render")]
	MissingAsset,
	#[error("Cover rendering is not configured (render_service_url is unset)")]
	RenderDisabled,
	#[error("The cosmetic already has a cover; pass force=true to regenerate")]
	CoverExists,
	#[error("Failed to render cover: {0}")]
	Render(String),
	#[error("Database error: {0}")]
	Database(#[from] sea_orm::error::DbErr),
	#[error("S3 error: {0}")]
	S3(#[from] s3::error::S3Error),
	#[error("Failed to store cover asset: {0}")]
	Store(String),
}

impl IntoResponse for RenderCoverError {
	fn into_response(self) -> axum::response::Response {
		(
			match self {
				Self::MissingCosmetic => StatusCode::NOT_FOUND,
				Self::MissingAsset | Self::CoverExists => StatusCode::BAD_REQUEST,
				Self::RenderDisabled => StatusCode::SERVICE_UNAVAILABLE,
				Self::Render(_) => StatusCode::BAD_GATEWAY,
				Self::Database(_) | Self::S3(_) | Self::Store(_) => {
					StatusCode::INTERNAL_SERVER_ERROR
				}
			},
			self.to_string(),
		)
			.into_response()
	}
}

#[derive(Debug, Deserialize, JsonSchema)]
struct RenderCoverRequest {
	cosmetic_id: i32,
	#[serde(default)]
	force: bool,
}

#[derive(Debug, Serialize, JsonSchema)]
struct RenderCoverResponse {
	cosmetic_id: i32,
	cover_asset_id: i32,
}

fn endpoint_doc(op: TransformOperation) -> TransformOperation {
	op.id("renderCosmeticCover")
		.summary("Render a cosmetic cover")
		.description(
			"Generates a cover image for an existing cosmetic by re-rendering its \
			 source asset through the render service, stores it, and points the \
			 cosmetic at the new cover. Skips cosmetics that already have a cover \
			 unless `force` is set. Admin password required.",
		)
		.tag("cosmetics")
		.response_with::<{ StatusCode::OK.as_u16() }, Json<RenderCoverResponse>, _>(
			|res| res.description("The cover was rendered and stored"),
		)
		.response_with::<{ StatusCode::NOT_FOUND.as_u16() }, String, _>(|res| {
			res.description("No cosmetic exists with the given id")
		})
		.response_with::<{ StatusCode::UNAUTHORIZED.as_u16() }, String, _>(|res| {
			res.description("Invalid or missing admin password")
		})
}

pub(super) fn router() -> ApiRouter<ApiState> {
	ApiRouter::new().api_route(
		"/render-cover",
		post_with(self::endpoint, self::endpoint_doc),
	)
}

#[tracing::instrument(level = "debug", skip(state, _auth))]
async fn endpoint(
	State(state): State<ApiState>,
	_auth: AdminAuthenticationExtractor,
	Json(body): Json<RenderCoverRequest>,
) -> Result<Json<RenderCoverResponse>, RenderCoverError> {
	use entities::{
		cosmetic, cosmetic_allowed_slot, cosmetic_group_allowed_slot, prelude::*,
	};

	if state.render_service_url.is_empty() {
		return Err(RenderCoverError::RenderDisabled);
	}

	let Some(cosmetic) = Cosmetic::find_by_id(body.cosmetic_id)
		.one(&state.database)
		.await?
	else {
		return Err(RenderCoverError::MissingCosmetic);
	};

	if cosmetic.cover_asset_id.is_some() && !body.force {
		return Err(RenderCoverError::CoverExists);
	}

	let Some(asset_id) = cosmetic.asset_id else {
		return Err(RenderCoverError::MissingAsset);
	};
	let Some(asset) = Asset::find_by_id(asset_id).one(&state.database).await? else {
		return Err(RenderCoverError::MissingAsset);
	};
	let Some(storage_path) = asset.storage_path.as_deref() else {
		return Err(RenderCoverError::MissingAsset);
	};
	let data = state.s3_bucket.get_object(storage_path).await?.to_vec();

	let mut slots: Vec<BodySlot> = CosmeticAllowedSlot::find()
		.filter(cosmetic_allowed_slot::Column::CosmeticId.eq(cosmetic.id))
		.all(&state.database)
		.await?
		.into_iter()
		.map(|row| row.slot)
		.collect();
	if let Some(group_id) = cosmetic.group_id
		&& slots.is_empty()
	{
		slots = CosmeticGroupAllowedSlot::find()
			.filter(cosmetic_group_allowed_slot::Column::GroupId.eq(group_id))
			.all(&state.database)
			.await?
			.into_iter()
			.map(|row| row.slot)
			.collect();
	}

	let is_bundle = asset.asset_kind == AssetKind::Bundle;

	let png = crate::api::cosmetics::cover::render_cover(
		&state.render_client,
		&state.render_service_url,
		&cosmetic.r#type,
		&slots,
		cosmetic.model_variant.as_deref(),
		is_bundle,
		&data,
	)
	.await
	.map_err(|error| RenderCoverError::Render(error.to_string()))?;

	let cover_asset_id = crate::api::collections::store_asset(
		&state,
		&png,
		Some("image/png".to_string()),
		"png",
		"covers",
	)
	.await
	.map_err(|error| RenderCoverError::Store(error.to_string()))?;

	let mut active: cosmetic::ActiveModel = cosmetic.into();
	active.cover_asset_id = Set(Some(cover_asset_id));
	let updated = active.update(&state.database).await?;

	Ok(Json(RenderCoverResponse {
		cosmetic_id: updated.id,
		cover_asset_id,
	}))
}
