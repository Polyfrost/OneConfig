#![feature(cow_is_borrowed)]
use std::borrow::Cow;

use maven::parsing::maven::MavenArtifactMetadata;

const VALID_METADATA: &str = include_str!("xml/valid_metadata.xml");
const VALID_METADATA_ENTITIES: &str = include_str!("xml/valid_metadata_entities.xml");

/// A test to ensure that the strings are properly no-copy when possible
#[test]
fn main() {
	let parsed = MavenArtifactMetadata::parse_from_str(VALID_METADATA)
		.expect("Should parse XML correctly");

	assert!(
		parsed.group_id.is_borrowed() && parsed.artifact_id.is_borrowed(),
		"Parsed strings should be correctly borrowed"
	);
	assert!(
		parsed.versioning.versions.iter().all(|v| v.is_borrowed()),
		"All parsed versions should be correctly borrowed"
	);

	println!("Parsed metadata:\n{parsed:?}");

	let parsed = MavenArtifactMetadata::parse_from_str(VALID_METADATA_ENTITIES)
		.expect("Should parse XML with entities correctly");

	assert!(
		matches!(
			(&parsed.group_id, &parsed.artifact_id),
			(Cow::Borrowed(_), Cow::Owned(_))
		),
		"Parsed strings with entities should be correctly owned"
	);
	assert!(
		parsed.versioning.versions.iter().all(|v| v.is_owned()),
		"All parsed versions should be correctly owned due to entities"
	);
}
