use sea_orm_migration::prelude::*;

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait::async_trait]
impl MigrationTrait for Migration {
	async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.alter_table(
				TableAlterStatement::new()
					.table(User::Table)
					.add_column(ColumnDef::new(User::ParticleColor).integer().null())
					.to_owned(),
			)
			.await
	}

	async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.alter_table(
				TableAlterStatement::new()
					.table(User::Table)
					.drop_column(User::ParticleColor)
					.to_owned(),
			)
			.await
	}
}

#[derive(DeriveIden)]
enum User {
	Table,
	ParticleColor,
}
