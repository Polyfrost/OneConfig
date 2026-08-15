use actix_web::web;
use maven::{
	parsing::{MavenArtifactMetadata, maven::Timestamp},
	types::ArtifactCoordinate,
};

use crate::api::{
	common::data::ApiData,
	legacy::types::{LegacyArtifact, LegacyArtifactsErrorResponse},
};

pub async fn get_latest_artifact(
	state: &web::Data<ApiData>,
	repository: &str,
	group_id: &str,
	artifact_id: &str,
	classifier: Option<&str>,
) -> Result<(LegacyArtifact, Timestamp), LegacyArtifactsErrorResponse> {
	let internal_repository_url = state.internal_maven_url.clone() + repository;
	let public_repository_url = state.public_maven_url.clone() + repository;

	let res = state
		// Fetch metadata with HTTP
		.client
		.get(MavenArtifactMetadata::get_metadata_url(&internal_repository_url, group_id, artifact_id))
		.send()
		.await
		.map_err(LegacyArtifactsErrorResponse::MavenFetch)?
		// Error on non-2xx response codes
		.error_for_status()
		.map_err(LegacyArtifactsErrorResponse::MavenResponse)?
		// Decode response
		.text()
		.await
		.map_err(LegacyArtifactsErrorResponse::MavenMetadataDecoding)?;

	let parsed = MavenArtifactMetadata::parse_from_str(&res)
		.map_err(LegacyArtifactsErrorResponse::MavenMetadataParsing)?;

	let coordinate =
		ArtifactCoordinate::new(group_id, artifact_id, parsed.versioning.latest);
	let coordinate = if let Some(classifier) = classifier {
		coordinate.with_classifier(classifier)
	} else {
		coordinate
	};

	let checksum = state
		.client
		// Fetch sha256 url
		.get(coordinate.to_sha256_url(&internal_repository_url))
		.send()
		.await
		.map_err(LegacyArtifactsErrorResponse::MavenFetch)?
		// Error on non-2xx response codes
		.error_for_status()
		.map_err(LegacyArtifactsErrorResponse::MavenResponse)?
		// Decode response as UTF8
		.text()
		.await
		.map_err(LegacyArtifactsErrorResponse::MavenMetadataDecoding)?;

	Ok((
		LegacyArtifact {
			url: coordinate.to_artifact_url(&public_repository_url),
			sha256: checksum,
		},
		parsed.versioning.last_updated,
	))
}
