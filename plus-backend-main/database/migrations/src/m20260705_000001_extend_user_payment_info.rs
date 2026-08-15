use sea_orm_migration::prelude::*;

#[derive(DeriveIden)]
pub enum User {
	Table,
	RefundCount,
}
#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait::async_trait]
impl MigrationTrait for Migration {
	async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.alter_table(
				TableAlterStatement::new()
					.table(User::Table)
					.add_column(
						ColumnDef::new(User::RefundCount)
							.integer()
							.default(0)
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
					.table(User::Table)
					.drop_column(User::RefundCount)
					.to_owned(),
			)
			.await
	}
}
