use sea_orm_migration::prelude::*;

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait::async_trait]
impl MigrationTrait for Migration {
	async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		let db = manager.get_connection();

		db.execute_unprepared(
			r#"
			CREATE SEQUENCE IF NOT EXISTS cosmetic_entity_id_seq;

			-- Advance the shared sequence past every existing id in both tables
			-- (and past the old per-table sequences) so new rows never reuse one.
			SELECT setval('cosmetic_entity_id_seq', GREATEST(
				(SELECT COALESCE(MAX(id), 0) FROM cosmetic),
				(SELECT COALESCE(MAX(id), 0) FROM emote),
				(SELECT COALESCE(last_value, 1) FROM cosmetic_id_seq),
				(SELECT COALESCE(last_value, 1) FROM emote_id_seq),
				1
			), true);

			ALTER TABLE cosmetic ALTER COLUMN id SET DEFAULT nextval('cosmetic_entity_id_seq');
			ALTER TABLE emote    ALTER COLUMN id SET DEFAULT nextval('cosmetic_entity_id_seq');

			-- Detach the now-unused per-table sequences so TRUNCATE ... RESTART
			-- IDENTITY no longer resets them out from under the shared sequence.
			ALTER SEQUENCE cosmetic_id_seq OWNED BY NONE;
			ALTER SEQUENCE emote_id_seq OWNED BY NONE;
			"#,
		)
		.await?;

		Ok(())
	}

	async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		let db = manager.get_connection();

		db.execute_unprepared(
			r#"
			ALTER SEQUENCE cosmetic_id_seq OWNED BY cosmetic.id;
			ALTER SEQUENCE emote_id_seq OWNED BY emote.id;
			ALTER TABLE cosmetic ALTER COLUMN id SET DEFAULT nextval('cosmetic_id_seq');
			ALTER TABLE emote    ALTER COLUMN id SET DEFAULT nextval('emote_id_seq');
			DROP SEQUENCE IF EXISTS cosmetic_entity_id_seq;
			"#,
		)
		.await?;

		Ok(())
	}
}
