use sea_orm_migration::prelude::*;

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait::async_trait]
impl MigrationTrait for Migration {
	async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.alter_table(
				TableAlterStatement::new()
					.table(Cosmetic::Table)
					.add_column(ColumnDef::new(Cosmetic::StripePriceId).text().null())
					.to_owned(),
			)
			.await?;

		manager
			.alter_table(
				TableAlterStatement::new()
					.table(Emote::Table)
					.add_column(ColumnDef::new(Emote::StripePriceId).text().null())
					.to_owned(),
			)
			.await
	}

	async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.alter_table(
				TableAlterStatement::new()
					.table(Cosmetic::Table)
					.drop_column(Cosmetic::StripePriceId)
					.to_owned(),
			)
			.await?;

		manager
			.alter_table(
				TableAlterStatement::new()
					.table(Emote::Table)
					.drop_column(Emote::StripePriceId)
					.to_owned(),
			)
			.await
	}
}

#[derive(DeriveIden)]
enum Cosmetic {
	Table,
	StripePriceId,
}

#[derive(DeriveIden)]
enum Emote {
	Table,
	StripePriceId,
}
