use sea_orm_migration::{
	prelude::{extension::postgres::Type, *},
	sea_orm::{EnumIter, Iterable as _},
};

#[derive(DeriveIden)]
pub struct TagType;

#[derive(DeriveIden, EnumIter)]
pub enum TagTypeVariants {
	Custom,
	Color,
}

#[derive(DeriveIden)]
pub enum Tags {
	Table,
	Id,
	Name,
	Description,
	TagType,
	CreatedAt,
}

#[derive(DeriveIden)]
pub enum TagsCosmetic {
	Table,
	TagId,
	CosmeticId,
}

#[derive(DeriveIden)]
pub enum Cosmetic {
	Table,
	Id,
}

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait::async_trait]
impl MigrationTrait for Migration {
	async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.create_type(
				Type::create()
					.as_enum(TagType)
					.values(TagTypeVariants::iter())
					.to_owned(),
			)
			.await?;

		manager
			.create_table(
				Table::create()
					.table(Tags::Table)
					.if_not_exists()
					.col(
						ColumnDef::new(Tags::Id)
							.integer()
							.auto_increment()
							.primary_key(),
					)
					.col(ColumnDef::new(Tags::Name).text().not_null().unique_key())
					.col(ColumnDef::new(Tags::Description).text().null())
					.col(ColumnDef::new(Tags::TagType).custom(TagType).not_null())
					.col(
						ColumnDef::new(Tags::CreatedAt)
							.timestamp_with_time_zone()
							.not_null()
							.default(Expr::current_timestamp()),
					)
					.to_owned(),
			)
			.await?;

		manager
			.create_table(
				TableCreateStatement::new()
					.table(TagsCosmetic::Table)
					.if_not_exists()
					.col(ColumnDef::new(TagsCosmetic::TagId).integer().not_null())
					.col(
						ColumnDef::new(TagsCosmetic::CosmeticId)
							.integer()
							.not_null(),
					)
					.foreign_key(
						ForeignKeyCreateStatement::new()
							.name("fk_tags_cosmetic_to_tag")
							.from_tbl(TagsCosmetic::Table)
							.from_col(TagsCosmetic::TagId)
							.to_tbl(Tags::Table)
							.to_col(Tags::Id)
							.on_delete(ForeignKeyAction::Cascade),
					)
					.foreign_key(
						ForeignKeyCreateStatement::new()
							.name("fk_tags_cosmetic_to_cosmetic")
							.from_tbl(TagsCosmetic::Table)
							.from_col(TagsCosmetic::CosmeticId)
							.to_tbl(Cosmetic::Table)
							.to_col(Cosmetic::Id)
							.on_delete(ForeignKeyAction::Cascade),
					)
					.primary_key(
						Index::create()
							.col(TagsCosmetic::TagId)
							.col(TagsCosmetic::CosmeticId),
					)
					.to_owned(),
			)
			.await
	}

	async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.drop_table(Table::drop().table(TagsCosmetic::Table).to_owned())
			.await?;
		manager
			.drop_table(Table::drop().table(Tags::Table).to_owned())
			.await?;
		manager
			.drop_type(Type::drop().name(TagType).to_owned())
			.await
	}
}
