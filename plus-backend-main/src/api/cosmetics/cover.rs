use base64::Engine;
use entities::sea_orm_active_enums::{BodySlot, CosmeticType};
use reqwest::Client;
use serde::Serialize;

#[derive(thiserror::Error, Debug)]
pub(super) enum RenderError {
	#[error("Render request failed: {0}")]
	Request(#[from] reqwest::Error),
	#[error("Render service returned {status}: {body}")]
	Status { status: u16, body: String },
}

#[derive(Serialize)]
struct RenderRequest<'a> {
	r#type: &'a CosmeticType,
	slots: &'a [BodySlot],
	#[serde(skip_serializing_if = "Option::is_none")]
	model_variant: Option<&'a str>,
	is_bundle: bool,
	asset_b64: String,
}

#[tracing::instrument(level = "debug", skip(client, data), fields(bytes = data.len()))]
pub(super) async fn render_cover(
	client: &Client,
	base_url: &str,
	cosmetic_type: &CosmeticType,
	slots: &[BodySlot],
	model_variant: Option<&str>,
	is_bundle: bool,
	data: &[u8],
) -> Result<Vec<u8>, RenderError> {
	let request = RenderRequest {
		r#type: cosmetic_type,
		slots,
		model_variant,
		is_bundle,
		asset_b64: base64::engine::general_purpose::STANDARD.encode(data),
	};

	let url = format!("{}/render", base_url.trim_end_matches('/'));
	let response = client.post(url).json(&request).send().await?;

	let status = response.status();
	if !status.is_success() {
		let body = response.text().await.unwrap_or_default();
		return Err(RenderError::Status {
			status: status.as_u16(),
			body,
		});
	}

	Ok(response.bytes().await?.to_vec())
}
