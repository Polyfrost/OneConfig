use sea_orm_migration::prelude::{extension::postgres::TypeAlterStatement, *};

#[derive(DeriveIden)]
pub enum TagTypeVariants {
	#[sea_orm(iden = "tag_type")]
	Enum,
	#[sea_orm(iden = "category")]
	Category,
}

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait::async_trait]
impl MigrationTrait for Migration {
	async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.alter_type(
				TypeAlterStatement::new()
					.name(TagTypeVariants::Enum)
					.add_value(TagTypeVariants::Category),
			)
			.await
	}

	async fn down(&self, _manager: &SchemaManager) -> Result<(), DbErr> {
		Ok(())
	}
}
