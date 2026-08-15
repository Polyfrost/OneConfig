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
use sea_orm::{
	ColumnTrait, EntityTrait, Order, PaginatorTrait, QueryFilter, QueryOrder, QuerySelect,
};
use serde::{Deserialize, Serialize};

use crate::api::{ApiState, bundles::BundleInfo};

#[derive(thiserror::Error, Debug, OperationIo)]
pub enum SearchError {
	#[error("Unable to query database: {0}")]
	Database(#[from] sea_orm::error::DbErr),
}

impl IntoResponse for SearchError {
	fn into_response(self) -> axum::response::Response {
		(
			match self {
				Self::Database(_) => StatusCode::INTERNAL_SERVER_ERROR,
			},
			self.to_string(),
		)
			.into_response()
	}
}

/// The order in which bundles are returned.
#[derive(Debug, Default, Deserialize, JsonSchema)]
#[serde(rename_all = "lowercase")]
pub enum Sort {
	/// Oldest bundles first (by creation time).
	Oldest,
	/// Newest bundles first (by creation time).
	#[default]
	Newest,
	/// Cheapest bundles first (by base price).
	Ascending,
	/// Most expensive bundles first (by base price).
	Descending,
}

/// The maximum number of bundles allowed per page.
const MAX_NB: u64 = 100;

fn default_nb() -> u64 {
	50
}

fn default_page() -> u64 {
	1
}

#[derive(Debug, Deserialize, JsonSchema)]
pub struct SearchQuery {
	/// The number of bundles per page, capped at 100.
	#[serde(default = "default_nb")]
	nb: u64,
	/// The 1-indexed page to return.
	#[serde(default = "default_page")]
	page: u64,
	/// The order bundles are returned in.
	#[serde(default)]
	sort: Sort,
	/// A substring to match against bundle names.
	text: Option<String>,
}

/// Pagination metadata describing the returned page within the full result set.
#[derive(Debug, Default, Serialize, JsonSchema)]
struct Pagination {
	/// The 1-indexed page these bundles are from.
	page: u64,
	/// The number of bundles on this page.
	count: u64,
	/// The total number of bundles matching the query across all pages.
	total_items: u64,
	/// The total number of pages available for the query.
	total_pages: u64,
}

#[derive(Debug, Default, Serialize, JsonSchema)]
pub struct SearchResponse {
	bundles: Vec<BundleInfo>,
	pagination: Pagination,
}

fn endpoint_doc(op: TransformOperation) -> TransformOperation {
	op.id("searchBundles")
		.summary("Search bundles")
		.description(
			"Lists enabled bundles, paginated by `nb` per page and 1-indexed `page`, \
			 optionally filtered by a `text` substring of the bundle name.",
		)
		.tag("bundles")
}

pub(super) fn router() -> ApiRouter<ApiState> {
	ApiRouter::new().api_route("/search", get_with(self::endpoint, self::endpoint_doc))
}

#[tracing::instrument(level = "debug", skip(state))]
async fn endpoint(
	State(state): State<ApiState>,
	Query(query): Query<SearchQuery>,
) -> Result<Json<SearchResponse>, SearchError> {
	use entities::{bundles, prelude::*};

	// [nb * (page - 1); nb * page).
	let nb = query.nb.min(MAX_NB);
	let offset = nb.saturating_mul(query.page.saturating_sub(1));

	let (column, order) = match query.sort {
		Sort::Oldest => (bundles::Column::CreatedAt, Order::Asc),
		Sort::Newest => (bundles::Column::CreatedAt, Order::Desc),
		Sort::Ascending => (bundles::Column::BasePrice, Order::Asc),
		Sort::Descending => (bundles::Column::BasePrice, Order::Desc),
	};

	let mut find = Bundles::find()
		.filter(bundles::Column::Enabled.eq(true))
		.filter(bundles::Column::BasePrice.is_not_null());
	if let Some(text) = query.text {
		find = find.filter(bundles::Column::Name.contains(text));
	}

	let total_items = find.clone().count(&state.database).await?;

	let bundles = find
		.order_by(column, order)
		.order_by(bundles::Column::Id, Order::Asc)
		.offset(offset)
		.limit(nb)
		.all(&state.database)
		.await?;

	let bundles: Vec<BundleInfo> = bundles.into_iter().map(BundleInfo::from).collect();

	let pagination = Pagination {
		page: query.page,
		count: bundles.len() as u64,
		total_items,
		total_pages: if nb == 0 { 0 } else { total_items.div_ceil(nb) },
	};

	Ok(Json(SearchResponse {
		bundles,
		pagination,
	}))
}
