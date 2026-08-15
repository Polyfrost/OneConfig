use sea_orm_migration::prelude::*;

use crate::m20250917_163707_create_cosmetics_table::Cosmetic;

#[derive(DeriveMigrationName)]
pub struct Migration;

#[derive(DeriveIden)]
enum CosmeticPackage {
	Table,
	PackageId,
	CosmeticId,
}

#[async_trait::async_trait]
impl MigrationTrait for Migration {
	async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.create_table(
				Table::create()
					.table(CosmeticPackage::Table)
					.if_not_exists()
					.col(
						ColumnDef::new(CosmeticPackage::PackageId)
							.integer()
							.not_null(),
					)
					.col(
						ColumnDef::new(CosmeticPackage::CosmeticId)
							.integer()
							.not_null(),
					)
					.foreign_key(
						ForeignKey::create()
							.from_col(CosmeticPackage::CosmeticId)
							.to(Cosmetic::Table, Cosmetic::Id),
					)
					.primary_key(
						Index::create()
							.col(CosmeticPackage::PackageId)
							.col(CosmeticPackage::CosmeticId),
					)
					.to_owned(),
			)
			.await?;

		Ok(())
	}

	async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.drop_table(Table::drop().table(CosmeticPackage::Table).to_owned())
			.await?;

		Ok(())
	}
}
