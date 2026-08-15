use aide::{
	OperationIo,
	axum::{ApiRouter, routing::post_with},
	transform::TransformOperation,
};
use axum::{Json, extract::State, http::StatusCode, response::IntoResponse};
use entities::sea_orm_active_enums::{TransactionProvider, TransactionStatus};
use schemars::JsonSchema;
use sea_orm::{
	ActiveModelTrait, ActiveValue, ColumnTrait, EntityTrait, QueryFilter, Set,
	TransactionTrait,
};
use serde::Deserialize;
use uuid::Uuid;

use crate::{
	api::{ApiState, account::AdminPlayer, websocket::structs::ClientBoundPacket},
	database::DatabaseUserExt,
};

#[derive(thiserror::Error, Debug, OperationIo)]
pub enum GrantError {
	#[error("The requested cosmetic does not exist")]
	MissingCosmetic,
	#[error("Unable to query database: {0}")]
	Database(#[from] sea_orm::error::DbErr),
}

impl IntoResponse for GrantError {
	fn into_response(self) -> axum::response::Response {
		(
			match self {
				Self::MissingCosmetic => StatusCode::NOT_FOUND,
				Self::Database(_) => StatusCode::INTERNAL_SERVER_ERROR,
			},
			self.to_string(),
		)
			.into_response()
	}
}

fn endpoint_doc(op: TransformOperation) -> TransformOperation {
	op.id("grantCosmetic")
		.summary("Grant a cosmetic to a player")
		.description("Grants cosmetic ownership to a player. Admin role required.")
		.tag("cosmetics")
}

#[derive(Debug, Deserialize, JsonSchema)]
struct GrantRequest {
	player: Uuid,
	cosmetic_id: i32,
}

pub(super) fn router() -> ApiRouter<ApiState> {
	ApiRouter::new().api_route("/grant", post_with(self::endpoint, self::endpoint_doc))
}

#[tracing::instrument(level = "debug", skip(state))]
async fn endpoint(
	State(state): State<ApiState>,
	AdminPlayer(_admin): AdminPlayer,
	Json(body): Json<GrantRequest>,
) -> Result<StatusCode, GrantError> {
	use entities::{cosmetic, player_owned_cosmetic, prelude::*, transaction};

	let txn = state.database.begin().await?;
	let Some(cosmetic) = Cosmetic::find_by_id(body.cosmetic_id).one(&txn).await? else {
		return Err(GrantError::MissingCosmetic);
	};

	// Buying a cosmetic grants the whole group: if this cosmetic belongs to a
	// group, every variant under that group is granted at once. Ungrouped
	// cosmetics grant just themselves.
	let cosmetic_ids: Vec<i32> = match cosmetic.group_id {
		Some(group_id) => Cosmetic::find()
			.filter(cosmetic::Column::GroupId.eq(group_id))
			.all(&txn)
			.await?
			.into_iter()
			.map(|c| c.id)
			.collect(),
		None => vec![cosmetic.id],
	};

	let player = User::get_or_create(&txn, body.player).await?;
	let transaction = transaction::ActiveModel {
		player_id: Set(player.id),
		provider: Set(TransactionProvider::AdminGrant),
		stripe_payment_id: Set(None),
		status: Set(TransactionStatus::Completed),
		raw_metadata: Set(serde_json::json!({ "reason": "admin_grant" })),
		..Default::default()
	}
	.insert(&txn)
	.await?;

	PlayerOwnedCosmetic::insert_many(cosmetic_ids.iter().map(|&cosmetic_id| {
		player_owned_cosmetic::ActiveModel {
			player_id: Set(player.id),
			cosmetic_id: Set(cosmetic_id),
			acquired_via: Set(TransactionProvider::AdminGrant),
			transaction_id: Set(Some(transaction.id)),
			acquired_at: ActiveValue::NotSet,
		}
	}))
	.on_conflict_do_nothing()
	.exec(&txn)
	.await?;
	txn.commit().await?;

	let connection_ids = state
		.realtime
		.connections_by_owner
		.read()
		.await
		.get(&body.player)
		.cloned()
		.unwrap_or_default();
	if !connection_ids.is_empty() {
		let connections = state.realtime.connections.read().await;
		for connection_id in connection_ids {
			let Some(connection) = connections.get(&connection_id) else {
				continue;
			};
			let _ = connection.tx.send(ClientBoundPacket::OwnershipUpdated {
				player: body.player,
				cosmetic_ids: cosmetic_ids.clone(),
				emote_ids: Vec::new(),
				revoked: false,
			});
		}
	}

	Ok(StatusCode::NO_CONTENT)
}
