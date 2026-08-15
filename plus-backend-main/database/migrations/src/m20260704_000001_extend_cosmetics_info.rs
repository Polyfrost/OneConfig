use sea_orm_migration::prelude::*;

use crate::m20260704_000000_create_collections_table::Collections;

#[derive(DeriveIden)]
pub enum Cosmetic {
	Table,
	StripeProductId,
	BasePrice,
	DiscountRate,
	Collection,
	Description,
}

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait::async_trait]
impl MigrationTrait for Migration {
	async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.alter_table(
				TableAlterStatement::new()
					.table(Cosmetic::Table)
					.add_column_if_not_exists(
						ColumnDef::new(Cosmetic::StripeProductId).null().text(),
					)
					.add_column_if_not_exists(
						ColumnDef::new(Cosmetic::BasePrice).null().float(),
					)
					.add_column_if_not_exists(
						ColumnDef::new(Cosmetic::DiscountRate).null().integer(),
					)
					.add_column_if_not_exists(
						ColumnDef::new(Cosmetic::Collection).null().integer(),
					)
					.add_foreign_key(
						TableForeignKey::new()
							.name("fk_cosmetics_collections")
							.from_tbl(Cosmetic::Table)
							.from_col(Cosmetic::Collection)
							.to_tbl(Collections::Table)
							.to_col(Collections::Id)
							.on_delete(ForeignKeyAction::SetNull)
							.on_update(ForeignKeyAction::Cascade),
					)
					.add_column_if_not_exists(
						ColumnDef::new(Cosmetic::Description).null().text(),
					)
					.to_owned(),
			)
			.await
	}

	async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.alter_table(
				TableAlterStatement::new()
					.table(Cosmetic::Table)
					.drop_column(Cosmetic::StripeProductId)
					.drop_column(Cosmetic::BasePrice)
					.drop_column(Cosmetic::DiscountRate)
					.drop_column(Cosmetic::Collection)
					.drop_foreign_key("fk_cosmetic_collections")
					.to_owned(),
			)
			.await
	}
}
