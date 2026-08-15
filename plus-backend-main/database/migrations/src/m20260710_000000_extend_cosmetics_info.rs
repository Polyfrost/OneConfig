use sea_orm_migration::prelude::*;

#[derive(DeriveIden)]
pub enum Cosmetic {
	Table,
	PurchaseCount,
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
						ColumnDef::new(Cosmetic::PurchaseCount)
							.integer()
							.not_null()
							.default(0),
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
					.drop_column(Cosmetic::PurchaseCount)
					.to_owned(),
			)
			.await
	}
}
