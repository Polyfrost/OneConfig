use sea_orm_migration::prelude::*;

use crate::m20260704_000000_create_collections_table::Collections;

#[derive(DeriveIden)]
pub enum Bundles {
	Table,
	Id,
	Name,
	Description,
	AssetId,
	Enabled,
	Collection,
	StripeProductId,
	StripePriceId,
	BasePrice,
	DiscountRate,
	CreatedAt,
}

#[derive(DeriveIden)]
pub enum BundlesCosmetics {
	Table,
	BundleId,
	CosmeticId,
}
#[derive(DeriveIden)]
pub enum BundlesEmotes {
	Table,
	BundleId,
	CosmeticId,
}
#[derive(DeriveIden)]
pub enum Cosmetic {
	Table,
	Id,
}
#[derive(DeriveIden)]
pub enum Emote {
	Table,
	Id,
}
#[derive(DeriveIden)]
pub enum Asset {
	Table,
	Id,
}

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait::async_trait]
impl MigrationTrait for Migration {
	async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.create_table(
				Table::create()
					.table(Bundles::Table)
					.if_not_exists()
					.col(
						ColumnDef::new(Bundles::Id)
							.integer()
							.auto_increment()
							.primary_key(),
					)
					.col(ColumnDef::new(Bundles::Name).text().not_null())
					.col(ColumnDef::new(Bundles::Description).text().null())
					.col(ColumnDef::new(Bundles::AssetId).integer().null())
					.foreign_key(
						ForeignKeyCreateStatement::new()
							.name("fk_bundles_asset")
							.from_tbl(Bundles::Table)
							.from_col(Bundles::AssetId)
							.to_tbl(Asset::Table)
							.to_col(Asset::Id)
							.on_delete(ForeignKeyAction::SetNull),
					)
					.col(
						ColumnDef::new(Bundles::Enabled)
							.boolean()
							.default(false)
							.not_null(),
					)
					.col(ColumnDef::new(Bundles::Collection).integer().null())
					.foreign_key(
						ForeignKeyCreateStatement::new()
							.name("fk_bundles_collection")
							.from_tbl(Bundles::Table)
							.from_col(Bundles::Collection)
							.to_tbl(Collections::Table)
							.to_col(Collections::Id)
							.on_delete(ForeignKeyAction::SetNull),
					)
					.col(ColumnDef::new(Bundles::StripeProductId).text().null())
					.col(ColumnDef::new(Bundles::StripePriceId).text().null())
					.col(ColumnDef::new(Bundles::BasePrice).float().null())
					.col(ColumnDef::new(Bundles::DiscountRate).integer().null())
					.col(
						ColumnDef::new(Bundles::CreatedAt)
							.timestamp_with_time_zone()
							.not_null()
							.default(Expr::current_timestamp()),
					)
					.to_owned(),
			)
			.await?;

		manager
			.create_table(
				TableCreateStatement::new()
					.table(BundlesCosmetics::Table)
					.if_not_exists()
					.col(
						ColumnDef::new(BundlesCosmetics::BundleId)
							.integer()
							.not_null(),
					)
					.col(
						ColumnDef::new(BundlesCosmetics::CosmeticId)
							.integer()
							.not_null(),
					)
					.foreign_key(
						ForeignKeyCreateStatement::new()
							.name("fk_bundle_cosmetic_to_bundle")
							.from_tbl(BundlesCosmetics::Table)
							.from_col(BundlesCosmetics::BundleId)
							.to_tbl(Bundles::Table)
							.to_col(Bundles::Id)
							.on_delete(ForeignKeyAction::Cascade),
					)
					.foreign_key(
						ForeignKeyCreateStatement::new()
							.name("fk_bundle_cosmetic_to_cosmetic")
							.from_tbl(BundlesCosmetics::Table)
							.from_col(BundlesCosmetics::CosmeticId)
							.to_tbl(Cosmetic::Table)
							.to_col(Cosmetic::Id)
							.on_delete(ForeignKeyAction::Cascade),
					)
					.primary_key(
						Index::create()
							.col(BundlesCosmetics::BundleId)
							.col(BundlesCosmetics::CosmeticId),
					)
					.to_owned(),
			)
			.await?;
		manager
			.create_table(
				TableCreateStatement::new()
					.table(BundlesEmotes::Table)
					.if_not_exists()
					.col(ColumnDef::new(BundlesEmotes::BundleId).integer().not_null())
					.col(
						ColumnDef::new(BundlesEmotes::CosmeticId)
							.integer()
							.not_null(),
					)
					.foreign_key(
						ForeignKeyCreateStatement::new()
							.name("fk_bundle_emote_to_bundle")
							.from_tbl(BundlesEmotes::Table)
							.from_col(BundlesEmotes::BundleId)
							.to_tbl(Bundles::Table)
							.to_col(Bundles::Id),
					)
					.foreign_key(
						ForeignKeyCreateStatement::new()
							.name("fk_bundle_emote_to_emote")
							.from_tbl(BundlesEmotes::Table)
							.from_col(BundlesEmotes::CosmeticId)
							.to_tbl(Emote::Table)
							.to_col(Emote::Id),
					)
					.primary_key(
						Index::create()
							.col(BundlesEmotes::BundleId)
							.col(BundlesEmotes::CosmeticId),
					)
					.to_owned(),
			)
			.await
	}

	async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.drop_table(Table::drop().table(Bundles::Table).to_owned())
			.await?;
		manager
			.drop_table(Table::drop().table(BundlesEmotes::Table).to_owned())
			.await?;
		manager
			.drop_table(Table::drop().table(BundlesCosmetics::Table).to_owned())
			.await
	}
}
