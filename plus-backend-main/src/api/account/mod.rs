mod login;

use aide::{OperationInput, axum::ApiRouter, openapi::SecurityRequirement};
use axum::{
	extract::FromRequestParts,
	http::request::Parts,
	response::{IntoResponse, Response},
};
use http::StatusCode;
use pasetors::{
	Local, claims::ClaimsValidationRules, local, token::UntrustedToken, version4::V4,
};
use reqwest::header::AUTHORIZATION;
use uuid::Uuid;

use crate::{api::ApiState, database::DatabaseUserExt};
use entities::{
	prelude::*, sea_orm_active_enums, sea_orm_active_enums::PlayerRole, user,
};

pub const OPENAPI_SECURITY_NAME: &str = "Bearer Token";
pub const PASETO_IMPLICIT_ASSERT: Option<&[u8]> = Some(b"plus-backend");

#[derive(Debug)]
pub struct AuthenticationExtractor(pub Uuid);
#[derive(Debug)]
pub struct OptionalAuthenticationExtractor(pub Option<Uuid>);
#[derive(Debug)]
pub struct AuthenticatedPlayer(pub user::Model);
#[derive(Debug)]
pub struct AdminPlayer(pub user::Model);

impl OperationInput for AuthenticationExtractor {
	fn operation_input(
		_ctx: &mut aide::generate::GenContext,
		operation: &mut aide::openapi::Operation,
	) {
		operation.security.push(SecurityRequirement::from([(
			OPENAPI_SECURITY_NAME.to_string(),
			Vec::new(),
		)]));
	}
}

impl OperationInput for OptionalAuthenticationExtractor {
	fn operation_input(
		_ctx: &mut aide::generate::GenContext,
		operation: &mut aide::openapi::Operation,
	) {
		operation.security.extend([
			SecurityRequirement::default(),
			SecurityRequirement::from([(OPENAPI_SECURITY_NAME.to_string(), Vec::new())]),
		]);
	}
}

impl OperationInput for AuthenticatedPlayer {
	fn operation_input(
		ctx: &mut aide::generate::GenContext,
		operation: &mut aide::openapi::Operation,
	) {
		AuthenticationExtractor::operation_input(ctx, operation);
	}
}

impl OperationInput for AdminPlayer {
	fn operation_input(
		ctx: &mut aide::generate::GenContext,
		operation: &mut aide::openapi::Operation,
	) {
		AuthenticationExtractor::operation_input(ctx, operation);
	}
}

const MISSING_AUTHORIZATION_ERR: (StatusCode, &str) =
	(StatusCode::UNAUTHORIZED, "Authorization header was missing");
const INVALID_AUTHORIZATION_ERR: (StatusCode, &str) =
	(StatusCode::UNAUTHORIZED, "Authorization header was invalid");
const BLACKLISTED_ERR: (StatusCode, &str) =
	(StatusCode::FORBIDDEN, "Authenticated player is blacklisted");
const INSUFFICIENT_ROLE_ERR: (StatusCode, &str) = (
	StatusCode::FORBIDDEN,
	"Authenticated player does not have permission",
);

impl FromRequestParts<ApiState> for AuthenticationExtractor {
	type Rejection = Response;

	async fn from_request_parts(
		parts: &mut Parts,
		state: &ApiState,
	) -> Result<Self, Self::Rejection> {
		let header = parts
			.headers
			.get(AUTHORIZATION)
			.ok_or(MISSING_AUTHORIZATION_ERR)
			.map_err(IntoResponse::into_response)?
			.to_str()
			.map_err(|_| INVALID_AUTHORIZATION_ERR.into_response())?;

		let Some(token) = header.strip_prefix("Bearer ") else {
			return Err(INVALID_AUTHORIZATION_ERR.into_response());
		};

		let token = local::decrypt(
			&state.paseto_key,
			&UntrustedToken::<Local, V4>::try_from(token)
				.map_err(|_| INVALID_AUTHORIZATION_ERR.into_response())?,
			&ClaimsValidationRules::new(),
			None,
			PASETO_IMPLICIT_ASSERT,
		)
		.map_err(|_| INVALID_AUTHORIZATION_ERR.into_response())?;
		let claims = token
			.payload_claims()
			.ok_or(MISSING_AUTHORIZATION_ERR.into_response())?;

		let sub = claims
			.get_claim("sub")
			.ok_or(MISSING_AUTHORIZATION_ERR.into_response())?
			.as_str()
			.ok_or(MISSING_AUTHORIZATION_ERR.into_response())?;

		Uuid::parse_str(sub)
			.map(Self)
			.map_err(|_| MISSING_AUTHORIZATION_ERR.into_response())
	}
}

impl FromRequestParts<ApiState> for OptionalAuthenticationExtractor {
	type Rejection = <AuthenticationExtractor as FromRequestParts<ApiState>>::Rejection;

	async fn from_request_parts(
		parts: &mut Parts,
		state: &ApiState,
	) -> Result<Self, Self::Rejection> {
		Ok(Self(if parts.headers.contains_key(AUTHORIZATION) {
			Some(
				AuthenticationExtractor::from_request_parts(parts, state)
					.await?
					.0,
			)
		} else {
			None
		}))
	}
}

impl FromRequestParts<ApiState> for AuthenticatedPlayer {
	type Rejection = Response;

	async fn from_request_parts(
		parts: &mut Parts,
		state: &ApiState,
	) -> Result<Self, Self::Rejection> {
		let uuid = AuthenticationExtractor::from_request_parts(parts, state)
			.await?
			.0;
		let player = User::get_or_create(&state.database, uuid)
			.await
			.map_err(|e| {
				(
					StatusCode::INTERNAL_SERVER_ERROR,
					format!("Unable to load authenticated player: {e}"),
				)
					.into_response()
			})?;

		if player.blacklisted {
			return Err(BLACKLISTED_ERR.into_response());
		}

		Ok(Self(player))
	}
}

impl FromRequestParts<ApiState> for AdminPlayer {
	type Rejection = Response;

	async fn from_request_parts(
		parts: &mut Parts,
		state: &ApiState,
	) -> Result<Self, Self::Rejection> {
		let player = AuthenticatedPlayer::from_request_parts(parts, state)
			.await?
			.0;

		if !role_at_least(&player.role, &PlayerRole::Admin) {
			return Err(INSUFFICIENT_ROLE_ERR.into_response());
		}

		Ok(Self(player))
	}
}

pub(super) fn role_at_least(
	actual: &sea_orm_active_enums::PlayerRole,
	required: &sea_orm_active_enums::PlayerRole,
) -> bool {
	role_rank(actual) >= role_rank(required)
}

fn role_rank(role: &sea_orm_active_enums::PlayerRole) -> u8 {
	match role {
		sea_orm_active_enums::PlayerRole::Player => 0,
		sea_orm_active_enums::PlayerRole::Moderator => 1,
		sea_orm_active_enums::PlayerRole::Admin => 2,
	}
}

#[cfg(test)]
mod tests {
	use entities::sea_orm_active_enums::PlayerRole;

	use super::role_at_least;

	#[test]
	fn role_order_allows_elevated_access() {
		assert!(role_at_least(&PlayerRole::Admin, &PlayerRole::Moderator));
		assert!(role_at_least(&PlayerRole::Moderator, &PlayerRole::Player));
		assert!(!role_at_least(&PlayerRole::Player, &PlayerRole::Moderator));
	}
}

pub(super) async fn setup_router() -> ApiRouter<ApiState> {
	ApiRouter::new().merge(login::router())
}
