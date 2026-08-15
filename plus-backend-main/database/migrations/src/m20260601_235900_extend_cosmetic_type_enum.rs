use sea_orm_migration::prelude::*;

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait::async_trait]
impl MigrationTrait for Migration {
	async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		let db = manager.get_connection();

		// Must be a separate migration from usage of new values (PostgreSQL 55P04).
		for statement in [
			"ALTER TYPE cosmetic_type ADD VALUE IF NOT EXISTS 'backpack'",
			"ALTER TYPE cosmetic_type ADD VALUE IF NOT EXISTS 'glasses'",
			"ALTER TYPE cosmetic_type ADD VALUE IF NOT EXISTS 'wings'",
			"ALTER TYPE cosmetic_type ADD VALUE IF NOT EXISTS 'glove'",
		] {
			db.execute_unprepared(statement).await?;
		}

		Ok(())
	}

	async fn down(&self, _manager: &SchemaManager) -> Result<(), DbErr> {
		Ok(())
	}
}
