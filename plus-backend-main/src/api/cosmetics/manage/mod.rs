mod create;
mod delete;
mod render_cover;
mod update;

use aide::axum::ApiRouter;

use crate::api::ApiState;

pub(super) fn router() -> ApiRouter<ApiState> {
	ApiRouter::new().nest(
		"/manage",
		ApiRouter::new()
			.merge(create::router())
			.merge(update::router())
			.merge(delete::router())
			.merge(render_cover::router()),
	)
}
