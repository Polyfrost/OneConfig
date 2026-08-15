use aide::{
	OperationIo,
	axum::{ApiRouter, routing::post_with},
	transform::TransformOperation,
};
use axum::{Json, extract::State, http::StatusCode, response::IntoResponse};
use schemars::JsonSchema;
use sea_orm::{
	ActiveModelTrait, ColumnTrait, EntityTrait, QueryFilter, Set, TransactionTrait,
};
use serde::Deserialize;

use crate::api::{ApiState, admin_auth::AdminAuthenticationExtractor, stripe::products};

#[derive(thiserror::Error, Debug, OperationIo)]
pub enum UpdateError {
	#[error("The requested bundle does not exist")]
	MissingBundle,
	#[error("The bundle has no Stripe product to price")]
	MissingProduct,
	#[error("The bundle has no base price to discount from")]
	MissingBasePrice,
	#[error("A discount requires either a discount rate or a new price")]
	InvalidDiscount,
	#[error("Database error: {0}")]
	Database(#[from] sea_orm::error::DbErr),
	#[error("Stripe error: {0}")]
	Stripe(#[from] stripe_client::StripeError),
}

impl IntoResponse for UpdateError {
	fn into_response(self) -> axum::response::Response {
		(
			match self {
				Self::MissingBundle => StatusCode::NOT_FOUND,
				Self::MissingProduct | Self::MissingBasePrice | Self::InvalidDiscount => {
					StatusCode::BAD_REQUEST
				}
				Self::Stripe(_) => StatusCode::BAD_GATEWAY,
				Self::Database(_) => StatusCode::INTERNAL_SERVER_ERROR,
			},
			self.to_string(),
		)
			.into_response()
	}
}

/// The pricing columns a request resolves to.
struct PriceUpdate {
	stripe_price_id: String,
	/// Set only on a silent increase; left untouched for a discount.
	base_price: Option<f32>,
	/// Always written: the rate for a discount, `None` to clear the discount on
	/// a silent increase (which restores the full default price).
	discount_rate: Option<i32>,
}

#[derive(Debug, Deserialize, JsonSchema)]
struct UpdateRequest {
	/// The id of the bundle to update.
	bundle_id: i32,
	/// When set, toggles the enabled flag.
	enabled: Option<bool>,
	/// When set, renames the bundle.
	name: Option<String>,
	/// When present, sets (or clears with null) the collection.
	#[serde(default, skip_serializing_if = "Option::is_none")]
	collection: Option<Option<i32>>,
	/// When present, sets (or clears with null) the description.
	#[serde(default, skip_serializing_if = "Option::is_none")]
	description: Option<Option<String>>,
	/// When present, replaces the bundle's contained cosmetics with this set.
	cosmetic_ids: Option<Vec<i32>>,
	/// A new price in USD major units. Without `discount` this is a silent
	/// increase; with `discount` it is the discounted price.
	new_price: Option<f32>,
	/// Whether this update creates a discount rather than a silent price change.
	#[serde(default)]
	discount: bool,
	/// The discount percentage. Optional when `new_price` is given (then it is
	/// computed); required otherwise.
	discount_rate: Option<i32>,
}

fn endpoint_doc(op: TransformOperation) -> TransformOperation {
	op.id("updateBundle")
		.summary("Update a bundle")
		.description(
			"Updates a bundle's metadata (enabled, name, collection, description), \
			 optionally replaces its contained cosmetics, and drives its Stripe \
			 pricing. A silent price increase creates a new default price; a discount \
			 creates a non-default price and records the rate. Admin password required.",
		)
		.tag("bundles")
		.response_with::<{ StatusCode::NO_CONTENT.as_u16() }, (), _>(|res| {
			res.description("The bundle was updated")
		})
		.response_with::<{ StatusCode::NOT_FOUND.as_u16() }, String, _>(|res| {
			res.description("No bundle exists with the given id")
		})
		.response_with::<{ StatusCode::UNAUTHORIZED.as_u16() }, String, _>(|res| {
			res.description("Invalid or missing admin password")
		})
}

pub(super) fn router() -> ApiRouter<ApiState> {
	ApiRouter::new().api_route("/update", post_with(self::endpoint, self::endpoint_doc))
}

#[tracing::instrument(level = "debug", skip(state, _auth))]
async fn endpoint(
	State(state): State<ApiState>,
	_auth: AdminAuthenticationExtractor,
	Json(body): Json<UpdateRequest>,
) -> Result<StatusCode, UpdateError> {
	use entities::{bundles, bundles_cosmetics, prelude::*};

	let Some(bundle) = Bundles::find_by_id(body.bundle_id)
		.one(&state.database)
		.await?
	else {
		return Err(UpdateError::MissingBundle);
	};

	// Resolve the pricing change (if any) against Stripe before touching the
	// database.
	let price_update = if body.new_price.is_some() || body.discount {
		let product_id = bundle
			.stripe_product_id
			.as_deref()
			.ok_or(UpdateError::MissingProduct)?;

		if body.discount {
			let base = bundle.base_price.ok_or(UpdateError::MissingBasePrice)?;
			let (discounted, rate) = match (body.discount_rate, body.new_price) {
				(Some(rate), _) => (base * (1.0 - rate as f32 / 100.0), rate),
				(None, Some(new_price)) => {
					let rate = (((base - new_price) / base) * 100.0).round() as i32;
					(new_price, rate)
				}
				(None, None) => return Err(UpdateError::InvalidDiscount),
			};

			let price_id = products::create_price(
				&state.stripe.client,
				product_id,
				products::to_cents(discounted),
			)
			.await?;

			Some(PriceUpdate {
				stripe_price_id: price_id,
				base_price: None,
				discount_rate: Some(rate),
			})
		} else {
			// Silent increase: new_price is guaranteed present by the guard above.
			let new_price = body.new_price.ok_or(UpdateError::InvalidDiscount)?;
			let price_id = products::create_price(
				&state.stripe.client,
				product_id,
				products::to_cents(new_price),
			)
			.await?;
			products::set_default_price(&state.stripe.client, product_id, &price_id)
				.await?;

			Some(PriceUpdate {
				stripe_price_id: price_id,
				base_price: Some(new_price),
				discount_rate: None,
			})
		}
	} else {
		None
	};

	let txn = state.database.begin().await?;

	let mut active: bundles::ActiveModel = bundle.into();
	let mut changed = false;

	if let Some(name) = &body.name {
		active.name = Set(name.clone());
		changed = true;
	}
	if let Some(enabled) = body.enabled {
		active.enabled = Set(enabled);
		changed = true;
	}
	if let Some(collection) = &body.collection {
		active.collection = Set(*collection);
		changed = true;
	}
	if let Some(description) = &body.description {
		active.description = Set(description.clone());
		changed = true;
	}
	if let Some(price) = &price_update {
		active.stripe_price_id = Set(Some(price.stripe_price_id.clone()));
		if let Some(base) = price.base_price {
			active.base_price = Set(Some(base));
		}
		active.discount_rate = Set(price.discount_rate);
		changed = true;
	}

	if changed {
		active.update(&txn).await?;
	}

	// Replace the bundle's contents when a new set was provided.
	if let Some(cosmetic_ids) = &body.cosmetic_ids {
		BundlesCosmetics::delete_many()
			.filter(bundles_cosmetics::Column::BundleId.eq(body.bundle_id))
			.exec(&txn)
			.await?;

		if !cosmetic_ids.is_empty() {
			BundlesCosmetics::insert_many(cosmetic_ids.iter().map(|cosmetic_id| {
				bundles_cosmetics::ActiveModel {
					bundle_id: Set(body.bundle_id),
					cosmetic_id: Set(*cosmetic_id),
				}
			}))
			.on_conflict_do_nothing()
			.exec(&txn)
			.await?;
		}
	}

	txn.commit().await?;

	Ok(StatusCode::NO_CONTENT)
}
