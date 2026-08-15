use std::{collections::HashSet, sync::Arc, time::Duration};

use moka::future::Cache;

use super::caching::{CacheKey, CacheValue};
use crate::{
	AppCommand,
	api::common::{caching::ETagType, metrics::AppMetrics},
};

pub struct ApiData {
	/// The maven URL prefix to expose publicly, for example https://repo.polyfrost.org/
	pub public_maven_url: String,
	/// The maven URL prefix to resolve artifacts internally, for example https://172.19.0.3:8080/
	pub internal_maven_url: String,
	/// A reqwest client to use to fetch maven data
	pub client: Arc<reqwest::Client>,
	/// The allowlist of paths that should be cached
	pub cache_allowlist: HashSet<&'static str>,
	/// The internal cache used to cache artifact responses.
	pub cache: Cache<CacheKey, CacheValue>,
	/// All the metrics objects used for encoding and recording metrics
	pub metrics: AppMetrics,
}

impl ApiData {
	pub fn new(args: &AppCommand) -> Self {
		Self {
			public_maven_url: args.public_maven_url.to_string(),
			internal_maven_url: args
				.internal_maven_url
				.as_ref()
				.map_or(args.public_maven_url.to_string(), |u| u.to_string()),
			client: reqwest::ClientBuilder::new()
				.user_agent(concat!(
					env!("CARGO_PKG_NAME"),
					"/",
					env!("CARGO_PKG_VERSION"),
					" (",
					env!("CARGO_PKG_REPOSITORY"),
					")"
				))
				.build()
				.unwrap()
				.into(),
			cache_allowlist: HashSet::from([
				"/oneconfig/{version}-{loader}",
				"/v1/artifacts/oneconfig",
				"/v1/artifacts/{artifact:stage1|relaunch}",
			]),
			cache: Cache::builder()
				.time_to_live(Duration::from_mins(2))
				.weigher(|k: &CacheKey, v: &CacheValue| {
					(k.path.len()
						+ k.query.len() + const { std::mem::size_of::<ETagType>() }
						+ v.response.len() + std::mem::size_of_val(&v.headers))
					.try_into()
					.unwrap_or(u32::MAX)
				})
				.max_capacity(/* 10 MiB */ const { 10 * 1024 * 1024 })
				.build(),
			metrics: AppMetrics::new(),
		}
	}
}
