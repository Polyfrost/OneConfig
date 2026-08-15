use actix_web::web;
use maven::{parsing::MavenArtifactMetadata, types::ArtifactCoordinate};
use reqwest::IntoUrl;
use semver::{Prerelease, Version};

use crate::api::{common::data::ApiData, v1::responses::ArtifactErrorResponse};

pub async fn fetch_latest_artifact_version(
	state: &web::Data<ApiData>,
	url: impl IntoUrl,
) -> Result<Version, ArtifactErrorResponse> {
	let res = state
		// Fetch metadata with HTTP
		.client
		.get(url)
		.send()
		.await
		.map_err(ArtifactErrorResponse::MavenFetch)?
		// Error on non-2xx response codes
		.error_for_status()
		.map_err(ArtifactErrorResponse::MavenResponse)?
		// Decode response
		.text()
		.await
		.map_err(ArtifactErrorResponse::MavenMetadataDecoding)?;

	let parsed = MavenArtifactMetadata::parse_from_str(&res)
		.map_err(ArtifactErrorResponse::MavenMetadataParsing)?;

	// Get latest version by semver
	let latest = parsed
		.versioning
		.versions
		.into_iter()
		.filter_map(|v| semver::Version::parse(&v).ok())
		.map(|mut v| {
			// Check if version prerelease ID is formatted as alpha7 rather than alpha.7
			let mut last_alpha = false;
			let mut incorrect_pre = None::<usize>;
			for (i, c) in v.pre.chars().enumerate() {
				if last_alpha && c.is_ascii_digit() {
					incorrect_pre = Some(i);
				} else {
					last_alpha = c.is_ascii_alphabetic();
				}
			}

			// Reformat identifier if version pre is incorrect
			if let Some(incorrect_pre) = incorrect_pre
				&& let Ok(pre) = Prerelease::new(&format!(
					"{}.{}",
					&v.pre[..incorrect_pre],
					&v.pre[incorrect_pre..]
				)) {
				v.pre = pre;
			};

			v
		})
		.max()
		.ok_or(ArtifactErrorResponse::NoArtifactVersions)?;

	Ok(latest)
}

pub async fn fetch_artifact_checksum(
	state: &web::Data<ApiData>,
	checksum_url: &str,
) -> Result<String, ArtifactErrorResponse> {
	let res = state
		// Fetch metadata with HTTP
		.client
		.get(checksum_url)
		.send()
		.await
		.map_err(ArtifactErrorResponse::MavenFetch)?
		// Error on non-2xx response codes
		.error_for_status()
		.map_err(ArtifactErrorResponse::MavenResponse)?
		// Decode response
		.text()
		.await
		.map_err(ArtifactErrorResponse::MavenMetadataDecoding)?;

	Ok(res)
}

pub async fn fetch_gradle_module_metadata(
	state: &web::Data<ApiData>,
	repo_url: &str,
	coordinate: &ArtifactCoordinate<'_>,
) -> Result<String, ArtifactErrorResponse> {
	let res = state
		// Fetch metadata with HTTP
		.client
		.get(coordinate.to_module_metadata_url(repo_url))
		.send()
		.await
		.map_err(ArtifactErrorResponse::MavenFetch)?
		// Error on non-2xx response codes
		.error_for_status()
		.map_err(ArtifactErrorResponse::MavenResponse)?
		// Decode response
		.text()
		.await
		.map_err(ArtifactErrorResponse::MavenMetadataDecoding)?;

	Ok(res)
}
