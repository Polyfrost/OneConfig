use sea_orm_migration::prelude::*;

#[derive(DeriveMigrationName)]
pub struct Migration;

#[derive(DeriveIden)]
enum Transaction {
	Table,
	Recipient,
}
#[derive(DeriveIden)]
enum User {
	Table,
	Id,
}

#[async_trait::async_trait]
impl MigrationTrait for Migration {
	async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		manager
			.alter_table(
				TableAlterStatement::new()
					.table(Transaction::Table)
					.add_column_if_not_exists(
						ColumnDef::new(Transaction::Recipient)
							.integer()
							.null()
							.to_owned(),
					)
					.add_foreign_key(
						TableForeignKey::new()
							.name("fk_recipient_id")
							.from_tbl(Transaction::Table)
							.from_col(Transaction::Recipient)
							.to_tbl(User::Table)
							.to_col(User::Id)
							.on_delete(ForeignKeyAction::SetNull)
							.on_update(ForeignKeyAction::Cascade),
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
					.drop_foreign_key("fk_recipient_id")
					.drop_column(Transaction::Recipient)
					.to_owned(),
			)
			.await
	}
}
