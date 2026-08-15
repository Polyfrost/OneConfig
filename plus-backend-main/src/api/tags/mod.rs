mod apply;
mod create;
mod list;
mod remove;

use std::collections::HashMap;

use aide::axum::ApiRouter;
use entities::sea_orm_active_enums::TagType;
use schemars::JsonSchema;
use sea_orm::{
	ColumnTrait, ConnectionTrait, DatabaseConnection, DbErr, EntityTrait, QueryFilter,
};
use serde::Serialize;

use crate::api::ApiState;

/// The tags applied to a cosmetic, grouped by their type.
#[derive(Clone, Debug, Default, Serialize, JsonSchema)]
pub(crate) struct CosmeticTags {
	/// The names of the `color` tags applied to this cosmetic.
	colors: Vec<String>,
	/// The names of the `custom` tags applied to this cosmetic.
	custom: Vec<String>,
	/// pretty self explanatory huh?
	category: Vec<String>,
}

/// Fetches the tags for each of the given cosmetic ids, grouped by type.
///
/// Cosmetics with no tags are absent from the map; callers should treat a
/// missing entry as an empty [`CosmeticTags`].
pub(crate) async fn tags_for_cosmetics(
	db: &DatabaseConnection,
	cosmetic_ids: &[i32],
) -> Result<HashMap<i32, CosmeticTags>, DbErr> {
	use entities::{prelude::*, tags_cosmetic};

	if cosmetic_ids.is_empty() {
		return Ok(HashMap::new());
	}

	let rows = TagsCosmetic::find()
		.filter(entities::tags::Column::TagType.ne(TagType::Category))
		.filter(tags_cosmetic::Column::CosmeticId.is_in(cosmetic_ids.iter().copied()))
		.find_also_related(Tags)
		.all(db)
		.await?;

	let mut grouped: HashMap<i32, CosmeticTags> = HashMap::new();
	for (link, tag) in rows {
		let Some(tag) = tag else {
			continue;
		};

		let entry = grouped.entry(link.cosmetic_id).or_default();
		match tag.tag_type {
			TagType::Color => entry.colors.push(tag.name),
			TagType::Custom => entry.custom.push(tag.name),
			TagType::Category => entry.category.push(tag.name),
		}
	}

	Ok(grouped)
}

async fn expand_groups<C: ConnectionTrait>(
	db: &C,
	cosmetic_ids: &[i32],
) -> Result<Option<Vec<i32>>, DbErr> {
	use std::collections::HashSet;

	use entities::{cosmetic, prelude::*};

	let requested: HashSet<i32> = cosmetic_ids.iter().copied().collect();
	let cosmetics = Cosmetic::find()
		.filter(cosmetic::Column::Id.is_in(requested.iter().copied()))
		.all(db)
		.await?;
	if cosmetics.len() != requested.len() {
		return Ok(None);
	}

	let group_ids: Vec<i32> = cosmetics.iter().filter_map(|c| c.group_id).collect();
	let mut expanded: HashSet<i32> = requested;
	if !group_ids.is_empty() {
		expanded.extend(
			Cosmetic::find()
				.filter(cosmetic::Column::GroupId.is_in(group_ids))
				.all(db)
				.await?
				.into_iter()
				.map(|c| c.id),
		);
	}

	Ok(Some(expanded.into_iter().collect()))
}

pub(super) async fn setup_router() -> ApiRouter<ApiState> {
	ApiRouter::new().nest(
		"/tags",
		ApiRouter::new()
			.merge(list::router())
			.merge(create::router())
			.merge(apply::router())
			.merge(remove::router()),
	)
}
