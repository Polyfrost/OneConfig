use aide::axum::ApiRouter;

use crate::api::ApiState;

mod endpoint;
pub mod structs;

pub(super) async fn setup_router() -> ApiRouter<ApiState> {
	ApiRouter::new().merge(endpoint::router())
}
