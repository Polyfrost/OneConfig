use aide::{
	OperationIo,
	axum::{ApiRouter, routing::get_with},
	transform::TransformOperation,
};
use axum::{
	Json,
	extract::{Path, State},
	http::StatusCode,
	response::{IntoResponse, Redirect},
};
use schemars::JsonSchema;
use sea_orm::EntityTrait;
use serde::Serialize;

use crate::api::ApiState;

#[derive(thiserror::Error, Debug, OperationIo)]
pub enum AssetError {
	#[error("No asset with that id has a resolvable url")]
	NotFound,
	#[error("Unable to query database: {0}")]
	Database(#[from] sea_orm::error::DbErr),
	#[error("Unable to presign asset url: {0}")]
	S3(#[from] s3::error::S3Error),
}

impl IntoResponse for AssetError {
	fn into_response(self) -> axum::response::Response {
		(
			match self {
				Self::NotFound => StatusCode::NOT_FOUND,
				Self::Database(_) | Self::S3(_) => StatusCode::INTERNAL_SERVER_ERROR,
			},
			self.to_string(),
		)
			.into_response()
	}
}

/// The resolvable url for an asset.
#[derive(Debug, Serialize, JsonSchema)]
pub struct AssetUrlResponse {
	/// The direct (possibly presigned) url the asset can be fetched from.
	url: String,
}

/// Resolve the direct url for the asset with the given id, presigning an S3
/// object url when the asset is backed by object storage.
async fn resolve_url(state: &ApiState, id: i32) -> Result<String, AssetError> {
	use entities::prelude::*;

	let asset = Asset::find_by_id(id)
		.one(&state.database)
		.await?
		.ok_or(AssetError::NotFound)?;

	match (&asset.url, &asset.storage_path) {
		(Some(url), _) => Ok(url.clone()),
		(None, Some(path)) => Ok(state.s3_bucket.presign_get(path, 86400, None).await?), // 24h
		(None, None) => Err(AssetError::NotFound),
	}
}

fn redirect_doc(op: TransformOperation) -> TransformOperation {
	op.id("getAsset")
		.summary("Get an asset")
		.description(
			"Redirects to the resolved url for the given asset. Prefer `/asset/{id}/url` \
			 from browser JavaScript, since following this redirect to object storage is \
			 subject to CORS. Returns 404 when no asset with that id has a resolvable url.",
		)
		.tag("assets")
}

fn url_doc(op: TransformOperation) -> TransformOperation {
	op.id("getAssetUrl")
		.summary("Get an asset's url")
		.description(
			"Returns the resolved url for the given asset as JSON, without redirecting. \
			 Use this from browser JavaScript to fetch the asset directly and avoid the \
			 CORS pitfalls of a cross-origin redirect. Returns 404 when no asset with \
			 that id has a resolvable url.",
		)
		.tag("assets")
}

pub(super) async fn setup_router() -> ApiRouter<ApiState> {
	ApiRouter::new()
		.api_route(
			"/asset/{id}",
			get_with(self::redirect_endpoint, self::redirect_doc),
		)
		.api_route(
			"/asset/{id}/url",
			get_with(self::url_endpoint, self::url_doc),
		)
}

#[tracing::instrument(level = "debug", skip(state))]
async fn redirect_endpoint(
	State(state): State<ApiState>,
	Path(id): Path<i32>,
) -> Result<Redirect, AssetError> {
	Ok(Redirect::temporary(&resolve_url(&state, id).await?))
}

#[tracing::instrument(level = "debug", skip(state))]
async fn url_endpoint(
	State(state): State<ApiState>,
	Path(id): Path<i32>,
) -> Result<Json<AssetUrlResponse>, AssetError> {
	Ok(Json(AssetUrlResponse {
		url: resolve_url(&state, id).await?,
	}))
}
