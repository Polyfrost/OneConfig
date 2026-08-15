use aide::{
	OperationIo,
	axum::{ApiRouter, routing::get_with},
	transform::TransformOperation,
};
use axum::{
	Json,
	extract::{Path, State},
	http::StatusCode,
	response::IntoResponse,
};
use schemars::JsonSchema;
use sea_orm::{ColumnTrait, EntityTrait, QueryFilter};
use serde::Serialize;

use crate::api::{ApiState, bundles::BundleInfo};

#[derive(thiserror::Error, Debug, OperationIo)]
pub enum ViewError {
	#[error("No enabled bundle with that id exists")]
	NotFound,
	#[error("Unable to query database: {0}")]
	Database(#[from] sea_orm::error::DbErr),
}

impl IntoResponse for ViewError {
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

/// A bundle's information along with the cosmetics and emotes it contains.
#[derive(Debug, Serialize, JsonSchema)]
pub struct ViewResponse {
	bundle: BundleInfo,
	/// The ids of the cosmetics contained in the bundle.
	cosmetics: Vec<i32>,
	/// The ids of the emotes contained in the bundle.
	emotes: Vec<i32>,
}

fn endpoint_doc(op: TransformOperation) -> TransformOperation {
	op.id("viewBundle")
		.summary("View a bundle")
		.description(
			"Returns an enabled bundle's information and the ids of the cosmetics and \
			 emotes it contains.",
		)
		.tag("bundles")
}

pub(super) fn router() -> ApiRouter<ApiState> {
	ApiRouter::new().api_route("/view/{id}", get_with(self::endpoint, self::endpoint_doc))
}

#[tracing::instrument(level = "debug", skip(state))]
async fn endpoint(
	State(state): State<ApiState>,
	Path(id): Path<i32>,
) -> Result<Json<ViewResponse>, ViewError> {
	use entities::{
		bundles, bundles_cosmetics, prelude::*, sea_orm_active_enums::CosmeticType,
	};

	let bundle = Bundles::find_by_id(id)
		.filter(bundles::Column::Enabled.eq(true))
		.one(&state.database)
		.await?
		.ok_or(ViewError::NotFound)?;

	let mut cosmetics = Vec::new();
	let mut emotes = Vec::new();
	for (link, cosmetic) in BundlesCosmetics::find()
		.filter(bundles_cosmetics::Column::BundleId.eq(id))
		.find_also_related(Cosmetic)
		.all(&state.database)
		.await?
	{
		match cosmetic {
			Some(cosmetic) if matches!(cosmetic.r#type, CosmeticType::Emote) => {
				emotes.push(link.cosmetic_id)
			}
			_ => cosmetics.push(link.cosmetic_id),
		}
	}

	Ok(Json(ViewResponse {
		bundle: bundle.into(),
		cosmetics,
		emotes,
	}))
}
