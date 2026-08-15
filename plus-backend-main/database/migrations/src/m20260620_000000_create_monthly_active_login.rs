use sea_orm_migration::prelude::*;

use crate::m20250917_163702_create_users_table::User;

#[derive(DeriveMigrationName)]
pub struct Migration;

#[derive(DeriveIden)]
enum MonthlyActiveLogin {
	Table,
	PlayerId,
	Month,
	FirstLoginAt,
	LastLoginAt,
	LoginCount,
}

#[async_trait::async_trait]
impl MigrationTrait for Migration {
	async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.create_table(
				Table::create()
					.table(MonthlyActiveLogin::Table)
					.if_not_exists()
					.col(
						ColumnDef::new(MonthlyActiveLogin::PlayerId)
							.integer()
							.not_null(),
					)
					.col(ColumnDef::new(MonthlyActiveLogin::Month).date().not_null())
					.col(
						ColumnDef::new(MonthlyActiveLogin::FirstLoginAt)
							.timestamp_with_time_zone()
							.not_null()
							.default(Expr::current_timestamp()),
					)
					.col(
						ColumnDef::new(MonthlyActiveLogin::LastLoginAt)
							.timestamp_with_time_zone()
							.not_null()
							.default(Expr::current_timestamp()),
					)
					.col(
						ColumnDef::new(MonthlyActiveLogin::LoginCount)
							.integer()
							.not_null()
							.default(1),
					)
					.foreign_key(
						ForeignKey::create()
							.from(MonthlyActiveLogin::Table, MonthlyActiveLogin::PlayerId)
							.to(User::Table, User::Id)
							.on_delete(ForeignKeyAction::Cascade),
					)
					.primary_key(
						Index::create()
							.col(MonthlyActiveLogin::PlayerId)
							.col(MonthlyActiveLogin::Month),
					)
					.to_owned(),
			)
			.await
	}

	async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.drop_table(Table::drop().table(MonthlyActiveLogin::Table).to_owned())
			.await
	}
}
