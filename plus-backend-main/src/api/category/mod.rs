mod list;

use aide::axum::ApiRouter;

use crate::api::ApiState;

// Fetches the category for each of the given cosmetic ids, grouped by type.

// Cosmetics with no category are absent from the map; callers should treat a
// missing entry as an empty [`CosmeticTags`].
// pub(crate) async fn category_for_cosmetics(
// 	db: &DatabaseConnection,
// 	cosmetic_ids: &[i32],
// ) -> Result<
// 	std::vec::Vec<(
// 		entities::tags_cosmetic::Model,
// 		std::option::Option<entities::tags::Model>,
// 	)>,
// 	DbErr,
// > {
// 	use entities::{prelude::*, tags_cosmetic};

// 	if cosmetic_ids.is_empty() {
// 		return Ok(vec![]);
// 	}

// 	let rows = TagsCosmetic::find()
// 		.filter(entities::tags::Column::TagType.eq(TagType::Category))
// 		.filter(tags_cosmetic::Column::CosmeticId.is_in(cosmetic_ids.iter().copied()))
// 		.find_also_related(Tags)
// 		.all(db)
// 		.await?;

// 	Ok(rows)
// }

pub(super) async fn setup_router() -> ApiRouter<ApiState> {
	ApiRouter::new().nest("/category", ApiRouter::new().merge(list::router()))
}
