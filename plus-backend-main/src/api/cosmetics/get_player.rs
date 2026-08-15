use aide::{
	OperationIo,
	axum::{ApiRouter, routing::get_with},
	transform::TransformOperation,
};
use axum::{
	Json,
	extract::{Query, State},
	http::StatusCode,
	response::IntoResponse,
};
use schemars::JsonSchema;
use sea_orm::{ColumnTrait as _, EntityTrait, ModelTrait as _, QueryFilter};
use serde::{Deserialize, Serialize};
use tokio::task::JoinSet;
use uuid::Uuid;

use crate::api::{
	ApiState,
	account::OptionalAuthenticationExtractor,
	cosmetics::{
		CosmeticInfo, EmoteInfo, EquippedCosmetics, group_cosmetics, load_groups,
	},
};

#[derive(thiserror::Error, Debug, OperationIo)]
pub enum ResponseError {
	#[error("Authentication was not given, so a player query parameter is required")]
	PlayerRequired,
	#[error("Unable to fetch user data from database: {0}")]
	DatabaseFetch(#[from] sea_orm::error::DbErr),
	#[error("Unable to presign S3 URLs: {0}")]
	S3Presign(#[from] s3::error::S3Error),
}

fn endpoint_doc(op: TransformOperation) -> TransformOperation {
	op.id("getPlayerCosmetics")
		.summary("Get a player's cosmetic status")
		.description(
			"Lists all cosmetics owned by a player, along with all active cosmetics",
		)
		.tag("cosmetics")
		.response_with::<{ StatusCode::BAD_REQUEST.as_u16() }, String, _>(|res| {
			res.description(
				"Authentication was not given, so a player query parameter is required",
			)
		})
		.response_with::<{ StatusCode::INTERNAL_SERVER_ERROR.as_u16() }, String, _>(
			|res| {
				res.description(
					"An internal server error occurred while trying to fetch cosmetics",
				)
			},
		)
}

impl IntoResponse for ResponseError {
	fn into_response(self) -> axum::response::Response {
		(
			match self {
				ResponseError::PlayerRequired => StatusCode::BAD_REQUEST,
				ResponseError::S3Presign(_) => StatusCode::INTERNAL_SERVER_ERROR,
				ResponseError::DatabaseFetch(_) => StatusCode::INTERNAL_SERVER_ERROR,
			},
			self.to_string(),
		)
			.into_response()
	}
}

#[derive(Debug, Deserialize, JsonSchema)]
struct QueryParams {
	/// The UUID of the player to look up the cosmetics of. This is only
	/// optional if authentication is passed instead.
	#[serde(default)]
	#[schemars(example = &"f7c77d99-9f15-4a66-a87d-c4a51ef30d19")]
	player: Option<Uuid>,
}

/// Information about the player's cosmetics
#[derive(Debug, Default, Serialize, JsonSchema)]
pub struct Response {
	cosmetics: Vec<CosmeticInfo>,
	emotes: Vec<EmoteInfo>,
	equipped: EquippedCosmetics,
	particle_color: Option<i32>,
}

pub(super) fn router() -> ApiRouter<ApiState> {
	ApiRouter::new().api_route("/player", get_with(self::endpoint, self::endpoint_doc))
}

#[tracing::instrument(level = "debug", skip(state))]
async fn endpoint(
	State(state): State<ApiState>,
	OptionalAuthenticationExtractor(player): OptionalAuthenticationExtractor,
	Query(query): Query<QueryParams>,
) -> Result<Json<Response>, ResponseError> {
	let mut response = Response::default();
	let Some(player) = query.player.or(player) else {
		return Err(ResponseError::PlayerRequired);
	};

	{
		use entities::{
			player_equipped_cosmetic, player_owned_cosmetic, prelude::*,
			sea_orm_active_enums::CosmeticType, user,
		};

		let Some(player) = User::find()
			.filter(user::Column::MinecraftUuid.eq(player))
			.one(&state.database)
			.await?
		else {
			return Ok(Json(response));
		};

		response.particle_color = player.particle_color;

		let owned = PlayerOwnedCosmetic::find()
			.filter(player_owned_cosmetic::Column::PlayerId.eq(player.id))
			.find_also_related(Cosmetic)
			.all(&state.database)
			.await?;

		let mut rows = Vec::new();
		let mut emote_tasks = JoinSet::new();
		for cosmetic in owned
			.into_iter()
			.filter_map(|(_, c)| c)
			.filter(|c| c.enabled)
		{
			let asset = match cosmetic.asset_id {
				Some(asset_id) => {
					Asset::find_by_id(asset_id).one(&state.database).await?
				}
				None => None,
			};

			if matches!(cosmetic.r#type, CosmeticType::Emote) {
				let asset_cache = state.asset_cache.clone();
				let s3_bucket = state.s3_bucket.clone();
				emote_tasks.spawn(async move {
					EmoteInfo::from_db_model(
						&cosmetic,
						asset.as_ref(),
						asset_cache,
						s3_bucket,
					)
					.await
				});
				continue;
			}

			let cover_asset = match cosmetic.cover_asset_id {
				Some(asset_id) => {
					Asset::find_by_id(asset_id).one(&state.database).await?
				}
				None => None,
			};

			let allowed_slots = cosmetic
				.find_related(CosmeticAllowedSlot)
				.all(&state.database)
				.await?
				.into_iter()
				.map(|s| s.slot)
				.collect();
			rows.push((cosmetic, asset, cover_asset, allowed_slots));
		}

		let groups = load_groups(&state.database).await?;
		response.cosmetics = group_cosmetics(
			rows,
			groups,
			state.asset_cache.clone(),
			state.s3_bucket.clone(),
			false,
		)
		.await?;

		response.emotes.extend(
			emote_tasks
				.join_all()
				.await
				.into_iter()
				.collect::<Result<Vec<_>, _>>()?,
		);

		response.equipped.extend(
			PlayerEquippedCosmetic::find()
				.filter(player_equipped_cosmetic::Column::PlayerId.eq(player.id))
				.find_also_related(Cosmetic)
				.all(&state.database)
				.await?
				.into_iter()
				.filter_map(|(equipment, cosmetic)| {
					cosmetic
						.filter(|c| c.enabled)
						.map(|_| (equipment.slot, equipment.cosmetic_id))
				}),
		);
	};

	Ok(Json(response))
}
