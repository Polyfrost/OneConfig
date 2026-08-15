use sea_orm_migration::prelude::*;

use crate::m20250917_163702_create_users_table::User;

#[derive(DeriveMigrationName)]
pub struct Migration;

#[derive(DeriveIden)]
enum DailyPlaytime {
	Table,
	PlayerId,
	Day,
	TotalSeconds,
	SessionCount,
}

#[async_trait::async_trait]
impl MigrationTrait for Migration {
	async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.create_table(
				Table::create()
					.table(DailyPlaytime::Table)
					.if_not_exists()
					.col(ColumnDef::new(DailyPlaytime::PlayerId).integer().not_null())
					.col(ColumnDef::new(DailyPlaytime::Day).date().not_null())
					.col(
						ColumnDef::new(DailyPlaytime::TotalSeconds)
							.big_integer()
							.not_null()
							.default(0),
					)
					.col(
						ColumnDef::new(DailyPlaytime::SessionCount)
							.integer()
							.not_null()
							.default(0),
					)
					.foreign_key(
						ForeignKey::create()
							.from(DailyPlaytime::Table, DailyPlaytime::PlayerId)
							.to(User::Table, User::Id)
							.on_delete(ForeignKeyAction::Cascade),
					)
					.primary_key(
						Index::create()
							.col(DailyPlaytime::PlayerId)
							.col(DailyPlaytime::Day),
					)
					.to_owned(),
			)
			.await
	}

	async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.drop_table(Table::drop().table(DailyPlaytime::Table).to_owned())
			.await
	}
}
