use sea_orm_migration::prelude::*;

#[derive(DeriveMigrationName)]
pub struct Migration;

#[derive(DeriveIden)]
enum Transaction {
	Table,
	Recipient,
	Buyer,
}

#[async_trait::async_trait]
impl MigrationTrait for Migration {
	async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.alter_table(
				TableAlterStatement::new()
					.table(Transaction::Table)
					.rename_column(Transaction::Recipient, Transaction::Buyer)
					.to_owned(),
			)
			.await
	}

	async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.alter_table(
				TableAlterStatement::new()
					.table(Transaction::Table)
					.rename_column(Transaction::Buyer, Transaction::Recipient)
					.to_owned(),
			)
			.await
	}
}
