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
use chrono::{DateTime, FixedOffset};
use entities::sea_orm_active_enums::CosmeticType;
use schemars::JsonSchema;
use sea_orm::{
	ColumnTrait, Condition, ConnectionTrait, EntityTrait, FromQueryResult, JoinType,
	Order, QueryFilter, QueryOrder, QuerySelect, QueryTrait, RelationTrait, Select,
	sea_query::{Alias, Asterisk, Expr, SimpleExpr},
};
use serde::{Deserialize, Serialize};

use crate::api::{
	ApiState,
	cosmetics::view::VariantView,
	tags::{CosmeticTags, tags_for_cosmetics},
};

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

/// The order in which results are returned.
#[derive(Debug, Default, Deserialize, JsonSchema)]
#[serde(rename_all = "lowercase")]
pub enum Sort {
	/// Oldest first (by creation time).
	Oldest,
	/// Newest first (by creation time).
	#[default]
	Newest,
	/// Cheapest first (by base price).
	Ascending,
	/// Most expensive first (by base price).
	Descending,
	/// the popularity is massive
	Popularity,
}

/// The maximum number of results allowed per page.
const MAX_NB: u64 = 100;

/// Minimum trigram `word_similarity` a name must reach against the search text
/// to be considered a fuzzy match. Higher = stricter. 0.3 tolerates typos and
/// partial words without drowning results in noise.
const SIMILARITY_THRESHOLD: f64 = 0.3;

fn default_nb() -> u64 {
	50
}

fn default_page() -> u64 {
	1
}

#[derive(Debug, Deserialize, JsonSchema)]
pub struct SearchQuery {
	/// The number of results per page, capped at 100.
	#[serde(default = "default_nb")]
	nb: u64,
	/// The 1-indexed page to return.
	#[serde(default = "default_page")]
	page: u64,
	/// The order results are returned in.
	#[serde(default)]
	sort: Sort,
	/// A substring to match against the name.
	text: Option<String>,
	/// Restrict results to one or more cosmetic types (including `emote`),
	/// comma-separated (e.g. `cape,emote`). Omit to return every type.
	#[serde(default, deserialize_with = "deserialize_types")]
	types: Option<Vec<CosmeticType>>,
	/// Restrict results to cosmetics carrying at least one of these tag names,
	/// comma-separated (e.g. `red,limited`). Omit to ignore tags.
	#[serde(default, deserialize_with = "deserialize_tags")]
	tags: Option<Vec<String>>,
	/// the collection id to search for
	collection: Option<i32>,
}

/// Parses a comma-separated list of tag names. Empty segments are ignored, and
/// an empty list is treated as no filter.
fn deserialize_tags<'de, D>(de: D) -> Result<Option<Vec<String>>, D::Error>
where
	D: serde::Deserializer<'de>,
{
	let Some(raw) = Option::<String>::deserialize(de)? else {
		return Ok(None);
	};

	let tags: Vec<String> = raw
		.split(',')
		.map(str::trim)
		.filter(|part| !part.is_empty())
		.map(str::to_owned)
		.collect();

	Ok((!tags.is_empty()).then_some(tags))
}

/// Parses a comma-separated list of cosmetic types (e.g. `cape,emote`),
/// deferring to each type's own deserialization. Empty segments are ignored,
/// and an empty list is treated as no filter.
fn deserialize_types<'de, D>(de: D) -> Result<Option<Vec<CosmeticType>>, D::Error>
where
	D: serde::Deserializer<'de>,
{
	use serde::de::{Error, IntoDeserializer};

	let Some(raw) = Option::<String>::deserialize(de)? else {
		return Ok(None);
	};

	let types = raw
		.split(',')
		.map(str::trim)
		.filter(|part| !part.is_empty())
		.map(|part| CosmeticType::deserialize(part.into_deserializer()))
		.collect::<Result<Vec<_>, serde::de::value::Error>>()
		.map_err(Error::custom)?;

	Ok((!types.is_empty()).then_some(types))
}

