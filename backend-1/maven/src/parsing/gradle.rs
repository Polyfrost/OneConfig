use std::{borrow::Cow, collections::HashMap};

use serde::Deserialize;
use serde_with::{BorrowCow, serde_as};

pub type ParseError = serde_json::Error;
// TODO: Typed attributes?
type Attributes<'a> = HashMap<Cow<'a, str>, AttributeValue<'a>>;

#[serde_as]
#[derive(Debug, Deserialize)]
#[serde(untagged)]
pub enum AttributeValue<'a> {
	String(#[serde_as(as = "BorrowCow")] Cow<'a, str>),
	Boolean(bool),
	Integer(i64),
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GradleModuleMetadata<'a> {
	#[serde(borrow)]
	pub component: Option<Component<'a>>,
	#[serde(borrow, default)]
	pub variants: Vec<Variant<'a>>,
}

#[serde_as]
#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Component<'a> {
	#[serde_as(as = "BorrowCow")]
	pub group: Cow<'a, str>,
	#[serde_as(as = "BorrowCow")]
	pub module: Cow<'a, str>,
	#[serde_as(as = "BorrowCow")]
	pub version: Cow<'a, str>,
	#[serde_as(as = "Option<BorrowCow>")]
	pub url: Option<Cow<'a, str>>,
	#[serde(borrow, default)]
	pub attributes: Attributes<'a>,
}

#[serde_as]
#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Variant<'a> {
	#[serde_as(as = "BorrowCow")]
	pub name: Cow<'a, str>,
	#[serde_as(as = "HashMap<BorrowCow, _>")]
	#[serde(borrow, default)]
	pub attributes: Attributes<'a>,
	#[serde(borrow, default)]
	pub dependencies: Vec<Dependency<'a>>,
	#[serde(borrow, default)]
	pub dependency_constraints: Vec<DependencyConstraints<'a>>,
	#[serde(borrow, default)]
	pub files: Vec<File<'a>>,
	#[serde(borrow, default)]
	pub capabilities: Vec<Capability<'a>>,
}

#[serde_as]
#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Capability<'a> {
	#[serde_as(as = "BorrowCow")]
	pub group: Cow<'a, str>,
	#[serde_as(as = "BorrowCow")]
	pub name: Cow<'a, str>,
	#[serde_as(as = "BorrowCow")]
	pub version: Cow<'a, str>,
}

#[serde_as]
#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Dependency<'a> {
	#[serde_as(as = "BorrowCow")]
	pub group: Cow<'a, str>,
	#[serde_as(as = "BorrowCow")]
	pub module: Cow<'a, str>,
	#[serde(borrow)]
	pub version: Option<VersionConstraint<'a>>,
	#[serde(borrow, default)]
	pub excludes: Vec<Excludes<'a>>,
	#[serde_as(as = "Option<BorrowCow>")]
	pub reason: Option<Cow<'a, str>>,
	#[serde_as(as = "HashMap<BorrowCow, _>")]
	#[serde(borrow, default)]
	pub attributes: Attributes<'a>,
	#[serde(borrow, default)]
	pub requested_capabilities: Vec<Capability<'a>>,
	#[serde(default)]
	pub endorse_strict_versions: bool,
	#[serde(borrow)]
	pub third_party_compatibility: Option<ThirdPartyCompatibility<'a>>,
}

#[serde_as]
#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DependencyConstraints<'a> {
	#[serde_as(as = "BorrowCow")]
	pub group: Cow<'a, str>,
	#[serde_as(as = "BorrowCow")]
	pub module: Cow<'a, str>,
	#[serde(borrow)]
	pub version: Option<VersionConstraint<'a>>,
	#[serde_as(as = "HashMap<BorrowCow, _>")]
	#[serde(borrow, default)]
	pub attributes: Attributes<'a>,
}

#[serde_as]
#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct File<'a> {
	#[serde_as(as = "BorrowCow")]
	pub name: Cow<'a, str>,
	#[serde_as(as = "BorrowCow")]
	pub url: Cow<'a, str>,
	pub size: u64,
	#[serde_as(as = "BorrowCow")]
	pub sha1: Cow<'a, str>,
	#[serde_as(as = "BorrowCow")]
	pub sha256: Cow<'a, str>,
	#[serde_as(as = "BorrowCow")]
	pub sha512: Cow<'a, str>,
	#[serde_as(as = "BorrowCow")]
	pub md5: Cow<'a, str>,
}

#[serde_as]
#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct VersionConstraint<'a> {
	#[serde_as(as = "Option<BorrowCow>")]
	pub requires: Option<Cow<'a, str>>,
	#[serde_as(as = "Option<BorrowCow>")]
	pub prefers: Option<Cow<'a, str>>,
	#[serde_as(as = "Option<BorrowCow>")]
	pub strictly: Option<Cow<'a, str>>,
	#[serde(default)]
	#[serde_as(as = "Vec<BorrowCow>")]
	pub rejects: Vec<Cow<'a, str>>,
}

#[serde_as]
#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Excludes<'a> {
	#[serde_as(as = "BorrowCow")]
	pub group: Cow<'a, str>,
	#[serde_as(as = "BorrowCow")]
	pub module: Cow<'a, str>,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ThirdPartyCompatibility<'a> {
	#[serde(borrow)]
	pub artifact_selector: ArtifactSelector<'a>,
}

#[serde_as]
#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ArtifactSelector<'a> {
	#[serde_as(as = "BorrowCow")]
	pub name: Cow<'a, str>,
	#[serde_as(as = "BorrowCow")]
	pub r#type: Cow<'a, str>,
	#[serde_as(as = "BorrowCow")]
	pub extension: Cow<'a, str>,
	#[serde_as(as = "BorrowCow")]
	pub classifier: Cow<'a, str>,
}

impl<'a> GradleModuleMetadata<'a> {
	pub fn parse_from_str(s: &'a str) -> Result<Self, serde_json::Error> {
		serde_json::from_str(s)
	}
}

#[cfg(test)]
mod tests {
	use super::*;

	const EXAMPLES: &[&str] = &[
		include_str!("../../tests/module/valid_metadata_1.module"),
		include_str!("../../tests/module/valid_metadata_2.module"),
	];

	#[test]
	fn test_parsing() {
		for metadata in EXAMPLES {
			let parsed = dbg!(GradleModuleMetadata::parse_from_str(metadata));
			assert!(parsed.is_ok());
		}
	}
}
