use actix_web::{
	HttpResponse, HttpResponseBuilder,
	body::{BoxBody, EitherBody, MessageBody},
	dev::{ServiceRequest, ServiceResponse},
	http::{
		StatusCode,
		header::{ETAG, HeaderMap, HeaderValue, IF_NONE_MATCH},
	},
	middleware::Next,
	web::{self, Bytes},
};
use prometheus_client::encoding::EncodeLabelSet;
use sha2::{Digest as _, Sha256};

use crate::api::common::data::ApiData;

#[derive(Debug, Hash, PartialEq, Eq, Clone, EncodeLabelSet)]
pub struct CacheLabels {
	endpoint: String,
}

#[derive(Hash, PartialEq, Eq, Clone)]
pub struct CacheKey {
	pub path: String,
	pub query: String,
}

pub type ETagType = [u8; 32];

#[derive(Clone)]
pub struct CacheValue {
	pub response: Bytes,
	pub headers: HeaderMap,
	pub status: StatusCode,
	pub etag: ETagType,
}

pub async fn middleware(
	service_request: ServiceRequest,
	next: Next<impl MessageBody>,
) -> Result<ServiceResponse<EitherBody<impl MessageBody>>, actix_web::Error> {
	let match_pattern = service_request
		.match_pattern()
		.unwrap_or("default".to_string());
	let Some(app_data) = service_request.app_data::<web::Data<ApiData>>() else {
		// If we don't have ApiData for whatever reason, we can't do much
		// cache-related Technically this could probably be an unwrap, but this is
		// cleaner
		return next
			.call(service_request)
			.await
			.map(|resp| resp.map_into_left_body());
	};

	// Check whether the endpoint should actually be cached
	if !app_data.cache_allowlist.contains(match_pattern.as_str()) {
		return next
			.call(service_request)
			.await
			.map(|resp| resp.map_into_left_body());
	}

	let cache = app_data.cache.clone();
	let metric_labels = CacheLabels {
		endpoint: match_pattern,
	};
	let cache_key = CacheKey {
		path: service_request.path().to_string(),
		query: service_request.query_string().to_string(),
	};

	// Get and parse If-None-Match condition if is a valid Sha256 ETag
	let if_none_match = service_request
		.headers()
		.get(IF_NONE_MATCH)
		.and_then(|v| {
			base16ct::lower::decode_vec(v)
				.ok()
				.and_then(|vv| Some((vv, v.to_str().ok()?.to_owned())))
		})
		.and_then(|v| Some((TryInto::<[u8; 32]>::try_into(v.0).ok()?, v.1)));

	// Resolve cache entry with path & query
	if let Some(cache_value) = cache.get(&cache_key).await {
		// Record a cache hit to the metrics
		app_data
			.metrics
			.global
			.cache_hits
			.get_or_create(&metric_labels)
			.inc();

		// Short circuit with HttpResponse::NotModified() if the If-None-Match header
		// matches cache
		if let Some((if_none_match, etag)) = if_none_match
			&& cache_value.etag == if_none_match
		{
			let mut res = HttpResponse::NotModified()
				.append_header((ETAG, etag))
				.body(());

			let headers = res.headers_mut();
			for (name, value) in cache_value.headers {
				headers.append(name, value);
			}

			return Ok(service_request.into_response(res).map_into_right_body());
		}

		let mut res = HttpResponseBuilder::new(cache_value.status)
			.append_header((
				ETAG,
				base16ct::lower::encode_string(cache_value.etag.as_ref()),
			))
			.body(cache_value.response);

		let headers = res.headers_mut();
		for (name, value) in cache_value.headers {
			headers.append(name, value);
		}

		return Ok(service_request.into_response(res).map_into_right_body());
	} else {
		// Record a cache miss to the metrics
		app_data
			.metrics
			.global
			.cache_misses
			.get_or_create(&metric_labels)
			.inc();
	}

	// If none of the caching cases were handled, pass through to other handlers
	let response = next.call(service_request).await;

	if let Ok(response) = response {
		// Deconstruct the response
		let (req, res) = response.into_parts();
		let (mut res, body) = res.into_parts();

		let Ok(bytes) = actix_web::body::to_bytes(body).await else {
			return Ok(ServiceResponse::new(
				req,
				HttpResponse::InternalServerError().body(
					"Unable to read response bytes for caching, should never happen",
				),
			)
			.map_into_right_body());
		};

		let etag: [u8; 32] = Sha256::digest(&bytes).into();
		if res.status().is_success() {
			// Only cache successful requests to avoid caching errors
			cache
				.insert(
					cache_key,
					CacheValue {
						response: bytes.clone(),
						headers: res.headers().to_owned(),
						status: res.status(),
						etag,
					},
				)
				.await;
		}

		let etag_str = &mut [0u8; 64];
		base16ct::lower::encode_str(&etag, etag_str)
			.expect("etag_str slice was the wrong length");
		res.headers_mut()
			.append(ETAG, HeaderValue::from_bytes(etag_str)?);

		Ok(ServiceResponse::new(
			req,
			res.set_body(BoxBody::new(bytes)).map_into_right_body(),
		))
	} else {
		response.map(|v| v.map_into_left_body())
	}
}