/// A single store entry, doesn't contain price id.
///
/// A grouped cosmetic collapses into one entry: the fields describe its
/// representative variant (the lowest `variant_order`, ties broken by id),
/// except `name`, which is the group's name. Price, description and tags are
/// shared across a group, so the representative's stand for the whole entry.
/// Every variant, the representative included, is listed in `variants`.
#[derive(Debug, Serialize, JsonSchema)]
struct CosmeticSearchInfo {
	/// The id of the representative variant, not of the group. Pass this to
	/// `/cosmetics/view/{id}`.
	id: i32,
	/// The group's name for a grouped cosmetic, the cosmetic's own name
	/// otherwise.
	name: String,
	description: Option<String>,
	collection: Option<i32>,
	r#type: CosmeticType,
	base_price: Option<f32>,
	discount_rate: Option<i32>,
	asset_id: Option<i32>,
	cover_asset_id: Option<i32>,
	created_at: DateTime<FixedOffset>,
	/// The representative variant's tags.
	tags: CosmeticTags,
	/// Every variant in this cosmetic's group, ordered by `variant_order`, so
	/// this is the whole swatch list rather than something to append to `id`.
	/// The entry at `id` is the first element. Null, not empty, for an ungrouped
	/// cosmetic, which has no variants to pick between.
	///
	/// `/cosmetics/view/{id}` returns the same list, except that it filters on
	/// `enabled` alone where this also omits unpriced variants, which have
	/// nothing to show in the store.
	variants: Option<Vec<VariantView>>,
}

impl CosmeticSearchInfo {
	fn from_cosmetic(
		cosmetic: entities::cosmetic::Model,
		name: String,
		tags: CosmeticTags,
		variants: Option<Vec<VariantView>>,
	) -> Self {
		CosmeticSearchInfo {
			id: cosmetic.id,
			name,
			description: cosmetic.description,
			collection: cosmetic.collection,
			r#type: cosmetic.r#type,
			base_price: cosmetic.base_price,
			discount_rate: cosmetic.discount_rate,
			asset_id: cosmetic.asset_id,
			cover_asset_id: cosmetic.cover_asset_id,
			created_at: cosmetic.created_at,
			tags,
			variants,
		}
	}
}

/// Pagination metadata describing the returned page within the full result set.
#[derive(Debug, Default, Serialize, JsonSchema)]
struct Pagination {
	/// The 1-indexed page these results are from.
	page: u64,
	/// The number of results on this page.
	count: u64,
	/// The total number of results matching the query across all pages.
	total_items: u64,
	/// The total number of pages available for the query.
	total_pages: u64,
}

#[derive(Debug, Default, Serialize, JsonSchema)]
pub struct SearchResponse {
	results: Vec<CosmeticSearchInfo>,
	pagination: Pagination,
}

fn endpoint_doc(op: TransformOperation) -> TransformOperation {
	op.id("searchCosmetics")
		.summary("Search cosmetics and emotes")
		.description(
			"Lists enabled cosmetics and emotes, paginated by `nb` per page and \
			 1-indexed `page`, optionally filtered by a `text` substring of the name \
			 and a `type`. Variants of the same cosmetic collapse into one result \
			 listing every variant in `variants`, so `nb` and the pagination counts \
			 are in whole cosmetics, not variants. A group matches if any of its \
			 variants does.",
		)
		.tag("cosmetics")
}

type BucketKey = (Option<i32>, Option<i32>);

#[derive(Debug, FromQueryResult)]
struct BucketRow {
	group_id: Option<i32>,
	solo_id: Option<i32>,
}

impl BucketRow {
	fn key(&self) -> BucketKey {
		(self.group_id, self.solo_id)
	}
}

fn bucket_key_of(cosmetic: &entities::cosmetic::Model) -> BucketKey {
	match cosmetic.group_id {
		Some(group_id) => (Some(group_id), None),
		None => (None, Some(cosmetic.id)),
	}
}

fn solo_id_expr() -> SimpleExpr {
	use entities::cosmetic;

	Expr::case(
		Expr::col((cosmetic::Entity, cosmetic::Column::GroupId)).is_null(),
		Expr::col((cosmetic::Entity, cosmetic::Column::Id)),
	)
	.into()
}

