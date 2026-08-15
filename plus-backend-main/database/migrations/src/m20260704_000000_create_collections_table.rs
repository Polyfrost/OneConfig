use sea_orm_migration::prelude::*;

#[derive(DeriveIden)]
pub enum Collections {
	Table,
	Id,
	Name,
	Description,
	CreatedAt,
}

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait::async_trait]
impl MigrationTrait for Migration {
	async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.create_table(
				Table::create()
					.table(Collections::Table)
					.if_not_exists()
					.col(
						ColumnDef::new(Collections::Id)
							.integer()
							.auto_increment()
							.primary_key(),
					)
					.col(ColumnDef::new(Collections::Name).text().not_null())
					.col(ColumnDef::new(Collections::Description).text().null())
					.col(
						ColumnDef::new(Collections::CreatedAt)
							.timestamp_with_time_zone()
							.not_null()
							.default(Expr::current_timestamp()),
					)
					.to_owned(),
			)
			.await
	}

	async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.drop_table(Table::drop().table(Collections::Table).to_owned())
			.await
	}
}
