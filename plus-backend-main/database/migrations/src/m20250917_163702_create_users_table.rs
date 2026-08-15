use sea_orm_migration::prelude::*;

#[derive(DeriveIden)]
pub enum User {
	Table,
	Id,
	MinecraftUuid,
}

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait::async_trait]
impl MigrationTrait for Migration {
	async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.create_table(
				Table::create()
					.table(User::Table)
					.if_not_exists()
					.col(
						ColumnDef::new(User::Id)
							.integer()
							.auto_increment()
							.primary_key(),
					)
					.col(
						ColumnDef::new(User::MinecraftUuid)
							.uuid()
							.unique_key()
							.not_null(),
					)
					.to_owned(),
			)
			.await
	}

	async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.drop_table(Table::drop().table(User::Table).to_owned())
			.await
	}
}
