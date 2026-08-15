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

use entities::sea_orm_active_enums::AssetKind;
use schemars::JsonSchema;
use sea_orm::{ActiveModelTrait, EntityTrait, Set};
use sha2::{Digest, Sha256};
use uuid::Uuid;

use crate::api::{
	ApiState, admin_auth::AdminAuthenticationExtractor, bundles::BundleInfo,
	stripe::products,
};

#[derive(thiserror::Error, Debug, OperationIo)]
pub enum CreateError {
	#[error("A bundle name is required")]
	MissingName,
	#[error("A base price is required to create a new Stripe product")]
	MissingPrice,
	#[error("Database error: {0}")]
	Database(#[from] sea_orm::error::DbErr),
	#[error("S3 error: {0}")]
	S3(#[from] s3::error::S3Error),
	#[error("Stripe error: {0}")]
	Stripe(#[from] stripe_client::StripeError),
	#[error("Multipart error: {0}")]
	Multipart(#[from] axum::extract::multipart::MultipartError),
	#[error("Multipart rejection: {0}")]
	Rejection(#[from] MultipartRejection),
}

impl IntoResponse for CreateError {
	fn into_response(self) -> axum::response::Response {
		(
			match self {
				Self::MissingName | Self::MissingPrice | Self::Rejection(_) => {
					StatusCode::BAD_REQUEST
				}
				Self::Stripe(_) => StatusCode::BAD_GATEWAY,
				Self::Database(_) | Self::S3(_) | Self::Multipart(_) => {
					StatusCode::INTERNAL_SERVER_ERROR
				}
			},
			self.to_string(),
		)
			.into_response()
	}
}

fn endpoint_doc(op: TransformOperation) -> TransformOperation {
	op.id("createBundle")
		.summary("Create a new bundle")
		.description(
			"Uploads a bundle's cover image to S3 (optional), registers the bundle \
			 in the database with its contained cosmetics, then provisions a Stripe \
			 product and price for it. Admin password required.",
		)
		.tag("bundles")
		.response_with::<{ StatusCode::OK.as_u16() }, Json<BundleInfo>, _>(|res| {
			res.description("The created bundle info")
		})
		.response_with::<{ StatusCode::UNAUTHORIZED.as_u16() }, String, _>(|res| {
			res.description("Invalid or missing admin password")
		})
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
struct BundleUploadRequest {
	/// Optional cover image for the bundle.
	#[schemars(with = "Option<String>")]
	file: Option<String>,
	/// The bundle's display name.
	name: String,
	/// Optional long-form description for the catalog and Stripe product.
	description: Option<String>,
	/// Optional id of the collection this bundle belongs to.
	collection: Option<i32>,
	/// The price in USD major units (e.g. `9.99`). Required to create the Stripe
	/// product and price.
	base_price: Option<f32>,
	/// The ids of the cosmetics (and emotes) this bundle contains (repeat the
	/// field for multiple).
	cosmetic_id: Vec<i32>,
}

impl OperationInput for FileUpload {
	fn operation_input(
		ctx: &mut aide::generate::GenContext,
		operation: &mut aide::openapi::Operation,
	) {
		operation.request_body = Some(aide::openapi::ReferenceOr::Item(
			aide::openapi::RequestBody {
				description: Some("Multipart bundle upload".into()),
				content: [(
					"multipart/form-data".into(),
					aide::openapi::MediaType {
						schema: Some(aide::openapi::SchemaObject {
							json_schema: ctx
								.schema
								.subschema_for::<BundleUploadRequest>(),
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

fn sha256_hex(data: &[u8]) -> String {
	Sha256::digest(data)
		.iter()
		.map(|byte| format!("{byte:02x}"))
		.collect()
}

async fn endpoint(
	State(state): State<ApiState>,
	_auth: AdminAuthenticationExtractor,
	FileUpload(mut multipart): FileUpload,
) -> Result<Json<BundleInfo>, CreateError> {
	let mut file_data = None;
	let mut content_type = None;
	let mut extension = "png".to_string();
	let mut name = None;
	let mut description = None;
	let mut collection = None;
	let mut base_price = None;
	let mut cosmetic_ids = Vec::new();

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
			Some("collection") => {
				let value = field.text().await?;
				if let Ok(parsed) = value.trim().parse::<i32>() {
					collection = Some(parsed);
				}
			}
			Some("base_price") => {
				let value = field.text().await?;
				if let Ok(parsed) = value.trim().parse::<f32>() {
					base_price = Some(parsed);
				}
			}
			Some("cosmetic_id") => {
				let value = field.text().await?;
				if let Ok(parsed) = value.trim().parse::<i32>() {
					cosmetic_ids.push(parsed);
				}
			}
			_ => {}
		}
	}

	let name = name.ok_or(CreateError::MissingName)?;

	// Upload the cover image to S3 when one was provided.
	let asset_id = match file_data {
		Some(data) => {
			let path = format!("bundles/{}.{}", Uuid::now_v7(), extension);
			state
				.s3_bucket
				.put_object_with_content_type(
					&path,
					&data,
					content_type.as_deref().unwrap_or("image/png"),
				)
				.await?;

			use entities::asset;
			let asset = asset::ActiveModel {
				storage_path: Set(Some(path)),
				url: Set(None),
				asset_kind: Set(AssetKind::Image),
				content_type: Set(content_type.or_else(|| Some("image/png".to_string()))),
				hash: Set(Some(sha256_hex(&data))),
				..Default::default()
			}
			.insert(&state.database)
			.await?;
			Some(asset.id)
		}
		None => None,
	};

	// Provision the Stripe product and its default price.
	let base_price = base_price.ok_or(CreateError::MissingPrice)?;
	let product_id =
		products::create_product(&state.stripe.client, &name, description.as_deref())
			.await?;
	let price_id = products::create_price(
		&state.stripe.client,
		&product_id,
		products::to_cents(base_price),
	)
	.await?;
	products::set_default_price(&state.stripe.client, &product_id, &price_id).await?;

	use entities::{bundles, bundles_cosmetics, prelude::*};

	let bundle = bundles::ActiveModel {
		name: Set(name),
		description: Set(description),
		asset_id: Set(asset_id),
		enabled: Set(true),
		collection: Set(collection),
		stripe_product_id: Set(Some(product_id)),
		stripe_price_id: Set(Some(price_id)),
		base_price: Set(Some(base_price)),
		discount_rate: Set(None),
		..Default::default()
	}
	.insert(&state.database)
	.await?;

	if !cosmetic_ids.is_empty() {
		BundlesCosmetics::insert_many(cosmetic_ids.iter().map(|cosmetic_id| {
			bundles_cosmetics::ActiveModel {
				bundle_id: Set(bundle.id),
				cosmetic_id: Set(*cosmetic_id),
			}
		}))
		.on_conflict_do_nothing()
		.exec(&state.database)
		.await?;
	}

	Ok(Json(bundle.into()))
}
