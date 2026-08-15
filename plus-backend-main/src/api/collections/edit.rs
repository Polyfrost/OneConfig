use aide::{
	OperationInput, OperationIo,
	axum::{ApiRouter, routing::put_with},
	transform::TransformOperation,
};
use axum::{
	Json,
	extract::{
		FromRequest, Multipart, Path, Request, State, multipart::MultipartRejection,
	},
	http::StatusCode,
	response::IntoResponse,
};
use chrono::{DateTime, FixedOffset};
use schemars::JsonSchema;
use sea_orm::{ActiveModelTrait, EntityTrait, Set};
use serde::Serialize;

use crate::api::{
	ApiState,
	admin_auth::AdminAuthenticationExtractor,
	collections::{StoreAssetError, store_asset},
};

#[derive(thiserror::Error, Debug, OperationIo)]
pub enum EditError {
	#[error("No collection with that id")]
	NotFound,
	#[error("Database error: {0}")]
	Database(#[from] sea_orm::error::DbErr),
	#[error("Unable to store asset: {0}")]
	StoreAsset(#[from] StoreAssetError),
	#[error("Multipart error: {0}")]
	Multipart(#[from] axum::extract::multipart::MultipartError),
	#[error("Multipart rejection: {0}")]
	Rejection(#[from] MultipartRejection),
}

impl IntoResponse for EditError {
	fn into_response(self) -> axum::response::Response {
		(
			match self {
				Self::NotFound => StatusCode::NOT_FOUND,
				Self::Rejection(_) => StatusCode::BAD_REQUEST,
				Self::Database(_) | Self::StoreAsset(_) | Self::Multipart(_) => {
					StatusCode::INTERNAL_SERVER_ERROR
				}
			},
			self.to_string(),
		)
			.into_response()
	}
}

/// The collection after the edit.
#[derive(Debug, Serialize, JsonSchema)]
pub struct EditResponse {
	id: i32,
	name: String,
	description: Option<String>,
	asset_id: Option<i32>,
	created_at: DateTime<FixedOffset>,
}

fn endpoint_doc(op: TransformOperation) -> TransformOperation {
	op.id("editCollection")
		.summary("Edit a collection")
		.description(
			"Updates a collection's name, description, and/or asset. Only the fields \
			 present in the request are changed. When a file is provided it is \
			 uploaded to S3 and set as the collection's asset. Admin role required.",
		)
		.tag("collections")
}

pub(super) fn router() -> ApiRouter<ApiState> {
	ApiRouter::new().api_route("/edit/{id}", put_with(self::endpoint, self::endpoint_doc))
}

struct FileUpload(Multipart);

impl<S> FromRequest<S> for FileUpload
where
	S: Send + Sync,
{
	type Rejection = EditError;

	async fn from_request(req: Request, state: &S) -> Result<Self, Self::Rejection> {
		Ok(Self(
			Multipart::from_request(req, state)
				.await
				.map_err(EditError::Rejection)?,
		))
	}
}

#[derive(JsonSchema)]
#[allow(dead_code)]
struct CollectionEditRequest {
	/// Optional new asset file for the collection.
	#[schemars(with = "Option<String>")]
	file: Option<String>,
	/// Optional new display name.
	name: Option<String>,
	/// Optional new description.
	description: Option<String>,
}

impl OperationInput for FileUpload {
	fn operation_input(
		ctx: &mut aide::generate::GenContext,
		operation: &mut aide::openapi::Operation,
	) {
		operation.request_body = Some(aide::openapi::ReferenceOr::Item(
			aide::openapi::RequestBody {
				description: Some("Multipart collection edit".into()),
				content: [(
					"multipart/form-data".into(),
					aide::openapi::MediaType {
						schema: Some(aide::openapi::SchemaObject {
							json_schema: ctx
								.schema
								.subschema_for::<CollectionEditRequest>(),
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
	Path(id): Path<i32>,
	FileUpload(mut multipart): FileUpload,
) -> Result<Json<EditResponse>, EditError> {
	use entities::{collections, prelude::*};

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

	let existing = Collections::find_by_id(id)
		.one(&state.database)
		.await?
		.ok_or(EditError::NotFound)?;

	let asset_id = match file_data {
		Some(data) => Some(
			store_asset(&state, &data, content_type, &extension, "collections").await?,
		),
		None => None,
	};

	let mut active: collections::ActiveModel = existing.into();
	if let Some(name) = name {
		active.name = Set(name);
	}
	if let Some(description) = description {
		active.description = Set(Some(description));
	}
	if let Some(asset_id) = asset_id {
		active.asset_id = Set(Some(asset_id));
	}

	let collection = active.update(&state.database).await?;

	Ok(Json(EditResponse {
		id: collection.id,
		name: collection.name,
		description: collection.description,
		asset_id: collection.asset_id,
		created_at: collection.created_at,
	}))
}
