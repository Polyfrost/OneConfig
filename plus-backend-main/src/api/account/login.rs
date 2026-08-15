use std::time::Duration;

use aide::{
	OperationIo,
	axum::{ApiRouter, routing::post_with},
	transform::TransformOperation,
};
use axum::{
	Json,
	extract::{Query, State, rejection::QueryRejection},
	http::StatusCode,
	response::IntoResponse,
};
use pasetors::{claims::Claims, local};
use schemars::JsonSchema;
use serde::{Deserialize, Serialize};
use uuid::Uuid;

use crate::{
	api::{ApiState, account::PASETO_IMPLICIT_ASSERT},
	database::{DatabaseUserExt, record_monthly_active_login},
};

#[derive(Deserialize)]
pub struct SessionserverLoginSuccess {
	id: Uuid,
}

#[derive(thiserror::Error, Debug, OperationIo)]
pub enum LoginError {
	#[error("The passed player UUID was not a valid UUID: {0}")]
	InvalidUuid(#[from] QueryRejection),
	#[error("Unable to authenticate with mojang API: {0}")]
	SessionserverAuthentication(#[source] reqwest::Error),
	#[error("Unable to construct authentication token: {0}")]
	TokenCreation(#[from] pasetors::errors::Error),
	#[error("Unable to update login analytics: {0}")]
	Database(#[from] sea_orm::error::DbErr),
	#[error("Mojang sessionserver authentication did not suceeed")]
	Unauthorized,
}

fn endpoint_doc(op: TransformOperation) -> TransformOperation {
	op.id("login")
		.summary("Log in as a minecraft player")
		.description("Logs in using mojang sessionserver authentication")
		.tag("account")
		.response_with::<{ StatusCode::INTERNAL_SERVER_ERROR.as_u16() }, String, _>(
			|res| {
				res.description(
					"An internal server error occurred while trying to log in",
				)
			},
		)
}

impl IntoResponse for LoginError {
	fn into_response(self) -> axum::response::Response {
		(
			match self {
				Self::InvalidUuid(_) => StatusCode::BAD_REQUEST,
				Self::SessionserverAuthentication(_) => StatusCode::INTERNAL_SERVER_ERROR,
				Self::TokenCreation(_) => StatusCode::INTERNAL_SERVER_ERROR,
				Self::Database(_) => StatusCode::INTERNAL_SERVER_ERROR,
				Self::Unauthorized => StatusCode::UNAUTHORIZED,
			},
			self.to_string(),
		)
			.into_response()
	}
}

#[derive(Debug, Deserialize, JsonSchema)]
struct LoginQuery {
	/// The username of the player to login as
	#[schemars(example = &"Hypixel")]
	username: String,
	/// The serverId that was given to Mojang for authentication
	///
	/// This should ideally be randomly generated on the client
	#[schemars(example = &"FnuhJQCStLeUOIwnrHgBjiTolqWRBBSe")]
	server_id: String,
}

/// A response given on successful a payment restore
#[derive(Debug, Default, Serialize, JsonSchema)]
pub struct LoginResponse {
	/// The authentication token to use for requests to the plus backend.
	/// These tokens are valid for 2 hours after their issue date.
	token: String,
}

pub(super) fn router() -> ApiRouter<ApiState> {
	ApiRouter::new().api_route("/login", post_with(self::endpoint, self::endpoint_doc))
}

#[tracing::instrument(level = "debug", skip(state))]
async fn endpoint(
	State(state): State<ApiState>,
	Query(query): Query<LoginQuery>,
) -> Result<Json<LoginResponse>, LoginError> {
	let response = state
		.client
		.get("https://sessionserver.mojang.com/session/minecraft/hasJoined")
		.query(&[("username", query.username), ("serverId", query.server_id)])
		.send()
		.await
		.map_err(LoginError::SessionserverAuthentication)?
		.error_for_status()
		.map_err(LoginError::SessionserverAuthentication)?;

	// 200 w/ JSON data is returned on success
	if response.status() != StatusCode::OK {
		return Err(LoginError::Unauthorized);
	}
	let body = response
		.text()
		.await
		.map_err(LoginError::SessionserverAuthentication)?;
	let parsed: SessionserverLoginSuccess =
		serde_json::from_str(&body).map_err(|_| LoginError::Unauthorized)?;
	let player =
		entities::prelude::User::get_or_create(&state.database, parsed.id).await?;
	record_monthly_active_login(&state.database, player.id).await?;

	let token = local::encrypt(
		&state.paseto_key,
		&{
			let mut claims = Claims::new()?;

			claims.set_expires_in(&Duration::from_secs(60 * 60 * 2 /* 2h */))?;
			claims.subject(&parsed.id.as_hyphenated().to_string())?;

			claims
		},
		None,
		PASETO_IMPLICIT_ASSERT,
	)?;

	Ok(Json(LoginResponse { token }))
}
