use crate::types::ArtifactCoordinate;

// TODO: Improve the duplicated lines here/clean this up
impl<'a> ArtifactCoordinate<'a> {
	/// Constructs the URL that can be used to download this artifact
	pub fn to_artifact_url(&self, repository_url: impl AsRef<str>) -> String {
		let repository_url = repository_url.as_ref();

		let mut url = String::new();

		// Add repository prefix and ensure trailing slash
		url += repository_url;
		if repository_url.chars().last().is_none_or(|c| c != '/') {
			url.push('/');
		};

		// Append the group to the url
		url.extend(
			self.group_id
				.chars()
				.map(|c| if c == '.' { '/' } else { c }),
		);
		url.push('/');

		// Add the artifact and version directories
		url += &self.artifact_id;
		url.push('/');
		url += &self.base_version;
		url.push('/');

		// Construct the actual artifact file name
		url += &self.artifact_id;
		url.push('-');
		url += &self.version;
		if !self.classifier.is_empty() {
			url.push('-');
			url += &self.classifier;
		}
		url.push('.');
		url += &self.extension;

		// Return final url
		url
	}

	/// Constructs the URL that can be used to download this artifact
	pub fn to_module_metadata_url(&self, repository_url: impl AsRef<str>) -> String {
		let repository_url = repository_url.as_ref();

		let mut url = String::new();

		// Add repository prefix and ensure trailing slash
		url += repository_url;
		if repository_url.chars().last().is_none_or(|c| c != '/') {
			url.push('/');
		};

		// Append the group to the url
		url.extend(
			self.group_id
				.chars()
				.map(|c| if c == '.' { '/' } else { c }),
		);
		url.push('/');

		// Add the artifact and version directories
		url += &self.artifact_id;
		url.push('/');
		url += &self.base_version;
		url.push('/');

		// Construct the actual .module file name
		url += &self.artifact_id;
		url.push('-');
		url += &self.version;
		url += ".module";

		// Return final url
		url
	}

	/// Constructs the URL that can be used to retrieve a checksum of this
	/// artifact
	pub fn to_checksum_url(
		&self,
		repository_url: impl AsRef<str>,
		format: &str,
	) -> String {
		let mut url = self.to_artifact_url(repository_url);
		url.push('.');
		url += format;

		url
	}

	/// Constructs the URL that can be used to retrieve the SHA256 checksum of
	/// this artifact
	pub fn to_sha256_url(&self, repository_url: impl AsRef<str>) -> String {
		self.to_checksum_url(repository_url, "sha256")
	}
}

#[cfg(test)]
mod tests {
	use std::borrow::Cow;

	use super::*;

	const EXAMPLE_ARTIFACTS: &[(&str, ArtifactCoordinate, &str, &str, &str, &str)] = &[
		(
			"https://repo.polyfrost.org/releases/",
			ArtifactCoordinate {
				group_id: Cow::Borrowed("cc.polyfrost"),
				artifact_id: Cow::Borrowed("oneconfig-1.8.9-forge"),
				version: Cow::Borrowed("0.2.2-alpha223"),
				base_version: Cow::Borrowed("0.2.2-alpha223"),
				classifier: Cow::Borrowed("full"),
				extension: Cow::Borrowed("jar"),
			},
			"https://repo.polyfrost.org/releases/cc/polyfrost/oneconfig-1.8.9-forge/0.2.2-alpha223/oneconfig-1.8.9-forge-0.2.2-alpha223-full.jar",
			"https://repo.polyfrost.org/releases/cc/polyfrost/oneconfig-1.8.9-forge/0.2.2-alpha223/oneconfig-1.8.9-forge-0.2.2-alpha223.module",
			"https://repo.polyfrost.org/releases/cc/polyfrost/oneconfig-1.8.9-forge/0.2.2-alpha223/oneconfig-1.8.9-forge-0.2.2-alpha223-full.jar.sha256",
			"https://repo.polyfrost.org/releases/cc/polyfrost/oneconfig-1.8.9-forge/0.2.2-alpha223/oneconfig-1.8.9-forge-0.2.2-alpha223-full.jar.md5",
		),
		(
			"https://maven.aliucord.com/snapshots",
			ArtifactCoordinate {
				group_id: Cow::Borrowed("com.aliucord"),
				artifact_id: Cow::Borrowed("gradle"),
				version: Cow::Borrowed("main-20241225.011801-8"),
				base_version: Cow::Borrowed("main-SNAPSHOT"),
				classifier: Cow::Borrowed(""),
				extension: Cow::Borrowed("jar"),
			},
			"https://maven.aliucord.com/snapshots/com/aliucord/gradle/main-SNAPSHOT/gradle-main-20241225.011801-8.jar",
			"https://maven.aliucord.com/snapshots/com/aliucord/gradle/main-SNAPSHOT/gradle-main-20241225.011801-8.module",
			"https://maven.aliucord.com/snapshots/com/aliucord/gradle/main-SNAPSHOT/gradle-main-20241225.011801-8.jar.sha256",
			"https://maven.aliucord.com/snapshots/com/aliucord/gradle/main-SNAPSHOT/gradle-main-20241225.011801-8.jar.md5",
		),
	];

	#[test]
	fn test_artifact_url() {
		for (repository, artifact, url, module_url, sha256_url, md5_url) in
			EXAMPLE_ARTIFACTS
		{
			assert_eq!(
				(
					artifact.to_artifact_url(repository).as_str(),
					artifact.to_module_metadata_url(repository).as_str(),
					artifact.to_sha256_url(repository).as_str(),
					artifact.to_checksum_url(repository, "md5").as_str(),
				),
				(*url, *module_url, *sha256_url, *md5_url),
				"Artifact::to_artifact_url should return the correct artifact URLs"
			);
		}
	}
}
