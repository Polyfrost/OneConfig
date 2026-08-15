use actix_web::{ResponseError, http::StatusCode};
use serde::Serialize;
use thiserror::Error;

#[derive(Serialize)]
pub struct ArtifactResponse {
	pub group: String,
	pub name: String,
	pub jij: bool,
	pub checksum: Checksum,
	pub url: String, // signatures: TODO
}

#[derive(Serialize)]
pub struct Checksum {
	pub r#type: ChecksumType,
	pub hash: String,
}

#[derive(Serialize)]
pub enum ChecksumType {
	#[serde(rename = "SHA-256")]
	Sha256,
}

#[derive(Debug, Error)]
pub enum ArtifactErrorResponse {
	#[error("unable to connect to maven: {0}")]
	MavenFetch(#[source] reqwest::Error),
	#[error("maven request returned non-2xx status code: {0}")]
	MavenResponse(#[source] reqwest::Error),
	#[error("decoding maven metadata response failed: {0}")]
	MavenMetadataDecoding(#[source] reqwest::Error),
	#[error("parsing maven metadata response as XML failed: {0}")]
	MavenMetadataParsing(#[source] maven::parsing::maven::ParseError),
	#[error("parsing gradle metadata response as JSON failed: {0}")]
	GradleMetadataParsing(#[source] maven::parsing::gradle::ParseError),
	#[error("maven artifact metadata contained no versions")]
	NoArtifactVersions,
	#[error("oneconfig dependency had no version requirement")]
	NoDependencyVersion { group: String, artifact: String },
	#[error("fetching checksums of dependencies panicked: {0}")]
	ChecksumTaskFailure(#[source] tokio::task::JoinError),
	#[error("serializing JSON response failed: {0}")]
	ResponseSerialization(#[source] serde_json::Error),
}

impl ResponseError for ArtifactErrorResponse {
	fn status_code(&self) -> StatusCode {
		StatusCode::INTERNAL_SERVER_ERROR
	}

	// TODO: Implement RFC9457 problem details
}
