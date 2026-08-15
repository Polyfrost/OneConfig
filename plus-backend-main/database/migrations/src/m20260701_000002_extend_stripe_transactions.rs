use sea_orm_migration::prelude::*;

#[derive(DeriveMigrationName)]
pub struct Migration;

#[derive(DeriveIden)]
enum Transaction {
	Table,
	ProviderTransactionId,
	StripePaymentId,
	Amount,
	DiscountRate,
}

#[async_trait::async_trait]
impl MigrationTrait for Migration {
	async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.alter_table(
				TableAlterStatement::new()
					.table(Transaction::Table)
					.rename_column(
						Transaction::ProviderTransactionId,
						Transaction::StripePaymentId,
					)
					.to_owned(),
			)
			.await?;
		manager
			.alter_table(
				TableAlterStatement::new()
					.table(Transaction::Table)
					.add_column_if_not_exists(
						ColumnDef::new(Transaction::Amount)
							.float()
							.null()
							.to_owned(),
					)
					.to_owned(),
			)
			.await?;
		manager
			.alter_table(
				TableAlterStatement::new()
					.table(Transaction::Table)
					.add_column_if_not_exists(
						ColumnDef::new(Transaction::DiscountRate)
							.integer()
							.null()
							.to_owned(),
					)
					.to_owned(),
			)
			.await
	}

	async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.alter_table(
				TableAlterStatement::new()
					.table(Transaction::Table)
					.rename_column(
						Transaction::StripePaymentId,
						Transaction::ProviderTransactionId,
					)
					.to_owned(),
			)
			.await?;
		manager
			.alter_table(
				TableAlterStatement::new()
					.table(Transaction::Table)
					.drop_column(Transaction::Amount)
					.to_owned(),
			)
			.await?;
		manager
			.alter_table(
				TableAlterStatement::new()
					.table(Transaction::Table)
					.drop_column(Transaction::Amount)
					.to_owned(),
			)
			.await
	}
}
