mod create;
mod pricing;
pub(in crate::api) mod products;
mod webhook;

use aide::axum::{ApiRouter, routing::post_with};

use crate::api::ApiState;

pub(super) async fn setup_router() -> ApiRouter<ApiState> {
	// Nested like every other module: the store calls `/stripe/create`, and an
	// unprefixed `/create` would sit oddly next to `/collections/create` and
	// `/tags/create`.
	ApiRouter::new().nest(
		"/stripe",
		ApiRouter::new()
			.api_route("/create", post_with(create::endpoint, create::endpoint_doc))
			.route("/webhook", axum::routing::post(webhook::endpoint)),
	)
}
