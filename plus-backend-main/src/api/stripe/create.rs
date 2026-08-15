use std::collections::HashMap;

use aide::{OperationIo, transform::TransformOperation};
use axum::{Json, extract::State, http::StatusCode, response::IntoResponse};
use entities::{player_owned_cosmetic, prelude::*, user};
use schemars::JsonSchema;
use sea_orm::{DbErr, prelude::*};
use serde::{Deserialize, Serialize};
use stripe_checkout::CheckoutSessionMode;
use stripe_checkout::checkout_session::{
	CreateCheckoutSession, CreateCheckoutSessionLineItems,
};
use uuid::Uuid;

use crate::api::{
	ApiState,
	stripe::pricing::{cosmetics_for_price, display_name},
};

#[derive(Debug, thiserror::Error, OperationIo)]
pub(super) enum CreateError {
	#[error("Unable to create checkout session: {0}")]
	Stripe(#[from] stripe_client::StripeError),
	#[error("Stripe did not return a checkout url")]
	MissingUrl,
	#[error("Player already owns {0}")]
	AlreadyOwned(String),
	#[error("Unable to check existing ownership: {0}")]
	Database(#[from] DbErr),
}

impl IntoResponse for CreateError {
	fn into_response(self) -> axum::response::Response {
		(
			match self {
				CreateError::Stripe(_) => StatusCode::BAD_GATEWAY,
				CreateError::MissingUrl | CreateError::Database(_) => {
					StatusCode::INTERNAL_SERVER_ERROR
				}
				CreateError::AlreadyOwned(_) => StatusCode::CONFLICT,
			},
			self.to_string(),
		)
			.into_response()
	}
}

#[derive(Debug, Deserialize, JsonSchema)]
pub(super) struct CreateRequest {
	/// The Minecraft UUID of the receiving player
	player: Uuid,
	/// The Minecraft UUID of the buyer, None if player == buyer
	buyer: Option<Uuid>,
	/// The Stripe price ids to charge for, one checkout line each
	prices: Vec<String>,
}

#[derive(Debug, Serialize, JsonSchema)]
pub(super) struct CreateResponse {
	/// The Stripe-hosted checkout page url to redirect the buyer to
	url: String,
}

pub fn endpoint_doc(op: TransformOperation) -> TransformOperation {
	op.id("createStripeCheckout")
		.summary("Create a Stripe checkout")
		.description(concat!(
			"Creates a Stripe checkout for one or more cosmetics/emotes ",
			"using their Stripe IDs returned from the list all cosmetics endpoint (not implemented). ",
			"Responds 409 naming the cosmetics if the receiving player already owns any of them."
		))
		.tag("stripe")
}

#[tracing::instrument(level = "debug", skip(state))]
pub(super) async fn endpoint(
	State(state): State<ApiState>,
	Json(request): Json<CreateRequest>,
) -> Result<Json<CreateResponse>, CreateError> {
	let CreateRequest {
		player,
		prices,
		buyer,
	} = request;

	let mut cosmetics = Vec::new();
	for price in &prices {
		cosmetics.extend(cosmetics_for_price(&state.database, price).await?);
	}

	if !cosmetics.is_empty()
		&& let Some(user) = User::find()
			.filter(user::Column::MinecraftUuid.eq(player))
			.one(&state.database)
			.await?
	{
		let owned: Vec<i32> = PlayerOwnedCosmetic::find()
			.filter(player_owned_cosmetic::Column::PlayerId.eq(user.id))
			.filter(
				player_owned_cosmetic::Column::CosmeticId
					.is_in(cosmetics.iter().map(|cosmetic| cosmetic.id)),
			)
			.all(&state.database)
			.await?
			.into_iter()
			.map(|owned| owned.cosmetic_id)
			.collect();

		if !owned.is_empty() {
			return Err(CreateError::AlreadyOwned(
				cosmetics
					.iter()
					.filter(|cosmetic| owned.contains(&cosmetic.id))
					.map(display_name)
					.collect::<Vec<_>>()
					.join(", "),
			));
		}
	}

	let line_items = prices
		.iter()
		.map(|price| {
			let mut item = CreateCheckoutSessionLineItems::new();
			item.price = Some(price.clone());
			item.quantity = Some(1);
			item
		})
		.collect::<Vec<_>>();

	let metadata = HashMap::from([
		("player".to_string(), player.to_string()), // minecraft uuid!!
		(
			"buyer".to_string(),
			buyer.map_or_else(|| player.to_string(), |b| b.to_string()),
		), // minecraft uuid!!
		("prices".to_string(), prices.join(",")),
	]);

	let session = CreateCheckoutSession::new()
		.line_items(line_items)
		.mode(CheckoutSessionMode::Payment)
		.success_url(state.stripe.success_url.clone())
		.cancel_url(state.stripe.cancel_url.clone())
		.metadata(metadata)
		.send(&state.stripe.client)
		.await?;

	session
		.url
		.map(|url| Json(CreateResponse { url }))
		.ok_or(CreateError::MissingUrl)
}
