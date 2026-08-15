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
use chrono::{DateTime, FixedOffset};
use entities::sea_orm_active_enums::CosmeticType;
use schemars::JsonSchema;
use sea_orm::{ColumnTrait, EntityTrait, QueryFilter, QueryOrder};
use serde::Serialize;

use crate::api::{
	ApiState,
	tags::{CosmeticTags, tags_for_cosmetics},
};

#[derive(thiserror::Error, Debug, OperationIo)]
pub enum ViewError {
	#[error("No enabled cosmetic with that id exists")]
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

#[derive(Debug, Serialize, JsonSchema)]
pub struct ViewResponse {
	/// The Stripe price id for this cosmetic, if one is set.
	stripe_price_id: Option<String>,
	id: i32,
	name: String,
	description: Option<String>,
	collection: Option<i32>,
	r#type: CosmeticType,
	base_price: Option<f32>,
	discount_rate: Option<i32>,
	asset_id: Option<i32>,
	cover_asset_id: Option<i32>,
	created_at: DateTime<FixedOffset>,
	tags: CosmeticTags,
	/// Every enabled variant of this cosmetic's group, ordered by
	/// `variant_order`, including the cosmetic asked for — so this is the whole
	/// swatch list and does not shift depending on which variant is viewed. Null,
	/// not empty, for an ungrouped cosmetic. Price and Stripe price id are
	/// omitted from each entry as they are shared across every variant.
	variants: Option<Vec<VariantView>>,
}

/// A sibling variant of the viewed cosmetic.
#[derive(Debug, Serialize, JsonSchema)]
pub struct VariantView {
	id: i32,
	variant_name: Option<String>,
	model_variant: Option<String>,
	variant_order: i32,
	asset_id: Option<i32>,
	cover_asset_id: Option<i32>,
}

impl VariantView {
	pub(super) fn from_cosmetic(cosmetic: entities::cosmetic::Model) -> Self {
		Self {
			id: cosmetic.id,
			variant_name: cosmetic.variant_name,
			model_variant: cosmetic.model_variant,
			variant_order: cosmetic.variant_order,
			asset_id: cosmetic.asset_id,
			cover_asset_id: cosmetic.cover_asset_id,
		}
	}
}

fn endpoint_doc(op: TransformOperation) -> TransformOperation {
	op.id("viewCosmetic")
		.summary("View a cosmetic")
		.description(
			"Returns the Stripe price id of an enabled cosmetic (including \
			 emotes). For a grouped cosmetic, `variants` lists every enabled \
			 variant of its group, this one included (price and price id omitted \
			 from each, as they are shared).",
		)
		.tag("cosmetics")
}

pub(super) fn router() -> ApiRouter<ApiState> {
	ApiRouter::new().api_route(
		"/cosmetics/view/{id}",
		get_with(self::endpoint, self::endpoint_doc),
	)
}

#[tracing::instrument(level = "debug", skip(state))]
async fn endpoint(
	State(state): State<ApiState>,
	Path(id): Path<i32>,
) -> Result<Json<ViewResponse>, ViewError> {
	use entities::{cosmetic, prelude::*};

	if let Some(cosmetic) = Cosmetic::find_by_id(id)
		.filter(cosmetic::Column::Enabled.eq(true))
		.one(&state.database)
		.await?
	{
		let tags = tags_for_cosmetics(&state.database, &[cosmetic.id])
			.await?
			.remove(&cosmetic.id)
			.unwrap_or_default();

		// Grouped cosmetics carry variants; load the whole group, this one
		// included, so the list is the same whichever variant was asked for.
		let (variants, group_name) = if let Some(group_id) = cosmetic.group_id {
			let group = Cosmetic::find()
				.filter(cosmetic::Column::GroupId.eq(group_id))
				.filter(cosmetic::Column::Enabled.eq(true))
				.order_by_asc(cosmetic::Column::VariantOrder)
				.order_by_asc(cosmetic::Column::Id)
				.all(&state.database)
				.await?;

			let group_name = CosmeticGroup::find_by_id(group_id)
				.one(&state.database)
				.await?
				.map(|group| group.name);

			(
				Some(
					group
						.into_iter()
						.filter(|c| {
							!super::is_redundant_variant(c.variant_name.as_deref())
						})
						.map(VariantView::from_cosmetic)
						.collect(),
				),
				group_name,
			)
		} else {
			(None, None)
		};

		Ok(Json(ViewResponse {
			stripe_price_id: cosmetic.stripe_price_id,
			id: cosmetic.id,
			name: group_name
				.or(cosmetic.name)
				.unwrap_or_else(|| format!("Cosmetic {}", cosmetic.id)),
			description: cosmetic.description,
			collection: cosmetic.collection,
			r#type: cosmetic.r#type,
			base_price: cosmetic.base_price,
			discount_rate: cosmetic.discount_rate,
			asset_id: cosmetic.asset_id,
			cover_asset_id: cosmetic.cover_asset_id,
			created_at: cosmetic.created_at,
			tags,
			variants,
		}))
	} else {
		Err(ViewError::NotFound)
	}
}
