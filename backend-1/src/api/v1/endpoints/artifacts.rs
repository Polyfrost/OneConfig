use std::fmt::{Display, Write};

use actix_web::{
	HttpResponse, Responder, get,
	web::{self, ServiceConfig},
};
use maven::{
	parsing::{GradleModuleMetadata, MavenArtifactMetadata, gradle::AttributeValue},
	types::ArtifactCoordinate,
};
use prometheus_client::encoding::{EncodeLabelSet, EncodeLabelValue};
use reqwest::StatusCode;
use serde::{Deserialize, Serialize};
use tokio::task::JoinSet;

use crate::api::{
	common::data::ApiData,
	v1::{
		responses::{ArtifactErrorResponse, ArtifactResponse, Checksum, ChecksumType},
		utils::{
			fetch_artifact_checksum, fetch_gradle_module_metadata,
			fetch_latest_artifact_version,
		},
	},
};

const ONECONFIG_GROUP: &str = "org.polyfrost.oneconfig";
const ONECONFIG_LOADER_INCLUDE_ATTRIBUTE: &str = "org.polyfrost.oneconfig.loader.include";
const ONECONFIG_LOADER_JIJ_ATTRIBUTE: &str = "org.polyfrost.oneconfig.loader.jij";

pub fn configure(config: &mut ServiceConfig) {
	config.service(
		web::scope("/artifacts")
			.service(oneconfig)
			.service(platform_agnostic_artifacts),
	);
}

#[derive(Serialize, Deserialize, Debug, Hash, PartialEq, Eq, Clone)]
#[serde(rename_all = "lowercase")]
pub enum ModLoader {
	Forge,
	Fabric,
}

impl EncodeLabelValue for ModLoader {
	fn encode(
		&self,
		encoder: &mut prometheus_client::encoding::LabelValueEncoder,
	) -> Result<(), std::fmt::Error> {
		encoder.write_str(&self.to_string())
	}
}

impl Display for ModLoader {
	fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
		f.write_str(match self {
			Self::Fabric => "fabric",
			Self::Forge => "forge",
		})
	}
}

#[derive(Serialize, Deserialize, Debug, Hash, PartialEq, Eq, Clone, EncodeLabelSet)]
pub struct OneConfigVersionInfo {
	/// The minecraft version to fetch artifacts for
	pub version: String,
	/// The mod loader to fetch artifacts for
	pub loader: ModLoader,
}

#[derive(Serialize, Deserialize, Debug, Hash, PartialEq, Eq, Clone)]
pub struct ArtifactQuery<V = ()> {
	/// Whether or not to use snapshots instead of official releases
	#[serde(default)]
	pub snapshots: bool,
	/// Extra version information
	#[serde(flatten)]
	pub version_info: V,
}

