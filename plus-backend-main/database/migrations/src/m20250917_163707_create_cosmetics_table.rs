use sea_orm_migration::{
	prelude::{extension::postgres::Type, *},
	sea_orm::{EnumIter, Iterable as _},
};

#[derive(DeriveIden)]
pub struct CosmeticType;

#[derive(DeriveIden, EnumIter)]
pub enum CosmeticVariants {
	Cape,
	Emote,
}

#[derive(DeriveIden)]
pub enum Cosmetic {
	Table,
	Id,
	Type,
	Path,
}

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait::async_trait]
impl MigrationTrait for Migration {
	async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.create_type(
				Type::create()
					.as_enum(CosmeticType)
					.values(CosmeticVariants::iter())
					.to_owned(),
			)
			.await?;

		manager
			.create_table(
				Table::create()
					.table(Cosmetic::Table)
					.if_not_exists()
					.col(
						ColumnDef::new(Cosmetic::Id)
							.integer()
							.auto_increment()
							.primary_key(),
					)
					.col(
						ColumnDef::new(Cosmetic::Type)
							.custom(CosmeticType)
							.not_null(),
					)
					.col(ColumnDef::new(Cosmetic::Path).string().null())
					.to_owned(),
			)
			.await?;

		Ok(())
	}

	async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.drop_table(Table::drop().table(Cosmetic::Table).to_owned())
			.await?;

		manager
			.drop_type(Type::drop().name(CosmeticType).to_owned())
			.await?;

		Ok(())
	}
}
