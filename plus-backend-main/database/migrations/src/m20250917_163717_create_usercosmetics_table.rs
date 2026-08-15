use sea_orm_migration::prelude::*;

use crate::{
	m20250917_163702_create_users_table::User,
	m20250917_163707_create_cosmetics_table::Cosmetic,
};

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait::async_trait]
impl MigrationTrait for Migration {
	async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.create_table(
				Table::create()
					.table(UserCosmetic::Table)
					.if_not_exists()
					.col(ColumnDef::new(UserCosmetic::User).integer().not_null())
					.col(ColumnDef::new(UserCosmetic::Cosmetic).integer().not_null())
					.col(
						ColumnDef::new(UserCosmetic::TransactionId)
							.string_len(25)
							.null()
							.default(Keyword::Null),
					)
					.foreign_key(
						ForeignKey::create()
							.from_col(UserCosmetic::User)
							.to(User::Table, User::Id),
					)
					.foreign_key(
						ForeignKey::create()
							.from_col(UserCosmetic::Cosmetic)
							.to(Cosmetic::Table, Cosmetic::Id),
					)
					.primary_key(
						Index::create()
							.col(UserCosmetic::User)
							.col(UserCosmetic::Cosmetic),
					)
					.to_owned(),
			)
			.await?;

		Ok(())
	}

	async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.drop_table(Table::drop().table(UserCosmetic::Table).to_owned())
			.await?;

		Ok(())
	}
}

#[derive(DeriveIden)]
enum UserCosmetic {
	Table,
	User,
	Cosmetic,
	TransactionId,
}