#[get("/oneconfig")]
async fn oneconfig(
	state: web::Data<ApiData>,
	query: web::Query<ArtifactQuery<OneConfigVersionInfo>>,
) -> Result<impl Responder, ArtifactErrorResponse> {
	let mut artifacts = Vec::<ArtifactResponse>::new();
	let repo_suffix = if query.snapshots {
		"snapshots"
	} else {
		"releases"
	};
	let public_repo_url = state.public_maven_url.clone() + repo_suffix;
	let internal_repo_url = state.internal_maven_url.clone() + repo_suffix;

	let oneconfig_artifact_id = format!(
		"{}-{}",
		query.version_info.version, query.version_info.loader
	);

	let latest_oneconfig_version = fetch_latest_artifact_version(
		&state,
		MavenArtifactMetadata::get_metadata_url(
			&internal_repo_url,
			ONECONFIG_GROUP,
			&oneconfig_artifact_id,
		),
	)
	.await;
	let latest_oneconfig_version = if let Err(ArtifactErrorResponse::MavenResponse(e)) =
		&latest_oneconfig_version
		&& let Some(StatusCode::NOT_FOUND) = e.status()
	{
		// Downgrade maven 404s to a 404 on this api, rather than a 500 unexpected error
		return Ok(HttpResponse::NotFound().content_type("text/plain").body(
			"no oneconfig releases found for given loader/version/snapshots options",
		));
	} else {
		latest_oneconfig_version?
	};

	let oneconfig_coordinate = ArtifactCoordinate::new(
		ONECONFIG_GROUP,
		&oneconfig_artifact_id,
		latest_oneconfig_version.to_string(),
	);

	// Add oneconfig itself to the artifacts
	artifacts.push(ArtifactResponse {
		group: ONECONFIG_GROUP.to_string(),
		name: oneconfig_coordinate.artifact_id().to_owned(),
		url: oneconfig_coordinate.to_artifact_url(&public_repo_url),
		checksum: Checksum {
			r#type: ChecksumType::Sha256,
			hash: fetch_artifact_checksum(
				&state,
				&oneconfig_coordinate.to_sha256_url(&internal_repo_url),
			)
			.await?,
		},
		jij: false,
	});

	// Fetch all dependencies of OneConfig with the
	// "org.polyfrost.oneconfig.loader.include" property

	let gradle_metadata =
		fetch_gradle_module_metadata(&state, &internal_repo_url, &oneconfig_coordinate)
			.await?;
	let gradle_metadata = GradleModuleMetadata::parse_from_str(&gradle_metadata)
		.map_err(ArtifactErrorResponse::GradleMetadataParsing)?;

	let mut join_set: JoinSet<Result<ArtifactResponse, ArtifactErrorResponse>> =
		JoinSet::new();

	for variant in gradle_metadata.variants {
		if variant.name != "oneConfigModulesApiElements" {
			continue;
		}

		for dep in variant.dependencies {
			if !matches!(
				dep.attributes.get(ONECONFIG_LOADER_INCLUDE_ATTRIBUTE),
				Some(AttributeValue::Boolean(true))
			) {
				continue;
			}

			// Take the version requirement in order of constraint strength
			let Some(version) = dep
				.version
				.and_then(|v| v.strictly.or(v.requires).or(v.prefers))
			else {
				return Err(ArtifactErrorResponse::NoDependencyVersion {
					group: dep.group.into_owned(),
					artifact: dep.module.into_owned(),
				});
			};
			let coordinate =
				ArtifactCoordinate::new(dep.group.clone(), dep.module.clone(), version);

			// TODO: Clean this up
			let artifact_url = coordinate.to_artifact_url(&public_repo_url);
			let checksum_url = coordinate.to_sha256_url(&internal_repo_url);
			let name = dep.module.into_owned();
			let group = dep.group.into_owned();
			let jij = matches!(
				dep.attributes.get(ONECONFIG_LOADER_JIJ_ATTRIBUTE),
				Some(AttributeValue::Boolean(true))
			);
			let state = state.clone();
			join_set.spawn(async move {
				Ok(ArtifactResponse {
					name,
					group,
					jij,
					url: artifact_url,
					checksum: Checksum {
						r#type: ChecksumType::Sha256,
						hash: fetch_artifact_checksum(&state, &checksum_url).await?,
					},
				})
			});
		}
	}

	// Wait for all deps to be resolved
	while let Some(next) = join_set.join_next().await {
		let task_result = next.map_err(ArtifactErrorResponse::ChecksumTaskFailure)?;

		artifacts.push(task_result?);
	}

	let res = HttpResponse::Ok().content_type("application/json").body(
		serde_json::to_string(&artifacts)
			.map_err(ArtifactErrorResponse::ResponseSerialization)?,
	);

	Ok(res)
}

#[get("/{artifact:stage1|relaunch|loader-assets}")]
async fn platform_agnostic_artifacts(
	state: web::Data<ApiData>,
	query: web::Query<ArtifactQuery>,
	path: web::Path<(String,)>,
) -> Result<impl Responder, ArtifactErrorResponse> {
	let artifact_id = path.into_inner().0;
	let repo_suffix = if query.snapshots {
		"snapshots"
	} else {
		"releases"
	};
	let public_repo_url = state.public_maven_url.clone() + repo_suffix;
	let internal_repo_url = state.internal_maven_url.clone() + repo_suffix;

	let latest_version = fetch_latest_artifact_version(
		&state,
		MavenArtifactMetadata::get_metadata_url(
			&internal_repo_url,
			ONECONFIG_GROUP,
			&artifact_id,
		),
	)
	.await;
	let latest_version = if let Err(ArtifactErrorResponse::MavenResponse(e)) =
		&latest_version
		&& let Some(StatusCode::NOT_FOUND) = e.status()
	{
		// Downgrade maven 404s to a 404 on this api, rather than a 500 unexpected error
		return Ok(HttpResponse::NotFound()
			.content_type("text/plain")
			.body("no artifact releases found for given snapshots options"));
	} else {
		latest_version?
	};

	// Resolve URL and checksum
	let artifact = ArtifactCoordinate::new(
		ONECONFIG_GROUP,
		&artifact_id,
		latest_version.to_string(),
	)
	.with_classifier("all");

	let checksum =
		fetch_artifact_checksum(&state, &artifact.to_sha256_url(&internal_repo_url))
			.await?;

	let response = ArtifactResponse {
		url: artifact.to_artifact_url(public_repo_url),
		name: artifact_id,
		group: ONECONFIG_GROUP.to_string(),
		jij: false,
		checksum: Checksum {
			r#type: ChecksumType::Sha256,
			hash: checksum,
		},
	};
	let response = HttpResponse::Ok().content_type("application/json").body(
		serde_json::to_string(&response)
			.map_err(ArtifactErrorResponse::ResponseSerialization)?,
	);

	Ok(response)
}
