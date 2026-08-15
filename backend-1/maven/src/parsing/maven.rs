use std::borrow::Cow;

use chrono::{DateTime, Utc};
use serde::Deserialize;

pub type ParseError = quick_xml::DeError;
pub type Timestamp = DateTime<Utc>;

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MavenArtifactMetadata<'a> {
	/// The groupId of this artifact
	#[serde(borrow)]
	pub group_id: Cow<'a, str>,
	/// The artifactId of this artifact
	#[serde(borrow)]
	pub artifact_id: Cow<'a, str>,
	/// Versioning information about this artifact
	#[serde(borrow)]
	pub versioning: ArtifactVersioning<'a>,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ArtifactVersioning<'a> {
	/// What the last version added to the directory is, including both releases
	/// and snapshots
	#[serde(borrow)]
	pub latest: Cow<'a, str>,
	/// What the last version added to the directory is, for the releases only
	#[serde(borrow)]
	pub release: Cow<'a, str>,
	/// Versions available of the artifact (both releases and snapshots)
	#[serde(borrow, deserialize_with = "crate::serde::derserialize_version_list")]
	pub versions: Vec<Cow<'a, str>>,
	/// When the metadata was last updated
	#[serde(deserialize_with = "crate::serde::deserialize_maven_timestamp")]
	pub last_updated: DateTime<Utc>,
}

impl<'a> MavenArtifactMetadata<'a> {
	pub fn get_metadata_url(
		repository_url: impl AsRef<str>,
		group_id: impl Into<Cow<'a, str>>,
		artifact_id: impl Into<Cow<'a, str>>,
	) -> String {
		let repository_url = repository_url.as_ref();

		let mut url = String::new();

		// Add repository prefix and ensure trailing slash
		url += repository_url;
		if repository_url.chars().last().is_none_or(|c| c != '/') {
			url.push('/');
		};

		// Append the group to the url
		url.extend(
			group_id
				.into()
				.chars()
				.map(|c| if c == '.' { '/' } else { c }),
		);
		url.push('/');

		// Add the artifact and maven-metadata.xml paths
		url += &artifact_id.into();
		url += "/maven-metadata.xml";

		url
	}

	pub fn parse_from_str(s: &'a str) -> Result<Self, quick_xml::DeError> {
		quick_xml::de::from_str(s)
	}
}

#[cfg(test)]
mod tests {
	use super::*;

	const EXAMPLE_URLS: &[(&str, &str, &str, &str)] = &[
		(
			"https://repo.polyfrost.org/releases/",
			"cc.polyfrost",
			"oneconfig-1.8.9-forge",
			"https://repo.polyfrost.org/releases/cc/polyfrost/oneconfig-1.8.9-forge/maven-metadata.xml",
		),
		(
			"https://maven.aliucord.com/snapshots",
			"com.aliucord",
			"gradle",
			"https://maven.aliucord.com/snapshots/com/aliucord/gradle/maven-metadata.xml",
		),
	];

	#[test]
	fn test_metadata_url() {
		for (repository, group, artifact, url) in EXAMPLE_URLS {
			assert_eq!(
				&MavenArtifactMetadata::get_metadata_url(repository, *group, *artifact),
				url,
				"MavenArtifactMetadata::get_metadata_url should return the correct \
				 artifact URL"
			);
		}
	}
}
