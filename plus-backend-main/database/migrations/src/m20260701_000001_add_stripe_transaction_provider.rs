use sea_orm_migration::prelude::{extension::postgres::Type, *};

#[derive(DeriveMigrationName)]
pub struct Migration;

#[derive(DeriveIden)]
enum TransactionProvider {
	#[sea_orm(iden = "transaction_provider")]
	Enum,
	#[sea_orm(iden = "tebex")]
	Tebex,
	#[sea_orm(iden = "stripe")]
	Stripe,
}

#[async_trait::async_trait]
impl MigrationTrait for Migration {
	async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.alter_type(
				Type::alter().name(TransactionProvider::Enum).rename_value(
					TransactionProvider::Tebex,
					TransactionProvider::Stripe,
				),
			)
			.await
	}

	async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.alter_type(
				Type::alter().name(TransactionProvider::Enum).rename_value(
					TransactionProvider::Stripe,
					TransactionProvider::Tebex,
				),
			)
			.await
	}
}
