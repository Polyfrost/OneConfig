use actix_web::{HttpResponse, Responder, get, web};
use tokio::try_join;

use crate::api::{
	common::data::ApiData,
	legacy::{
		types::{
			LegacyArtifactsErrorResponse, LegacyArtifactsResponse, LegacyLoader,
			LegacyVersion,
		},
		utils::get_latest_artifact,
	},
};

const ONECONFIG_GROUP: &str = "cc.polyfrost";

#[get("/oneconfig/{version}-{loader}")]
async fn oneconfig(
	state: web::Data<ApiData>,
	path: web::Path<(LegacyVersion, LegacyLoader)>,
) -> Result<impl Responder, LegacyArtifactsErrorResponse> {
	let version = serde_variant::to_variant_name(&path.0)
		.map_err(LegacyArtifactsErrorResponse::EnumVariantSerialization)?;
	let loader = serde_variant::to_variant_name(&path.1)
		.map_err(LegacyArtifactsErrorResponse::EnumVariantSerialization)?;
	let loader_type = path.1.get_loader_type();

	let oneconfig_artifact = format!("oneconfig-{version}-{loader}");
	let loader_artifact = format!("oneconfig-loader-{loader_type}");

	let ((release, release_ts), (snapshot, snapshot_ts), (loader, _)) = try_join!(
		get_latest_artifact(
			&state,
			"releases",
			ONECONFIG_GROUP,
			&oneconfig_artifact,
			Some("full")
		),
		get_latest_artifact(
			&state,
			"snapshots",
			ONECONFIG_GROUP,
			&oneconfig_artifact,
			None
		),
		get_latest_artifact(&state, "releases", ONECONFIG_GROUP, &loader_artifact, None)
	)?;

	// Replace snapshot with release if it is newer
	let snapshot = if release_ts > snapshot_ts {
		release.clone()
	} else {
		snapshot
	};

	let res = HttpResponse::Ok().json(LegacyArtifactsResponse {
		release,
		snapshot,
		loader,
	});

	Ok(res)
}
