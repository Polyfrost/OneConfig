mod manage;
mod search;
mod view;

use aide::axum::ApiRouter;
use chrono::{DateTime, FixedOffset};
use schemars::JsonSchema;
use serde::Serialize;

use crate::api::ApiState;

/// The public view of a bundle, shared by the search, view and create
/// endpoints.
///
/// Stripe's product id is deliberately absent: only the price id is ever needed
/// client side to start a checkout.
#[derive(Debug, Serialize, JsonSchema)]
pub(super) struct BundleInfo {
	pub(super) id: i32,
	pub(super) name: String,
	pub(super) description: Option<String>,
	/// The id of the bundle's cover asset, resolvable through `/asset/{id}`.
	pub(super) asset_id: Option<i32>,
	pub(super) collection: Option<i32>,
	/// The price in USD major units (e.g. `4.99`).
	pub(super) base_price: Option<f32>,
	pub(super) discount_rate: Option<i32>,
	/// The Stripe price id to start a checkout with, if one is set.
	pub(super) stripe_price_id: Option<String>,
	pub(super) created_at: DateTime<FixedOffset>,
}

impl From<entities::bundles::Model> for BundleInfo {
	fn from(bundle: entities::bundles::Model) -> Self {
		Self {
			id: bundle.id,
			name: bundle.name,
			description: bundle.description,
			asset_id: bundle.asset_id,
			collection: bundle.collection,
			base_price: bundle.base_price,
			discount_rate: bundle.discount_rate,
			stripe_price_id: bundle.stripe_price_id,
			created_at: bundle.created_at,
		}
	}
}

pub(super) fn router() -> ApiRouter<ApiState> {
	ApiRouter::new().nest(
		"/bundles",
		ApiRouter::new()
			.merge(search::router())
			.merge(view::router())
			.merge(manage::router()),
	)
}
