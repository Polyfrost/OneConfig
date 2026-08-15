use sea_orm_migration::prelude::*;

#[derive(DeriveIden)]
pub enum Tags {
	Table,
	DisplayName,
}
#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait::async_trait]
impl MigrationTrait for Migration {
	async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.alter_table(
				TableAlterStatement::new()
					.table(Tags::Table)
					.add_column_if_not_exists(
						ColumnDef::new(Tags::DisplayName).text().null(),
					)
					.to_owned(),
			)
			.await
	}

	async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.alter_table(
				TableAlterStatement::new()
					.table(Tags::Table)
					.drop_column(Tags::DisplayName)
					.to_owned(),
			)
			.await
	}
}
