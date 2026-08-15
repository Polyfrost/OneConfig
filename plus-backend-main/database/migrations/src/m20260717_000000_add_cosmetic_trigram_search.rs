use sea_orm_migration::prelude::*;

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait::async_trait]
impl MigrationTrait for Migration {
	async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		let db = manager.get_connection();

		// Trigram matching powers the fuzzy `text` filter and relevance ranking
		// in /cosmetics/search. The GIN indexes let `word_similarity` / ILIKE
		// use an index instead of scanning every row.
		db.execute_unprepared(
			r#"
			CREATE EXTENSION IF NOT EXISTS pg_trgm;

			CREATE INDEX IF NOT EXISTS cosmetic_name_trgm_idx
				ON cosmetic USING gin (name gin_trgm_ops);

			CREATE INDEX IF NOT EXISTS cosmetic_group_name_trgm_idx
				ON cosmetic_group USING gin (name gin_trgm_ops);
			"#,
		)
		.await?;

		Ok(())
	}

	async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		let db = manager.get_connection();

		db.execute_unprepared(
			r#"
			DROP INDEX IF EXISTS cosmetic_group_name_trgm_idx;
			DROP INDEX IF EXISTS cosmetic_name_trgm_idx;
			DROP EXTENSION IF EXISTS pg_trgm;
			"#,
		)
		.await?;

		Ok(())
	}
}
