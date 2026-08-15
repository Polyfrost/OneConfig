use entities::{bundles, bundles_cosmetics, cosmetic, prelude::*};
use sea_orm::{DbErr, prelude::*, sea_query::Query};

pub(super) async fn cosmetics_for_price(
	db: &impl ConnectionTrait,
	price: &str,
) -> Result<Vec<cosmetic::Model>, DbErr> {
	let cosmetics = Cosmetic::find()
		.filter(cosmetic::Column::StripePriceId.eq(price))
		.all(db)
		.await?;
	if !cosmetics.is_empty() {
		return Ok(cosmetics);
	}

	let Some(bundle) = Bundles::find()
		.filter(bundles::Column::StripePriceId.eq(price))
		.one(db)
		.await?
	else {
		return Ok(cosmetics);
	};

	Cosmetic::find()
		.filter(
			cosmetic::Column::Id.in_subquery(
				Query::select()
					.column(bundles_cosmetics::Column::CosmeticId)
					.from(bundles_cosmetics::Entity)
					.and_where(bundles_cosmetics::Column::BundleId.eq(bundle.id))
					.to_owned(),
			),
		)
		.all(db)
		.await
}

pub(super) fn display_name(cosmetic: &cosmetic::Model) -> String {
	let base = cosmetic
		.name
		.clone()
		.unwrap_or_else(|| format!("Cosmetic #{}", cosmetic.id));

	match &cosmetic.variant_name {
		Some(variant) => format!("{base} ({variant})"),
		None => base,
	}
}
