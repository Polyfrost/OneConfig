use aide::{OperationIo, transform::TransformOperation};
use axum::{
	Json,
	extract::{Query, State},
	http::StatusCode,
	response::IntoResponse,
};
use entities::sea_orm_active_enums::{TransactionProvider, TransactionStatus};
use schemars::JsonSchema;
use sea_orm::{ColumnTrait, EntityTrait, QueryFilter};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

use crate::api::{
	ApiState,
	account::{AuthenticatedPlayer, role_at_least},
};

#[derive(thiserror::Error, Debug, OperationIo)]
pub enum TransactionsError {
	#[error("Authenticated player does not have permission")]
	Forbidden,
	#[error("The requested player does not exist")]
	PlayerMissing,
	#[error("Unable to query database: {0}")]
	Database(#[from] sea_orm::error::DbErr),
}

impl IntoResponse for TransactionsError {
	fn into_response(self) -> axum::response::Response {
		(
			match self {
				Self::Forbidden => StatusCode::FORBIDDEN,
				Self::PlayerMissing => StatusCode::NOT_FOUND,
				Self::Database(_) => StatusCode::INTERNAL_SERVER_ERROR,
			},
			self.to_string(),
		)
			.into_response()
	}
}

pub fn endpoint_doc(op: TransformOperation) -> TransformOperation {
	op.id("getPlayerTransactions")
		.summary("Get player transactions")
		.description("Lists transactions for the authenticated player or for another player when the caller is elevated.")
		.tag("transactions")
}

#[derive(Debug, Deserialize, JsonSchema)]
pub struct TransactionsQuery {
	#[serde(default)]
	player: Option<Uuid>,
}

#[derive(Debug, Serialize, JsonSchema)]
struct TransactionInfo {
	id: i32,
	provider: TransactionProvider,
	stripe_payment_id: Option<String>,
	status: TransactionStatus,
	raw_metadata: serde_json::Value,
	amount: Option<f32>,
	discount_rate: Option<i32>,
	buyer: Option<Uuid>,
}

#[derive(Debug, Default, Serialize, JsonSchema)]
pub struct TransactionsResponse {
	transactions: Vec<TransactionInfo>,
}

#[tracing::instrument(level = "debug", skip(state))]
pub(super) async fn endpoint(
	State(state): State<ApiState>,
	AuthenticatedPlayer(authenticated): AuthenticatedPlayer,
	Query(query): Query<TransactionsQuery>,
) -> Result<Json<TransactionsResponse>, TransactionsError> {
	use entities::{prelude::*, transaction, user};

	let target_uuid = query.player.unwrap_or(authenticated.minecraft_uuid);
	if target_uuid != authenticated.minecraft_uuid
		&& !role_at_least(
			&authenticated.role,
			&entities::sea_orm_active_enums::PlayerRole::Moderator,
		) {
		return Err(TransactionsError::Forbidden);
	}

	let Some(player) = User::find()
		.filter(user::Column::MinecraftUuid.eq(target_uuid))
		.one(&state.database)
		.await?
	else {
		return Err(TransactionsError::PlayerMissing);
	};

	use futures::future::try_join_all;
	let transactions_raw = Transaction::find()
		.filter(transaction::Column::PlayerId.eq(player.id))
		.all(&state.database)
		.await?;

	// this case should never happen since this endpoint is pulling all
	// transactions made by user
	// instead: you should be calling on a specific transaction id that you get from
	// getting your own cosmetics, and fetch that one
	let transactions = try_join_all(transactions_raw.into_iter().map(|transaction| {
		let db = &state.database;
		async move {
			let buyer = if let Some(recipient) = transaction.buyer {
				User::find_by_id(recipient)
					.one(db)
					.await?
					.map(|rec| rec.minecraft_uuid)
			} else {
				None
			};

			Ok::<_, TransactionsError>(TransactionInfo {
				id: transaction.id,
				provider: transaction.provider,
				stripe_payment_id: transaction.stripe_payment_id,
				status: transaction.status,
				raw_metadata: transaction.raw_metadata,
				buyer,
				amount: transaction
					.amount
					.filter(|_| transaction.buyer.is_none_or(|id| id == player.id)),
				discount_rate: transaction
					.discount_rate
					.filter(|_| transaction.buyer.is_none_or(|id| id == player.id)),
			})
		}
	}))
	.await?;

	Ok(Json(TransactionsResponse { transactions }))
}
