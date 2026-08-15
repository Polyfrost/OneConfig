use actix_web::{
	HttpResponse, Responder,
	body::MessageBody,
	dev::{ServiceRequest, ServiceResponse},
	get,
	middleware::Next,
	web::{self, ServiceConfig},
};
use documented::DocumentedFields;
use prometheus_client::{
	encoding::{EncodeLabelSet, text::encode},
	metrics::{counter::Counter, family::Family},
	registry::Registry,
};

use crate::api::{
	common::{caching::CacheLabels, data::ApiData},
	legacy::metrics::ApiLegacyMetrics,
	v1::metrics::ApiV1Metrics,
};

// TODO: Improve this macro so you can use only one macro call and it will
// register all of them

/// A macro that automatically initializes and registers a metric using inferred
/// types and doc comments from the ApiMetrics struct
#[macro_export]
macro_rules! make_api_metric {
	($registry:expr, $metrics_struct:ident, $name:ident) => {
		make_api_metric!($registry, $metrics_struct, $name, Family)
	};
	($registry:expr, $metrics_struct:ident, $name:ident, $type:ident) => {
		let $name = $type::default();
		let name_str = stringify!($name);
		$registry.register(
			name_str,
			$metrics_struct::get_field_docs(name_str)
				.expect(&format!("No doc comment for '{}' field", name_str))
				.replace('\n', ""),
			$name.clone(),
		);
	};
}

pub trait MetricsGroup {
	fn init_metrics(registry: &mut Registry) -> Self;
}

pub struct AppMetrics {
	/// The root metrics registry used for storing and retrieving metrics
	pub registry: Registry,
	/// All of the global (application-wide) metrics
	pub global: GlobalMetrics,
	/// All of the legacy API metrics
	pub legacy: ApiLegacyMetrics,
	/// All of the API v1 metrics
	pub v1: ApiV1Metrics,
}

impl AppMetrics {
	pub fn new() -> AppMetrics {
		let mut registry = <Registry>::default();

		AppMetrics {
			global: GlobalMetrics::init_metrics(&mut registry),
			legacy: ApiLegacyMetrics::init_metrics(&mut registry),
			v1: ApiV1Metrics::init_metrics(&mut registry),
			registry,
		}
	}
}

#[derive(DocumentedFields)]
pub struct GlobalMetrics {
	/// The amount of generic API requests by endpoint and status code
	api_requests: Family<ApiRequestLabels, Counter>,
	/// The amount of cache hits by endpoint
	pub cache_hits: Family<CacheLabels, Counter>,
	/// The amount of cache misses by endpoint
	pub cache_misses: Family<CacheLabels, Counter>,
}

impl MetricsGroup for GlobalMetrics {
	fn init_metrics(registry: &mut Registry) -> Self {
		make_api_metric!(registry, Self, api_requests);
		make_api_metric!(registry, Self, cache_hits);
		make_api_metric!(registry, Self, cache_misses);

		Self {
			api_requests,
			cache_hits,
			cache_misses,
		}
	}
}

pub fn configure(config: &mut ServiceConfig) {
	config.service(metrics_endpoint);
}

/// The endpoint to allow scraping metrics
#[get("/metrics")]
async fn metrics_endpoint(state: web::Data<ApiData>) -> impl Responder {
	let mut body = String::new();
	if let Err(e) = encode(&mut body, &state.metrics.registry) {
		return HttpResponse::InternalServerError()
			.content_type("text/plain")
			.body(format!("Error encoding metrics: {e}"));
	}

	HttpResponse::Ok()
		.content_type("application/openmetrics-text; version=1.0.0; charset=utf-8")
		.body(body)
}

#[derive(Debug, Hash, PartialEq, Eq, Clone, EncodeLabelSet)]
struct ApiRequestLabels {
	path: String,
	status_code: u16,
}

pub async fn middleware(
	mut service_request: ServiceRequest,
	next: Next<impl MessageBody>,
) -> Result<ServiceResponse<impl MessageBody>, actix_web::Error> {
	let data = service_request.extract::<web::Data<ApiData>>().await?;

	let path = service_request.uri().path().to_string();
	let response = next.call(service_request).await;

	let labels = match &response {
		Ok(r) => ApiRequestLabels {
			path,
			status_code: r.status().as_u16(),
		},
		Err(e) => ApiRequestLabels {
			path,
			status_code: e.as_response_error().status_code().as_u16(),
		},
	};
	data.metrics
		.global
		.api_requests
		.get_or_create(&labels)
		.inc();

	response
}
