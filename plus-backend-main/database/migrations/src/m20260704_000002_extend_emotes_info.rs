use sea_orm_migration::prelude::*;

use crate::m20260704_000000_create_collections_table::Collections;

#[derive(DeriveIden)]
pub enum Emote {
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
					.table(Emote::Table)
					.add_column_if_not_exists(
						ColumnDef::new(Emote::StripeProductId).null().text(),
					)
					.add_column_if_not_exists(
						ColumnDef::new(Emote::BasePrice).null().float(),
					)
					.add_column_if_not_exists(
						ColumnDef::new(Emote::DiscountRate).null().integer(),
					)
					.add_column_if_not_exists(
						ColumnDef::new(Emote::Collection).null().integer(),
					)
					.add_foreign_key(
						TableForeignKey::new()
							.name("fk_emote_collections")
							.from_tbl(Emote::Table)
							.from_col(Emote::Collection)
							.to_tbl(Collections::Table)
							.to_col(Collections::Id)
							.on_delete(ForeignKeyAction::SetNull)
							.on_update(ForeignKeyAction::Cascade),
					)
					.add_column_if_not_exists(
						ColumnDef::new(Emote::Description).null().text(),
					)
					.to_owned(),
			)
			.await
	}

	async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.alter_table(
				TableAlterStatement::new()
					.table(Emote::Table)
					.drop_column(Emote::StripeProductId)
					.drop_column(Emote::BasePrice)
					.drop_column(Emote::DiscountRate)
					.drop_column(Emote::Collection)
					.drop_foreign_key("fk_emote_collections")
					.to_owned(),
			)
			.await
	}
}
