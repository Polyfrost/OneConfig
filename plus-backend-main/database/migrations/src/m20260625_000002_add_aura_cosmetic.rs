use sea_orm_migration::prelude::*;

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait::async_trait]
impl MigrationTrait for Migration {
	async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		let db = manager.get_connection();

		for statement in [
			"ALTER TYPE cosmetic_type ADD VALUE IF NOT EXISTS 'aura'",
			"ALTER TYPE body_slot ADD VALUE IF NOT EXISTS 'aura'",
		] {
			db.execute_unprepared(statement).await?;
		}

		Ok(())
	}

	async fn down(&self, _manager: &SchemaManager) -> Result<(), DbErr> {
		Ok(())
	}
}
