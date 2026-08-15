use aide::{
	OperationInput, OperationIo,
	axum::{ApiRouter, routing::post_with},
	transform::TransformOperation,
};
use axum::{
	Json,
	extract::{FromRequest, Multipart, Request, State, multipart::MultipartRejection},
	http::StatusCode,
	response::IntoResponse,
};
use chrono::{DateTime, FixedOffset};
use schemars::JsonSchema;
use sea_orm::{ActiveModelTrait, Set};
use serde::Serialize;

use crate::api::{
	ApiState,
	admin_auth::AdminAuthenticationExtractor,
	collections::{StoreAssetError, store_asset},
};

#[derive(thiserror::Error, Debug, OperationIo)]
pub enum CreateError {
	#[error("Missing collection name")]
	MissingName,
	#[error("Database error: {0}")]
	Database(#[from] sea_orm::error::DbErr),
	#[error("Unable to store asset: {0}")]
	StoreAsset(#[from] StoreAssetError),
	#[error("Multipart error: {0}")]
	Multipart(#[from] axum::extract::multipart::MultipartError),
	#[error("Multipart rejection: {0}")]
	Rejection(#[from] MultipartRejection),
}

impl IntoResponse for CreateError {
	fn into_response(self) -> axum::response::Response {
		(
			match self {
				Self::MissingName | Self::Rejection(_) => StatusCode::BAD_REQUEST,
				Self::Database(_) | Self::StoreAsset(_) | Self::Multipart(_) => {
					StatusCode::INTERNAL_SERVER_ERROR
				}
			},
			self.to_string(),
		)
			.into_response()
	}
}

/// The collection created by the request.
#[derive(Debug, Serialize, JsonSchema)]
pub struct CreateResponse {
	id: i32,
	name: String,
	description: Option<String>,
	asset_id: Option<i32>,
	created_at: DateTime<FixedOffset>,
}

fn endpoint_doc(op: TransformOperation) -> TransformOperation {
	op.id("createCollection")
		.summary("Create a collection")
		.description(
			"Creates a new collection. When a file is provided it is uploaded to S3 \
			 and referenced as the collection's asset. Admin role required.",
		)
		.tag("collections")
}

pub(super) fn router() -> ApiRouter<ApiState> {
	ApiRouter::new().api_route("/create", post_with(self::endpoint, self::endpoint_doc))
}

struct FileUpload(Multipart);

impl<S> FromRequest<S> for FileUpload
where
	S: Send + Sync,
{
	type Rejection = CreateError;

	async fn from_request(req: Request, state: &S) -> Result<Self, Self::Rejection> {
		Ok(Self(
			Multipart::from_request(req, state)
				.await
				.map_err(CreateError::Rejection)?,
		))
	}
}

#[derive(JsonSchema)]
#[allow(dead_code)]
struct CollectionCreateRequest {
	/// Optional asset file for the collection.
	#[schemars(with = "Option<String>")]
	file: Option<String>,
	/// The collection's display name.
	name: String,
	/// Optional description of the collection.
	description: Option<String>,
}

impl OperationInput for FileUpload {
	fn operation_input(
		ctx: &mut aide::generate::GenContext,
		operation: &mut aide::openapi::Operation,
	) {
		operation.request_body = Some(aide::openapi::ReferenceOr::Item(
			aide::openapi::RequestBody {
				description: Some("Multipart collection upload".into()),
				content: [(
					"multipart/form-data".into(),
					aide::openapi::MediaType {
						schema: Some(aide::openapi::SchemaObject {
							json_schema: ctx
								.schema
								.subschema_for::<CollectionCreateRequest>(),
							example: None,
							external_docs: None,
						}),
						..Default::default()
					},
				)]
				.into_iter()
				.collect(),
				required: true,
				extensions: Default::default(),
			},
		));
	}
}

async fn endpoint(
	State(state): State<ApiState>,
	_auth: AdminAuthenticationExtractor,
	FileUpload(mut multipart): FileUpload,
) -> Result<(StatusCode, Json<CreateResponse>), CreateError> {
	use entities::collections;

	let mut file_data = None;
	let mut content_type = None;
	let mut extension = "png".to_string();
	let mut name = None;
	let mut description = None;

	while let Some(field) = multipart.next_field().await? {
		match field.name() {
			Some("file") => {
				if let Some(file_name) = field.file_name()
					&& let Some(ext) = std::path::Path::new(file_name).extension()
				{
					extension = ext.to_string_lossy().to_string();
				}
				content_type = field.content_type().map(|s| s.to_string());
				file_data = Some(field.bytes().await?);
			}
			Some("name") => {
				let value = field.text().await?;
				let trimmed = value.trim();
				if !trimmed.is_empty() {
					name = Some(trimmed.to_string());
				}
			}
			Some("description") => {
				let value = field.text().await?;
				let trimmed = value.trim();
				if !trimmed.is_empty() {
					description = Some(trimmed.to_string());
				}
			}
			_ => {}
		}
	}

	let name = name.ok_or(CreateError::MissingName)?;

	let asset_id = match file_data {
		Some(data) => Some(
			store_asset(&state, &data, content_type, &extension, "collections").await?,
		),
		None => None,
	};

	let collection = collections::ActiveModel {
		name: Set(name),
		description: Set(description),
		asset_id: Set(asset_id),
		..Default::default()
	}
	.insert(&state.database)
	.await?;

	Ok((
		StatusCode::CREATED,
		Json(CreateResponse {
			id: collection.id,
			name: collection.name,
			description: collection.description,
			asset_id: collection.asset_id,
			created_at: collection.created_at,
		}),
	))
}
