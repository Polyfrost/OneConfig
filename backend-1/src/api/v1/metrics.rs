use actix_web::{
	body::MessageBody,
	dev::{ServiceRequest, ServiceResponse},
	middleware::Next,
	web,
};
use documented::DocumentedFields;
use prometheus_client::{
	encoding::EncodeLabelSet,
	metrics::{counter::Counter, family::Family},
	registry::Registry,
};

use crate::{
	api::{
		common::{data::ApiData, metrics::MetricsGroup},
		v1::endpoints::artifacts::{ArtifactQuery, OneConfigVersionInfo},
	},
	make_api_metric,
};

#[derive(Debug, Hash, PartialEq, Eq, Clone, EncodeLabelSet)]
struct PlatformAgnosticArtifactLabels {
	r#type: String,
}

/// A struct containing all of the metrics state for the API
#[derive(DocumentedFields)]
pub struct ApiV1Metrics {
	/// The amount of OneConfig artifacts requests, by version and loader
	oneconfig_artifacts_requests: Family<OneConfigVersionInfo, Counter>,
	/// The amount of platform-agnostic artifacts requests, by type
	platform_agnostic_artifacts_requests: Family<PlatformAgnosticArtifactLabels, Counter>,
}

impl MetricsGroup for ApiV1Metrics {
	fn init_metrics(registry: &mut Registry) -> Self {
		let registry = registry.sub_registry_with_prefix("v1");

		make_api_metric!(registry, Self, oneconfig_artifacts_requests);
		make_api_metric!(registry, Self, platform_agnostic_artifacts_requests);

		Self {
			oneconfig_artifacts_requests,
			platform_agnostic_artifacts_requests,
		}
	}
}

/// A middleware to increment all metrics per-request
pub async fn middleware(
	mut service_request: ServiceRequest,
	next: Next<impl MessageBody>,
) -> Result<ServiceResponse<impl MessageBody>, actix_web::Error> {
	let data = service_request.extract::<web::Data<ApiData>>().await?;

	match service_request
		.match_pattern()
		.unwrap_or("default".to_string())
		.as_str()
	{
		"/v1/artifacts/oneconfig" => {
			data.metrics
				.v1
				.oneconfig_artifacts_requests
				.get_or_create(
					&service_request
						.extract::<web::Query<ArtifactQuery<OneConfigVersionInfo>>>()
						.await?
						.version_info,
				)
				.inc();
		}
		"/v1/artifacts/{artifact:stage1|relaunch}" => {
			data.metrics
				.v1
				.platform_agnostic_artifacts_requests
				.get_or_create(&PlatformAgnosticArtifactLabels {
					// Unfortunately actix makes it difficult to extract the real
					// parsed URL parameter, so just substring instead as a substitute
					r#type:
						service_request.uri().path()[const { "/v1/artifacts/".len() }..]
							.to_string(),
				})
				.inc();
		}
		_ => (),
	};

	// Let the real request handler continue
	next.call(service_request).await
}
