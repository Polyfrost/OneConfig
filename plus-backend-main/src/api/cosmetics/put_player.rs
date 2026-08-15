use aide::{
	OperationIo,
	axum::{ApiRouter, routing::put_with},
	transform::TransformOperation,
};
use axum::{
	Json,
	extract::State,
	http::StatusCode,
	response::{IntoResponse, NoContent},
};
use schemars::JsonSchema;
use sea_orm::{
	ActiveValue, ColumnTrait, EntityTrait, QueryFilter, Set, TransactionTrait,
};
use serde::Deserialize;

use crate::api::{
	ApiState, account::AuthenticatedPlayer, cosmetics::PartialEquippedCosmetics,
};

#[derive(thiserror::Error, Debug, OperationIo)]
pub enum ResponseError {
	#[error("The given ID {id} is not owned by the player")]
	UnownedCosmetic { id: i32 },
	#[error("The given ID {id} cannot be equipped in slot {slot}")]
	InvalidSlot { slot: String, id: i32 },
	#[error("Unable to query database: {0}")]
	Database(#[from] sea_orm::error::DbErr),
}

fn endpoint_doc(op: TransformOperation) -> TransformOperation {
	op.id("putCosmetics")
		.summary("Set a player's active cosmetics")
		.description("Sets the authorized player's active cosmetics")
		.tag("cosmetics")
		.response_with::<{ StatusCode::BAD_REQUEST.as_u16() }, String, _>(|res| {
			res.description(
				"An error given when a passed ID is not owned or cannot be equipped in a slot",
			)
			.example("The given ID 2 cannot be equipped in slot cape")
		})
		.response_with::<{ StatusCode::INTERNAL_SERVER_ERROR.as_u16() }, String, _>(
			|res| {
				res.description(
					"An internal server error occurred while trying to set active \
					 cosmetics",
				)
			},
		)
}

impl IntoResponse for ResponseError {
	fn into_response(self) -> axum::response::Response {
		(
			match self {
				ResponseError::UnownedCosmetic { .. }
				| ResponseError::InvalidSlot { .. } => StatusCode::BAD_REQUEST,
				ResponseError::Database(_) => StatusCode::INTERNAL_SERVER_ERROR,
			},
			self.to_string(),
		)
			.into_response()
	}
}

#[derive(Debug, Deserialize, JsonSchema)]
struct RequestBody {
	/// Slot keyed equipment updates.
	#[serde(flatten)]
	equipment: PartialEquippedCosmetics,
}

pub(super) fn router() -> ApiRouter<ApiState> {
	ApiRouter::new().api_route("/player", put_with(self::endpoint, self::endpoint_doc))
}

#[tracing::instrument(level = "debug", skip(state))]
async fn endpoint(
	State(state): State<ApiState>,
	AuthenticatedPlayer(player): AuthenticatedPlayer,
	Json(body): Json<RequestBody>,
) -> Result<NoContent, ResponseError> {
	{
		use entities::{
			cosmetic_allowed_slot, player_equipped_cosmetic, player_owned_cosmetic,
			prelude::*,
		};
		use sea_orm::sea_query::OnConflict;

		let txn = state.database.begin().await?;

		for (slot, value) in body.equipment.equipped {
			if let Some(id) = value {
				let owned = PlayerOwnedCosmetic::find()
					.filter(player_owned_cosmetic::Column::PlayerId.eq(player.id))
					.filter(player_owned_cosmetic::Column::CosmeticId.eq(id))
					.one(&txn)
					.await?
					.is_some();
				if !owned {
					return Err(ResponseError::UnownedCosmetic { id });
				}

				let allowed = CosmeticAllowedSlot::find()
					.filter(cosmetic_allowed_slot::Column::CosmeticId.eq(id))
					.filter(cosmetic_allowed_slot::Column::Slot.eq(slot.clone()))
					.one(&txn)
					.await?
					.is_some();
				if !allowed {
					return Err(ResponseError::InvalidSlot {
						slot: format!("{slot:?}"),
						id,
					});
				}

				PlayerEquippedCosmetic::insert(player_equipped_cosmetic::ActiveModel {
					player_id: Set(player.id),
					slot: Set(slot),
					cosmetic_id: Set(id),
					updated_at: ActiveValue::NotSet,
				})
				.on_conflict(
					OnConflict::columns([
						player_equipped_cosmetic::Column::PlayerId,
						player_equipped_cosmetic::Column::Slot,
					])
					.update_column(player_equipped_cosmetic::Column::CosmeticId)
					.to_owned(),
				)
				.exec(&txn)
				.await?;
			} else {
				PlayerEquippedCosmetic::delete_many()
					.filter(player_equipped_cosmetic::Column::PlayerId.eq(player.id))
					.filter(player_equipped_cosmetic::Column::Slot.eq(slot))
					.exec(&txn)
					.await?;
			}
		}

		txn.commit().await?;
	}

	Ok(NoContent)
}
