use std::borrow::Cow;

/// A Maven GAV(CE) coordinate uniquely specifying an artifact file
pub struct ArtifactCoordinate<'a> {
	pub(crate) group_id: Cow<'a, str>,
	pub(crate) artifact_id: Cow<'a, str>,
	pub(crate) version: Cow<'a, str>,
	pub(crate) base_version: Cow<'a, str>,
	pub(crate) classifier: Cow<'a, str>,
	pub(crate) extension: Cow<'a, str>,
}

impl<'a> ArtifactCoordinate<'a> {
	pub fn new(
		group_id: impl Into<Cow<'a, str>>,
		artifact_id: impl Into<Cow<'a, str>>,
		version: impl Into<Cow<'a, str>>,
	) -> Self {
		let version = version.into();
		ArtifactCoordinate {
			group_id: group_id.into(),
			artifact_id: artifact_id.into(),
			base_version: version.clone(),
			version,
			classifier: "".into(),
			extension: "jar".into(),
		}
	}

	pub fn group_id(&self) -> &str {
		&self.group_id
	}

	pub fn artifact_id(&self) -> &str {
		&self.artifact_id
	}

	pub fn version(&self) -> &str {
		&self.version
	}

	pub fn base_version(&self) -> &str {
		&self.base_version
	}

	pub fn classifier(&self) -> &str {
		&self.classifier
	}

	pub fn extension(&self) -> &str {
		&self.extension
	}

	pub fn with_base_version(mut self, base_version: impl Into<Cow<'a, str>>) -> Self {
		self.base_version = base_version.into();
		self
	}

	pub fn with_classifier(mut self, classifier: impl Into<Cow<'a, str>>) -> Self {
		self.classifier = classifier.into();
		self
	}

	pub fn with_extension(mut self, extension: impl Into<Cow<'a, str>>) -> Self {
		self.extension = extension.into();
		self
	}
}
