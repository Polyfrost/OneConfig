use sea_orm_migration::prelude::*;

#[derive(DeriveIden)]
pub enum Collections {
	Table,
	AssetId,
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
			.alter_table(
				TableAlterStatement::new()
					.table(Collections::Table)
					.add_column(ColumnDef::new(Collections::AssetId).integer().null())
					.add_foreign_key(
						ForeignKeyCreateStatement::new()
							.name("fk_collection_asset")
							.from_tbl(Collections::Table)
							.from_col(Collections::AssetId)
							.to_tbl(Asset::Table)
							.to_col(Asset::Id)
							.get_foreign_key(),
					)
					.to_owned(),
			)
			.await
	}

	async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.alter_table(
				TableAlterStatement::new()
					.table(Collections::Table)
					.drop_column(Collections::AssetId)
					.drop_foreign_key("fk_collection_asset")
					.to_owned(),
			)
			.await
	}
}
