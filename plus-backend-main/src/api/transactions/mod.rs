mod player;

use aide::axum::{ApiRouter, routing::get_with};

use crate::api::ApiState;

pub(super) async fn setup_router() -> ApiRouter<ApiState> {
	ApiRouter::new()
		.api_route("/player", get_with(player::endpoint, player::endpoint_doc))
}