fn filtered(query: &SearchQuery) -> Select<entities::prelude::Cosmetic> {
	use entities::{cosmetic, prelude::*, tags, tags_cosmetic};

	let mut find = Cosmetic::find()
		.filter(cosmetic::Column::Enabled.eq(true))
		.filter(cosmetic::Column::BasePrice.is_not_null());

	if let Some(text) = &query.text {
		// Match against both the variant's own name and its group's name. The
		// left join makes `cosmetic_group.name` available (NULL for ungrouped
		// cosmetics); the same join is reused for relevance ranking.
		let pattern = format!("%{}%", text);
		find = find
			.join(JoinType::LeftJoin, cosmetic::Relation::CosmeticGroup.def())
			.filter(
				Condition::any()
					// Case-insensitive substring: exact "cape" inside "Red Cape".
					.add(Expr::cust_with_values("cosmetic.name ILIKE $1", [pattern.clone()]))
					.add(Expr::cust_with_values("cosmetic_group.name ILIKE $1", [pattern]))
					// Trigram fuzzy: tolerates typos ("caep") and word order.
					.add(Expr::cust_with_values(
						"word_similarity($1, cosmetic.name) >= $2",
						[
							sea_orm::Value::from(text.clone()),
							sea_orm::Value::from(SIMILARITY_THRESHOLD),
						],
					))
					.add(Expr::cust_with_values(
						"word_similarity($1, cosmetic_group.name) >= $2",
						[
							sea_orm::Value::from(text.clone()),
							sea_orm::Value::from(SIMILARITY_THRESHOLD),
						],
					)),
			);
	}
	if let Some(kinds) = &query.types {
		find = find.filter(cosmetic::Column::Type.is_in(kinds.clone()));
	}
	if let Some(collection_id) = &query.collection {
		find = find.filter(cosmetic::Column::Collection.eq(collection_id.to_owned()))
	}

	if let Some(names) = &query.tags {
		find = find.filter(
			cosmetic::Column::Id.in_subquery(
				sea_orm::sea_query::Query::select()
					.column(tags_cosmetic::Column::CosmeticId)
					.from(tags_cosmetic::Entity)
					.inner_join(
						tags::Entity,
						Expr::col((tags_cosmetic::Entity, tags_cosmetic::Column::TagId))
							.equals((tags::Entity, tags::Column::Id)),
					)
					.and_where(tags::Column::Name.is_in(names.clone()))
					.to_owned(),
			),
		);
	}

	find
}

pub(super) fn router() -> ApiRouter<ApiState> {
	ApiRouter::new().api_route(
		"/cosmetics/search",
		get_with(self::endpoint, self::endpoint_doc),
	)
}

