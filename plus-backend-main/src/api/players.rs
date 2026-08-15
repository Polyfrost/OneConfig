use aide::{
	OperationIo,
	axum::{ApiRouter, routing::put_with},
	transform::TransformOperation,
};
use axum::{Json, extract::State, http::StatusCode, response::IntoResponse};
use entities::sea_orm_active_enums::PlayerRole;
use schemars::JsonSchema;
use sea_orm::{ActiveModelTrait, ColumnTrait, EntityTrait, QueryFilter, Set};
use serde::Deserialize;
use uuid::Uuid;

use crate::api::{ApiState, account::AdminPlayer};

#[derive(thiserror::Error, Debug, OperationIo)]
pub enum RoleError {
	#[error("The requested player does not exist")]
	PlayerMissing,
	#[error("Unable to query database: {0}")]
	Database(#[from] sea_orm::error::DbErr),
}

impl IntoResponse for RoleError {
	fn into_response(self) -> axum::response::Response {
		(
			match self {
				Self::PlayerMissing => StatusCode::NOT_FOUND,
				Self::Database(_) => StatusCode::INTERNAL_SERVER_ERROR,
			},
			self.to_string(),
		)
			.into_response()
	}
}

fn endpoint_doc(op: TransformOperation) -> TransformOperation {
	op.id("setPlayerRole")
		.summary("Set a player role")
		.description("Sets a player's role. Admin role required.")
		.tag("players")
}

#[derive(Debug, Deserialize, JsonSchema)]
struct RoleRequest {
	player: Uuid,
	role: PlayerRole,
}

pub(super) async fn setup_router() -> ApiRouter<ApiState> {
	ApiRouter::new().api_route(
		"/players/role",
		put_with(self::endpoint, self::endpoint_doc),
	)
}

#[tracing::instrument(level = "debug", skip(state))]
async fn endpoint(
	State(state): State<ApiState>,
	AdminPlayer(_admin): AdminPlayer,
	Json(body): Json<RoleRequest>,
) -> Result<StatusCode, RoleError> {
	use entities::{prelude::*, user};

	let Some(player) = User::find()
		.filter(user::Column::MinecraftUuid.eq(body.player))
		.one(&state.database)
		.await?
	else {
		return Err(RoleError::PlayerMissing);
	};

	let mut player: user::ActiveModel = player.into();
	player.role = Set(body.role);
	player.update(&state.database).await?;

	Ok(StatusCode::NO_CONTENT)
}
