mod create;
mod delete;
mod edit;
mod list;
mod view;

use aide::axum::ApiRouter;
use entities::sea_orm_active_enums::AssetKind;
use sea_orm::{ActiveModelTrait, Set};
use sha2::{Digest, Sha256};
use uuid::Uuid;

use crate::api::ApiState;

#[derive(thiserror::Error, Debug)]
pub(crate) enum StoreAssetError {
	#[error("Database error: {0}")]
	Database(#[from] sea_orm::error::DbErr),
	#[error("S3 error: {0}")]
	S3(#[from] s3::error::S3Error),
}

fn sha256_hex(data: &[u8]) -> String {
	Sha256::digest(data)
		.iter()
		.map(|byte| format!("{byte:02x}"))
		.collect()
}

pub(crate) async fn store_asset(
	state: &ApiState,
	data: &[u8],
	content_type: Option<String>,
	extension: &str,
	prefix: &str,
) -> Result<i32, StoreAssetError> {
	use entities::asset;

	let path = format!("{}/{}.{}", prefix, Uuid::now_v7(), extension);
	state
		.s3_bucket
		.put_object_with_content_type(
			&path,
			data,
			content_type.as_deref().unwrap_or("image/png"),
		)
		.await?;

	let asset = asset::ActiveModel {
		storage_path: Set(Some(path)),
		url: Set(None),
		asset_kind: Set(AssetKind::Image),
		content_type: Set(content_type.or_else(|| Some("image/png".to_string()))),
		hash: Set(Some(sha256_hex(data))),
		..Default::default()
	}
	.insert(&state.database)
	.await?;

	Ok(asset.id)
}

pub(super) async fn setup_router() -> ApiRouter<ApiState> {
	ApiRouter::new().nest(
		"/collections",
		ApiRouter::new()
			.merge(list::router())
			.merge(create::router())
			.merge(edit::router())
			.merge(delete::router())
			.merge(view::router()),
	)
}
