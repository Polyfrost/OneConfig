use actix_web::{ResponseError, http::StatusCode};
use serde::{Deserialize, Serialize};
use thiserror::Error;

#[derive(Serialize, Deserialize, Debug, Hash, PartialEq, Eq, Clone)]
pub enum LegacyVersion {
	#[serde(rename = "1.8.9")]
	OneEightNine,
	#[serde(rename = "1.12.2")]
	OneTwelveTwo,
}

#[derive(Serialize, Deserialize, Debug, Hash, PartialEq, Eq, Clone)]
#[serde(rename_all = "lowercase")]
pub enum LegacyLoader {
	Forge,
}

impl LegacyLoader {
	pub fn get_loader_type(&self) -> &'static str {
		match self {
			LegacyLoader::Forge => "launchwrapper",
		}
	}
}

#[derive(Serialize, Deserialize, Debug, Hash, PartialEq, Eq, Clone)]
pub struct LegacyArtifact {
	pub url: String,
	pub sha256: String,
}

#[derive(Serialize, Deserialize, Debug, Hash, PartialEq, Eq, Clone)]
pub struct LegacyArtifactsResponse {
	pub release: LegacyArtifact,
	pub snapshot: LegacyArtifact,
	pub loader: LegacyArtifact,
}

#[derive(Debug, Error)]
pub enum LegacyArtifactsErrorResponse {
	#[error("unable to serialize serde enum variant name: {0}")]
	EnumVariantSerialization(#[source] serde_variant::UnsupportedType),
	#[error("unable to connect to maven: {0}")]
	MavenFetch(#[source] reqwest::Error),
	#[error("maven request returned non-2xx status code: {0}")]
	MavenResponse(#[source] reqwest::Error),
	#[error("decoding maven metadata response failed: {0}")]
	MavenMetadataDecoding(#[source] reqwest::Error),
	#[error("parsing maven metadata response as XML failed: {0}")]
	MavenMetadataParsing(#[source] maven::parsing::maven::ParseError),
}

impl ResponseError for LegacyArtifactsErrorResponse {
	fn status_code(&self) -> StatusCode {
		StatusCode::INTERNAL_SERVER_ERROR
	}

	// TODO: Implement RFC9457 problem details
}
