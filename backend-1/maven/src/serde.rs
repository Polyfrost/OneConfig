//! The module contains utilities to help with parsing maven metadata XML using
//! serde

use std::borrow::Cow;

use chrono::{DateTime, NaiveDateTime, Utc};
use serde::{Deserialize, Deserializer};
use serde_with::{BorrowCow, serde_as};

pub(crate) fn deserialize_maven_timestamp<'de, D: Deserializer<'de>>(
	deserializer: D,
) -> Result<DateTime<Utc>, D::Error> {
	let s = Cow::<'de, str>::deserialize(deserializer)?;

	Ok(NaiveDateTime::parse_from_str(&s, "%Y%m%d%H%M%S")
		.map_err(serde::de::Error::custom)?
		.and_utc())
}

pub(crate) fn derserialize_version_list<'de, D: Deserializer<'de>>(
	deserializer: D,
) -> Result<Vec<Cow<'de, str>>, D::Error> {
	/// Represents
	/// ```xml
	/// <list>
	///     <element>...</element>
	///     <element>...</element>
	///     <element>...</element>
	///     <!-- ... -->
	/// </list>
	/// ```
	#[serde_as]
	#[derive(Deserialize)]
	struct VersionList<'a> {
		#[serde(borrow, default)]
		#[serde_as(as = "Vec<BorrowCow>")]
		version: Vec<Cow<'a, str>>,
	}

	Ok(VersionList::deserialize(deserializer)?.version)
}
