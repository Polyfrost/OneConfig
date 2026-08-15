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
use std::collections::HashMap;

use entities::sea_orm_active_enums::{AssetKind, BodySlot, CosmeticType};
use schemars::JsonSchema;
use sea_orm::{ActiveModelTrait, ColumnTrait, EntityTrait, QueryFilter, Set};
use sha2::{Digest, Sha256};
use uuid::Uuid;

use crate::api::{
	ApiState,
	admin_auth::AdminAuthenticationExtractor,
	cosmetics::{CosmeticInfo, group_cosmetics},
	stripe::products,
};

#[derive(thiserror::Error, Debug, OperationIo)]
pub enum UploadError {
	#[error("Missing file in multipart data")]
	MissingFile,
	#[error("Missing or invalid cosmetic type")]
	InvalidType,
	#[error("At least one body slot must be provided")]
	MissingSlots,
	#[error("Invalid body slot")]
	InvalidSlot,
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
	#[error("Invalid ZIP bundle: {0}")]
	Zip(#[from] zip::result::ZipError),
}

impl IntoResponse for UploadError {
	fn into_response(self) -> axum::response::Response {
		(
			match self {
				Self::MissingFile
				| Self::InvalidType
				| Self::MissingSlots
				| Self::InvalidSlot
				| Self::MissingPrice
				| Self::Zip(_)
				| Self::Rejection(_) => StatusCode::BAD_REQUEST,
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

fn parse_cosmetic_type(value: &str) -> Option<CosmeticType> {
	match value {
		"cape" => Some(CosmeticType::Cape),
		"backpack" => Some(CosmeticType::Backpack),
		"glasses" => Some(CosmeticType::Glasses),
		"wings" => Some(CosmeticType::Wings),
		"glove" => Some(CosmeticType::Glove),
		"hat" => Some(CosmeticType::Hat),
		"aura" => Some(CosmeticType::Aura),
		"boots" => Some(CosmeticType::Boots),
		"shoulder" => Some(CosmeticType::Shoulder),
		"pet" => Some(CosmeticType::Pet),
		"emote" => Some(CosmeticType::Emote),
		_ => None,
	}
}

fn parse_body_slot(value: &str) -> Option<BodySlot> {
	match value {
		"cape" => Some(BodySlot::Cape),
		"backpack" => Some(BodySlot::Backpack),
		"glasses" => Some(BodySlot::Glasses),
		"wings" => Some(BodySlot::Wings),
		"left_hand" => Some(BodySlot::LeftHand),
		"right_hand" => Some(BodySlot::RightHand),
		"hat" => Some(BodySlot::Hat),
		"aura" => Some(BodySlot::Aura),
		"boots" => Some(BodySlot::Boots),
		"shoulder" => Some(BodySlot::Shoulder),
		"pet" => Some(BodySlot::Pet),
		_ => None,
	}
}

fn default_name(cosmetic_type: &CosmeticType) -> &'static str {
	match cosmetic_type {
		CosmeticType::Cape => "Cape",
		CosmeticType::Backpack => "Backpack",
		CosmeticType::Glasses => "Glasses",
		CosmeticType::Wings => "Wings",
		CosmeticType::Glove => "Glove",
		CosmeticType::Hat => "Hat",
		CosmeticType::Aura => "Aura",
		CosmeticType::Boots => "Boots",
		CosmeticType::Shoulder => "Shoulder",
		CosmeticType::Pet => "Pet",
		CosmeticType::Emote => "Emote",
	}
}

fn storage_prefix(cosmetic_type: &CosmeticType) -> &'static str {
	match cosmetic_type {
		CosmeticType::Cape => "capes",
		CosmeticType::Backpack => "backpacks",
		CosmeticType::Glasses => "glasses",
		CosmeticType::Wings => "wings",
		CosmeticType::Glove => "gloves",
		CosmeticType::Hat => "hats",
		CosmeticType::Aura => "auras",
		CosmeticType::Boots => "boots",
		CosmeticType::Shoulder => "shoulders",
		CosmeticType::Pet => "pets",
		CosmeticType::Emote => "emotes",
	}
}

fn endpoint_doc(op: TransformOperation) -> TransformOperation {
	op.id("createCosmetic")
		.summary("Create a new cosmetic")
		.description(
			"Uploads a new cosmetic to S3 and registers it in the database with its \
			 allowed body slots, then provisions a Stripe product and price for it. \
			 A cosmetic joining an existing group reuses that group's Stripe ids. \
			 Emotes (type `emote`) are stored as bundles and take no body slots.",
		)
		.tag("cosmetics")
		.response_with::<{ StatusCode::OK.as_u16() }, Json<CosmeticInfo>, _>(|res| {
			res.description("The created cosmetic info")
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
	type Rejection = UploadError;

	async fn from_request(req: Request, state: &S) -> Result<Self, Self::Rejection> {
		Ok(Self(
			Multipart::from_request(req, state)
				.await
				.map_err(UploadError::Rejection)?,
		))
	}
}

#[derive(JsonSchema)]
#[allow(dead_code)]
struct CosmeticUploadRequest {
	#[schemars(with = "String")]
	file: String,
	/// The cosmetic type: one of `cape`, `backpack`, `glasses`, `wings`, `glove`,
	/// `hat`, `aura`, `boots`, `shoulder`, `emote`.
	r#type: String,
	/// Optional display name; defaults to the type name.
	name: Option<String>,
	/// Optional long-form description for the catalog and Stripe product.
	description: Option<String>,
	/// Optional id of the collection this cosmetic belongs to.
	collection: Option<i32>,
	/// The price in USD major units (e.g. `4.99`). Required when a new Stripe
	/// product must be created; ignored when reusing an existing group's price.
	base_price: Option<f32>,
	/// One or more allowed body slots (repeat the field for multiple).
	/// not required for emotes
	slots: Vec<String>,
	/// Optional group name. When set, this cosmetic becomes a variant of the
	/// (find-or-created) group of the same name and type, so the player buys
	/// the group once and chooses between its variants.
	group: Option<String>,
	/// Optional user-facing variant label within the group (e.g. "Blue").
	variant_name: Option<String>,
	/// Optional skin model this variant targets ("slim"/"wide"); only for
	/// variants the client must pick by skin.
	model_variant: Option<String>,
	/// Optional ordering of this variant within its group (default 0).
	variant_order: Option<i32>,
}

impl OperationInput for FileUpload {
	fn operation_input(
		ctx: &mut aide::generate::GenContext,
		operation: &mut aide::openapi::Operation,
	) {
		operation.request_body = Some(aide::openapi::ReferenceOr::Item(
			aide::openapi::RequestBody {
				description: Some("Multipart cosmetic upload".into()),
				content: [(
					"multipart/form-data".into(),
					aide::openapi::MediaType {
						schema: Some(aide::openapi::SchemaObject {
							json_schema: ctx
								.schema
								.subschema_for::<CosmeticUploadRequest>(),
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
) -> Result<Json<CosmeticInfo>, UploadError> {
	let mut file_data = None;
	let mut content_type = None;
	let mut extension = "png".to_string();
	let mut cosmetic_type = None;
	let mut name = None;
	let mut description = None;
	let mut collection = None;
	let mut base_price = None;
	let mut slots = Vec::new();
	let mut group_name = None;
	let mut variant_name = None;
	let mut model_variant = None;
	let mut variant_order = 0;

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
			Some("type") => {
				let value = field.text().await?;
				cosmetic_type = Some(
					parse_cosmetic_type(value.trim()).ok_or(UploadError::InvalidType)?,
				);
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
			Some("slots") => {
				let value = field.text().await?;
				slots
					.push(parse_body_slot(value.trim()).ok_or(UploadError::InvalidSlot)?);
			}
			Some("group") => {
				let value = field.text().await?;
				let trimmed = value.trim();
				if !trimmed.is_empty() {
					group_name = Some(trimmed.to_string());
				}
			}
			Some("variant_name") => {
				let value = field.text().await?;
				let trimmed = value.trim();
				if !trimmed.is_empty() {
					variant_name = Some(trimmed.to_string());
				}
			}
			Some("model_variant") => {
				let value = field.text().await?;
				let trimmed = value.trim();
				if !trimmed.is_empty() {
					model_variant = Some(trimmed.to_string());
				}
			}
			Some("variant_order") => {
				let value = field.text().await?;
				if let Ok(parsed) = value.trim().parse::<i32>() {
					variant_order = parsed;
				}
			}
			_ => {}
		}
	}

	let Some(data) = file_data else {
		return Err(UploadError::MissingFile);
	};
	let cosmetic_type = cosmetic_type.ok_or(UploadError::InvalidType)?;

	let is_emote = matches!(cosmetic_type, CosmeticType::Emote);
	if !is_emote && slots.is_empty() {
		return Err(UploadError::MissingSlots);
	}

	let is_bundle = crate::api::cosmetics::is_zip(&data);
	let data: Vec<u8> = if is_bundle {
		crate::api::cosmetics::strip_macos_junk(&data)?
	} else {
		data.to_vec()
	};
	if is_bundle {
		extension = "zip".to_string();
		content_type = Some("application/zip".to_string());
	}
	let asset_kind = if is_emote || is_bundle {
		AssetKind::Bundle
	} else {
		AssetKind::Image
	};
	let default_content_type = if is_emote {
		"application/zip"
	} else {
		"image/png"
	};

	let path = format!(
		"{}/{}.{}",
		storage_prefix(&cosmetic_type),
		Uuid::now_v7(),
		extension
	);

	state
		.s3_bucket
		.put_object_with_content_type(
			&path,
			&data,
			content_type.as_deref().unwrap_or(default_content_type),
		)
		.await?;

	use entities::{
		asset, cosmetic, cosmetic_allowed_slot, cosmetic_group,
		cosmetic_group_allowed_slot, prelude::*,
	};

	let asset = asset::ActiveModel {
		storage_path: Set(Some(path)),
		url: Set(None),
		asset_kind: Set(asset_kind),
		content_type: Set(content_type.or_else(|| Some(default_content_type.to_string()))),
		hash: Set(Some(sha256_hex(&data))),
		..Default::default()
	}
	.insert(&state.database)
	.await?;

	let cover_asset_id = if state.render_service_url.is_empty() {
		None
	} else {
		match crate::api::cosmetics::cover::render_cover(
			&state.render_client,
			&state.render_service_url,
			&cosmetic_type,
			&slots,
			model_variant.as_deref(),
			is_bundle,
			&data,
		)
		.await
		{
			Ok(png) => match crate::api::collections::store_asset(
				&state,
				&png,
				Some("image/png".to_string()),
				"png",
				"covers",
			)
			.await
			{
				Ok(id) => Some(id),
				Err(error) => {
					tracing::warn!("Unable to store cosmetic cover asset: {error}");
					None
				}
			},
			Err(error) => {
				tracing::warn!("Unable to render cosmetic cover: {error}");
				None
			}
		}
	};

	let group = match group_name {
		Some(group_name) => {
			let existing = CosmeticGroup::find()
				.filter(cosmetic_group::Column::Name.eq(group_name.clone()))
				.filter(cosmetic_group::Column::Type.eq(cosmetic_type.clone()))
				.one(&state.database)
				.await?;
			let group = match existing {
				Some(group) => group,
				None => {
					cosmetic_group::ActiveModel {
						name: Set(group_name),
						r#type: Set(cosmetic_type.clone()),
						enabled: Set(true),
						..Default::default()
					}
					.insert(&state.database)
					.await?
				}
			};
			if !slots.is_empty() {
				cosmetic_group_allowed_slot::Entity::insert_many(slots.iter().map(
					|slot| cosmetic_group_allowed_slot::ActiveModel {
						group_id: Set(group.id),
						slot: Set(slot.clone()),
					},
				))
				.on_conflict_do_nothing()
				.exec(&state.database)
				.await?;
			}
			Some(group)
		}
		None => None,
	};

	let resolved_name = name.unwrap_or_else(|| default_name(&cosmetic_type).to_string());

	// Resolve the Stripe product/price: variants share one product and price, so
	// reuse an existing group sibling's ids when present, otherwise create them.
	let sibling = match &group {
		Some(group) => {
			Cosmetic::find()
				.filter(cosmetic::Column::GroupId.eq(group.id))
				.filter(cosmetic::Column::StripeProductId.is_not_null())
				.filter(cosmetic::Column::StripePriceId.is_not_null())
				.one(&state.database)
				.await?
		}
		None => None,
	};

	let (stripe_product_id, stripe_price_id, price_value, discount_rate) = match sibling {
		Some(sibling) => (
			sibling.stripe_product_id,
			sibling.stripe_price_id,
			sibling.base_price,
			sibling.discount_rate,
		),
		None => {
			let base_price = base_price.ok_or(UploadError::MissingPrice)?;
			let product_name = group
				.as_ref()
				.map(|g| g.name.as_str())
				.unwrap_or(resolved_name.as_str());
			let product_id = products::create_product(
				&state.stripe.client,
				product_name,
				description.as_deref(),
			)
			.await?;
			let price_id = products::create_price(
				&state.stripe.client,
				&product_id,
				products::to_cents(base_price),
			)
			.await?;
			products::set_default_price(&state.stripe.client, &product_id, &price_id)
				.await?;
			(Some(product_id), Some(price_id), Some(base_price), None)
		}
	};

	let model = cosmetic::ActiveModel {
		asset_id: Set(Some(asset.id)),
		cover_asset_id: Set(cover_asset_id),
		name: Set(Some(resolved_name)),
		r#type: Set(cosmetic_type),
		enabled: Set(true),
		group_id: Set(group.as_ref().map(|g| g.id)),
		variant_name: Set(variant_name),
		model_variant: Set(model_variant),
		variant_order: Set(variant_order),
		stripe_product_id: Set(stripe_product_id),
		stripe_price_id: Set(stripe_price_id),
		base_price: Set(price_value),
		discount_rate: Set(discount_rate),
		collection: Set(collection),
		description: Set(description),
		..Default::default()
	}
	.insert(&state.database)
	.await?;

	if !slots.is_empty() {
		cosmetic_allowed_slot::Entity::insert_many(slots.iter().map(|slot| {
			cosmetic_allowed_slot::ActiveModel {
				cosmetic_id: Set(model.id),
				slot: Set(slot.clone()),
			}
		}))
		.exec(&state.database)
		.await?;
	}

	let info = crate::api::cosmetics::CachedAssetInfo::from_db_model(
		&asset,
		state.s3_bucket.clone(),
	)
	.await?;
	state.asset_cache.insert(asset.id, info).await;

	let groups = match &group {
		Some(group) => {
			let group_slots = CosmeticGroupAllowedSlot::find()
				.filter(cosmetic_group_allowed_slot::Column::GroupId.eq(group.id))
				.all(&state.database)
				.await?
				.into_iter()
				.map(|s| s.slot)
				.collect();
			HashMap::from([(group.id, (group.clone(), group_slots))])
		}
		None => HashMap::new(),
	};

	let cover_asset = match cover_asset_id {
		Some(cover_asset_id) => {
			Asset::find_by_id(cover_asset_id)
				.one(&state.database)
				.await?
		}
		None => None,
	};

	let mut infos = group_cosmetics(
		vec![(model, Some(asset), cover_asset, slots)],
		groups,
		state.asset_cache.clone(),
		state.s3_bucket.clone(),
		false,
	)
	.await?;

	Ok(Json(infos.remove(0)))
}