#[tracing::instrument(level = "debug", skip(state))]
async fn endpoint(
	State(state): State<ApiState>,
	Query(query): Query<SearchQuery>,
) -> Result<Json<SearchResponse>, SearchError> {
	use std::collections::HashMap;

	use entities::{cosmetic, cosmetic_group, prelude::*};

	let nb = query.nb.min(MAX_NB);
	let offset = nb.saturating_mul(query.page.saturating_sub(1));

	let (sort_key, order) = match query.sort {
		Sort::Oldest => (
			Expr::col((cosmetic::Entity, cosmetic::Column::CreatedAt)).min(),
			Order::Asc,
		),
		Sort::Newest => (
			Expr::col((cosmetic::Entity, cosmetic::Column::CreatedAt)).max(),
			Order::Desc,
		),
		Sort::Ascending => (
			Expr::col((cosmetic::Entity, cosmetic::Column::BasePrice)).min(),
			Order::Asc,
		),
		Sort::Descending => (
			Expr::col((cosmetic::Entity, cosmetic::Column::BasePrice)).max(),
			Order::Desc,
		),
		Sort::Popularity => (
			Expr::col((cosmetic::Entity, cosmetic::Column::PurchaseCount)).sum(),
			Order::Asc,
		),
	};

	let mut buckets = filtered(&query)
		.select_only()
		.column(cosmetic::Column::GroupId)
		.expr_as(solo_id_expr(), "solo_id")
		.group_by(Expr::col((cosmetic::Entity, cosmetic::Column::GroupId)))
		.group_by(solo_id_expr());

	let total_items = {
		let count = sea_orm::sea_query::Query::select()
			.expr(Expr::col(Asterisk).count())
			.from_subquery(buckets.clone().into_query(), Alias::new("buckets"))
			.to_owned();
		let backend = state.database.get_database_backend();

		state
			.database
			.query_one(backend.build(&count))
			.await?
			.map(|row| row.try_get_by_index::<i64>(0))
			.transpose()?
			.unwrap_or(0)
			.max(0) as u64
	};

	// With a search text, best matches lead; the requested `sort` only breaks
	// ties between equally-relevant results. Without text, `sort` drives fully.
	if let Some(text) = &query.text {
		let relevance = Expr::cust_with_values(
			"GREATEST(\
				MAX(word_similarity($1, cosmetic.name)), \
				MAX(COALESCE(word_similarity($2, cosmetic_group.name), 0))\
			)",
			[
				sea_orm::Value::from(text.clone()),
				sea_orm::Value::from(text.clone()),
			],
		);
		buckets = buckets.order_by(relevance, Order::Desc);
	}

	let page: Vec<BucketRow> = buckets
		.order_by(sort_key, order)
		.order_by(
			Expr::col((cosmetic::Entity, cosmetic::Column::Id)).min(),
			Order::Asc,
		)
		.offset(offset)
		.limit(nb)
		.into_model()
		.all(&state.database)
		.await?;

	let group_ids: Vec<i32> = page.iter().filter_map(|row| row.group_id).collect();
	let solo_ids: Vec<i32> = page.iter().filter_map(|row| row.solo_id).collect();

	let mut members: HashMap<BucketKey, Vec<cosmetic::Model>> = HashMap::new();
	if !page.is_empty() {
		let mut belongs = Condition::any();
		if !group_ids.is_empty() {
			belongs = belongs.add(cosmetic::Column::GroupId.is_in(group_ids.clone()));
		}
		if !solo_ids.is_empty() {
			belongs = belongs.add(cosmetic::Column::Id.is_in(solo_ids));
		}

		let cosmetics = Cosmetic::find()
			.filter(cosmetic::Column::Enabled.eq(true))
			.filter(cosmetic::Column::BasePrice.is_not_null())
			.filter(belongs)
			.order_by_asc(cosmetic::Column::VariantOrder)
			.order_by_asc(cosmetic::Column::Id)
			.all(&state.database)
			.await?;

		for cosmetic in cosmetics {
			if super::is_redundant_variant(cosmetic.variant_name.as_deref()) {
				continue;
			}
			members
				.entry(bucket_key_of(&cosmetic))
				.or_default()
				.push(cosmetic);
		}
	}

	let group_names: HashMap<i32, String> = if group_ids.is_empty() {
		HashMap::new()
	} else {
		CosmeticGroup::find()
			.filter(cosmetic_group::Column::Id.is_in(group_ids))
			.all(&state.database)
			.await?
			.into_iter()
			.map(|group| (group.id, group.name))
			.collect()
	};

	let representative_ids: Vec<i32> = page
		.iter()
		.filter_map(|row| members.get(&row.key())?.first().map(|c| c.id))
		.collect();
	let mut tags = tags_for_cosmetics(&state.database, &representative_ids).await?;

	let mut results: Vec<CosmeticSearchInfo> = Vec::with_capacity(page.len());
	for row in &page {
		let Some(members) = members.remove(&row.key()) else {
			continue;
		};

		let variants = row.group_id.map(|_| {
			members
				.iter()
				.cloned()
				.map(VariantView::from_cosmetic)
				.collect()
		});

		let mut members = members.into_iter();
		let Some(representative) = members.next() else {
			continue;
		};

		let name = match row.group_id.and_then(|id| group_names.get(&id)) {
			Some(name) => name.clone(),
			None => representative
				.name
				.clone()
				.unwrap_or_else(|| format!("Cosmetic {}", representative.id)),
		};
		let tags = tags.remove(&representative.id).unwrap_or_default();

		results.push(CosmeticSearchInfo::from_cosmetic(
			representative,
			name,
			tags,
			variants,
		));
	}

	let pagination = Pagination {
		page: query.page,
		count: results.len() as u64,
		total_items,
		total_pages: if nb == 0 { 0 } else { total_items.div_ceil(nb) },
	};

	Ok(Json(SearchResponse {
		results,
		pagination,
	}))
}
