use sea_orm_migration::prelude::*;

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait::async_trait]
impl MigrationTrait for Migration {
	async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.alter_table(
				TableAlterStatement::new()
					.table(UserCosmetic::Table)
					.add_column(
						ColumnDef::new(UserCosmetic::Active)
							.boolean()
							.default(false)
							.not_null(),
					)
					.to_owned(),
			)
			.await
	}

	async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.alter_table(
				TableAlterStatement::new()
					.table(UserCosmetic::Table)
					.drop_column(UserCosmetic::Active)
					.to_owned(),
			)
			.await
	}
}

#[derive(DeriveIden)]
enum UserCosmetic {
	Table,
	Active,
}
