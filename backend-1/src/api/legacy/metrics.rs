use std::fmt::Write;

use actix_web::{
	body::MessageBody,
	dev::{ServiceRequest, ServiceResponse},
	http::header,
	middleware::Next,
	web,
};
use documented::DocumentedFields;
use prometheus_client::{
	encoding::{EncodeLabelSet, EncodeLabelValue},
	metrics::{counter::Counter, family::Family},
	registry::Registry,
};
use serde::{Deserialize, Serialize};

use crate::{
	api::common::{data::ApiData, metrics::MetricsGroup},
	make_api_metric,
};

macro_rules! impl_serde_variant_as_label_value {
	($enum:ty) => {
		impl EncodeLabelValue for $enum {
			fn encode(
				&self,
				encoder: &mut prometheus_client::encoding::LabelValueEncoder,
			) -> Result<(), std::fmt::Error> {
				encoder.write_str(
					serde_variant::to_variant_name(self).map_err(|_| std::fmt::Error)?,
				)
			}
		}
	};
}

#[derive(Serialize, Deserialize, Debug, Hash, PartialEq, Eq, Clone)]
#[serde(rename_all = "lowercase")]
pub enum UserAgentType {
	Loader,
	Wrapper,
	Unknown,
}

#[derive(Serialize, Deserialize, Debug, Hash, PartialEq, Eq, Clone, EncodeLabelSet)]
pub struct OneConfigRequest {
	pub version: String,
	pub loader: String,
	pub user_agent_type: UserAgentType,
}

impl_serde_variant_as_label_value!(UserAgentType);

/// A struct containing all of the metrics state for the API
#[derive(DocumentedFields)]
pub struct ApiLegacyMetrics {
	/// The amount of OneConfig requests, by version, loader, and user agent
	/// type
	oneconfig_requests: Family<OneConfigRequest, Counter>,
}

impl MetricsGroup for ApiLegacyMetrics {
	fn init_metrics(registry: &mut Registry) -> Self {
		let registry = registry.sub_registry_with_prefix("legacy");

		make_api_metric!(registry, Self, oneconfig_requests);

		Self { oneconfig_requests }
	}
}

/// A middleware to increment all metrics per-request
pub async fn middleware(
	mut service_request: ServiceRequest,
	next: Next<impl MessageBody>,
) -> Result<ServiceResponse<impl MessageBody>, actix_web::Error> {
	let data = service_request.extract::<web::Data<ApiData>>().await?;

	#[allow(clippy::single_match)]
	match service_request
		.match_pattern()
		.unwrap_or("default".to_string())
		.as_str()
	{
		"/oneconfig/{version}-{loader}" => 'inner: {
			let Some((version, loader)) = service_request.uri().path()
				[(const { "/oneconfig/".len() })..]
				.split_once('-')
			else {
				break 'inner;
			};

			data.metrics
				.legacy
				.oneconfig_requests
				.get_or_create(&OneConfigRequest {
					version: version.to_owned(),
					loader: loader.to_owned(),
					user_agent_type: match service_request
						.headers()
						.get(header::USER_AGENT)
						.and_then(|v| v.to_str().ok())
					{
						Some(s) if s.contains("OneConfigLoader") => UserAgentType::Loader,
						Some(s) if s.contains("OneConfigWrapper") => {
							UserAgentType::Wrapper
						}
						Some(_) | None => UserAgentType::Unknown,
					},
				})
				.inc();
		}
		_ => (),
	};

	// Let the real request handler continue
	next.call(service_request).await
}
