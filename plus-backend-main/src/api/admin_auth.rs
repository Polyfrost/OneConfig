use aide::{OperationInput, openapi::SecurityRequirement};
use axum::{
	extract::FromRequestParts,
	http::{StatusCode, request::Parts},
	response::{IntoResponse, Response},
};

use crate::api::ApiState;

pub struct AdminAuthenticationExtractor;

impl OperationInput for AdminAuthenticationExtractor {
	fn operation_input(
		_ctx: &mut aide::generate::GenContext,
		operation: &mut aide::openapi::Operation,
	) {
		operation.security.push(SecurityRequirement::from([(
			"Admin Password".to_string(),
			Vec::new(),
		)]));
	}
}

impl FromRequestParts<ApiState> for AdminAuthenticationExtractor {
	type Rejection = Response;

	async fn from_request_parts(
		parts: &mut Parts,
		state: &ApiState,
	) -> Result<Self, Self::Rejection> {
		let auth_header = parts
			.headers
			.get("Authorization")
			.and_then(|h| h.to_str().ok());

		match auth_header {
			Some(h) if h == state.admin_password => Ok(Self),
			_ => Err((
				StatusCode::UNAUTHORIZED,
				"Invalid or missing admin password",
			)
				.into_response()),
		}
	}
}
