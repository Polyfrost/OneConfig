use sea_orm_migration::prelude::*;

#[derive(DeriveMigrationName)]
pub struct Migration;

#[async_trait::async_trait]
impl MigrationTrait for Migration {
	async fn up(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		let db = manager.get_connection();

		db.execute_unprepared(
			r#"
			DO $$ BEGIN
				CREATE TYPE player_role AS ENUM ('player', 'moderator', 'admin');
			EXCEPTION WHEN duplicate_object THEN NULL;
			END $$;

			DO $$ BEGIN
				CREATE TYPE asset_kind AS ENUM ('image', 'bundle');
			EXCEPTION WHEN duplicate_object THEN NULL;
			END $$;

			DO $$ BEGIN
				CREATE TYPE body_slot AS ENUM ('cape', 'backpack', 'glasses', 'wings', 'left_hand', 'right_hand');
			EXCEPTION WHEN duplicate_object THEN NULL;
			END $$;

			DO $$ BEGIN
				CREATE TYPE transaction_provider AS ENUM ('tebex', 'ingame', 'admin_grant');
			EXCEPTION WHEN duplicate_object THEN NULL;
			END $$;

			DO $$ BEGIN
				CREATE TYPE transaction_status AS ENUM ('pending', 'completed', 'failed', 'refunded');
			EXCEPTION WHEN duplicate_object THEN NULL;
			END $$;

			ALTER TABLE "user"
				ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
				ADD COLUMN IF NOT EXISTS blacklisted BOOLEAN NOT NULL DEFAULT FALSE,
				ADD COLUMN IF NOT EXISTS role player_role NOT NULL DEFAULT 'player';

			CREATE TABLE IF NOT EXISTS asset (
				id SERIAL PRIMARY KEY,
				storage_path TEXT UNIQUE,
				url TEXT,
				asset_kind asset_kind NOT NULL,
				content_type TEXT,
				hash TEXT,
				created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
				updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
				CONSTRAINT asset_location_check CHECK (storage_path IS NOT NULL OR url IS NOT NULL)
			);

			INSERT INTO asset (storage_path, asset_kind, content_type, hash)
			SELECT
				path,
				CASE WHEN type = 'emote' THEN 'bundle'::asset_kind ELSE 'image'::asset_kind END,
				CASE
					WHEN path LIKE '%.zip' THEN 'application/zip'
					WHEN path LIKE '%.png' THEN 'image/png'
					ELSE NULL
				END,
				'37a6259cc0c1dae299a7866489dff0bd'
			FROM cosmetic
			WHERE path IS NOT NULL
			ON CONFLICT (storage_path) DO NOTHING;

			ALTER TABLE cosmetic
				ADD COLUMN IF NOT EXISTS asset_id INTEGER REFERENCES asset(id),
				ADD COLUMN IF NOT EXISTS name TEXT,
				ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE,
				ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
				ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

			UPDATE cosmetic
			SET
				asset_id = asset.id,
				name = COALESCE(cosmetic.name, initcap(replace(split_part(cosmetic.path, '/', 2), '.', ' ')))
			FROM asset
			WHERE cosmetic.path = asset.storage_path
				AND cosmetic.asset_id IS NULL;

			UPDATE cosmetic
			SET name = COALESCE(name, 'Cosmetic ' || id::text);

			CREATE TABLE IF NOT EXISTS emote (
				id SERIAL PRIMARY KEY,
				asset_id INTEGER REFERENCES asset(id),
				name TEXT NOT NULL,
				created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
				updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
				enabled BOOLEAN NOT NULL DEFAULT TRUE
			);

			INSERT INTO emote (id, asset_id, name, created_at, updated_at, enabled)
			SELECT id, asset_id, COALESCE(name, 'Emote ' || id::text), created_at, updated_at, enabled
			FROM cosmetic
			WHERE type = 'emote'
			ON CONFLICT (id) DO NOTHING;

			SELECT setval(pg_get_serial_sequence('emote', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM emote), 1));

			CREATE TABLE IF NOT EXISTS cosmetic_allowed_slot (
				cosmetic_id INTEGER NOT NULL REFERENCES cosmetic(id) ON DELETE CASCADE,
				slot body_slot NOT NULL,
				PRIMARY KEY (cosmetic_id, slot)
			);

			INSERT INTO cosmetic_allowed_slot (cosmetic_id, slot)
			SELECT id, 'cape'::body_slot FROM cosmetic WHERE type = 'cape'
			ON CONFLICT DO NOTHING;

			INSERT INTO cosmetic_allowed_slot (cosmetic_id, slot)
			SELECT id, 'backpack'::body_slot FROM cosmetic WHERE type::text = 'backpack'
			ON CONFLICT DO NOTHING;

			INSERT INTO cosmetic_allowed_slot (cosmetic_id, slot)
			SELECT id, 'glasses'::body_slot FROM cosmetic WHERE type::text = 'glasses'
			ON CONFLICT DO NOTHING;

			INSERT INTO cosmetic_allowed_slot (cosmetic_id, slot)
			SELECT id, 'wings'::body_slot FROM cosmetic WHERE type::text = 'wings'
			ON CONFLICT DO NOTHING;

			INSERT INTO cosmetic_allowed_slot (cosmetic_id, slot)
			SELECT id, 'left_hand'::body_slot FROM cosmetic WHERE type::text = 'glove'
			ON CONFLICT DO NOTHING;

			INSERT INTO cosmetic_allowed_slot (cosmetic_id, slot)
			SELECT id, 'right_hand'::body_slot FROM cosmetic WHERE type::text = 'glove'
			ON CONFLICT DO NOTHING;

			CREATE TABLE IF NOT EXISTS "transaction" (
				id SERIAL PRIMARY KEY,
				player_id INTEGER NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
				provider transaction_provider NOT NULL,
				provider_transaction_id TEXT,
				status transaction_status NOT NULL DEFAULT 'completed',
				created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
				raw_metadata JSONB NOT NULL DEFAULT '{}'::jsonb
			);

			CREATE UNIQUE INDEX IF NOT EXISTS transaction_provider_id_unique
				ON "transaction" (provider, provider_transaction_id)
				WHERE provider_transaction_id IS NOT NULL;

			INSERT INTO "transaction" (player_id, provider, provider_transaction_id, status, raw_metadata)
			SELECT DISTINCT "user", 'tebex'::transaction_provider, transaction_id, 'completed'::transaction_status, '{}'::jsonb
			FROM user_cosmetic
			WHERE transaction_id IS NOT NULL
			ON CONFLICT (provider, provider_transaction_id) WHERE provider_transaction_id IS NOT NULL DO NOTHING;

			CREATE TABLE IF NOT EXISTS player_owned_cosmetic (
				player_id INTEGER NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
				cosmetic_id INTEGER NOT NULL REFERENCES cosmetic(id) ON DELETE CASCADE,
				acquired_via transaction_provider NOT NULL,
				transaction_id INTEGER REFERENCES "transaction"(id) ON DELETE SET NULL,
				acquired_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
				PRIMARY KEY (player_id, cosmetic_id)
			);

			CREATE TABLE IF NOT EXISTS player_owned_emote (
				player_id INTEGER NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
				emote_id INTEGER NOT NULL REFERENCES emote(id) ON DELETE CASCADE,
				acquired_via transaction_provider NOT NULL,
				transaction_id INTEGER REFERENCES "transaction"(id) ON DELETE SET NULL,
				acquired_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
				PRIMARY KEY (player_id, emote_id)
			);

			CREATE TABLE IF NOT EXISTS player_equipped_cosmetic (
				player_id INTEGER NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
				slot body_slot NOT NULL,
				cosmetic_id INTEGER NOT NULL REFERENCES cosmetic(id) ON DELETE CASCADE,
				updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
				PRIMARY KEY (player_id, slot)
			);

			INSERT INTO player_owned_cosmetic (player_id, cosmetic_id, acquired_via, transaction_id)
			SELECT
				uc."user",
				uc.cosmetic,
				CASE WHEN uc.transaction_id IS NULL THEN 'admin_grant'::transaction_provider ELSE 'tebex'::transaction_provider END,
				t.id
			FROM user_cosmetic uc
			INNER JOIN cosmetic c ON c.id = uc.cosmetic
			LEFT JOIN "transaction" t ON t.provider = 'tebex' AND t.provider_transaction_id = uc.transaction_id
			WHERE c.type <> 'emote'
			ON CONFLICT DO NOTHING;

			INSERT INTO player_owned_emote (player_id, emote_id, acquired_via, transaction_id)
			SELECT
				uc."user",
				uc.cosmetic,
				CASE WHEN uc.transaction_id IS NULL THEN 'admin_grant'::transaction_provider ELSE 'tebex'::transaction_provider END,
				t.id
			FROM user_cosmetic uc
			INNER JOIN cosmetic c ON c.id = uc.cosmetic
			LEFT JOIN "transaction" t ON t.provider = 'tebex' AND t.provider_transaction_id = uc.transaction_id
			WHERE c.type = 'emote'
			ON CONFLICT DO NOTHING;

			INSERT INTO player_equipped_cosmetic (player_id, slot, cosmetic_id)
			SELECT uc."user", 'cape'::body_slot, uc.cosmetic
			FROM user_cosmetic uc
			INNER JOIN cosmetic c ON c.id = uc.cosmetic
			WHERE uc.active = TRUE AND c.type = 'cape'
			ON CONFLICT (player_id, slot) DO UPDATE SET cosmetic_id = EXCLUDED.cosmetic_id, updated_at = NOW();

			CREATE TABLE IF NOT EXISTS emote_package (
				package_id INTEGER NOT NULL,
				emote_id INTEGER NOT NULL REFERENCES emote(id) ON DELETE CASCADE,
				PRIMARY KEY (package_id, emote_id)
			);

			INSERT INTO emote_package (package_id, emote_id)
			SELECT cp.package_id, cp.cosmetic_id
			FROM cosmetic_package cp
			INNER JOIN cosmetic c ON c.id = cp.cosmetic_id
			WHERE c.type = 'emote'
			ON CONFLICT DO NOTHING;

			DELETE FROM cosmetic_package cp
			USING cosmetic c
			WHERE c.id = cp.cosmetic_id AND c.type = 'emote';

			DROP TABLE IF EXISTS user_cosmetic;
			DELETE FROM cosmetic WHERE type = 'emote';
			ALTER TABLE cosmetic DROP COLUMN IF EXISTS path;
			"#,
		)
		.await?;

		Ok(())
	}

	async fn down(&self, manager: &SchemaManager) -> Result<(), DbErr> {
		let db = manager.get_connection();

		db.execute_unprepared(
			r#"
			ALTER TABLE cosmetic ADD COLUMN IF NOT EXISTS path VARCHAR;

			CREATE TABLE IF NOT EXISTS user_cosmetic (
				"user" INTEGER NOT NULL REFERENCES "user"(id),
				cosmetic INTEGER NOT NULL REFERENCES cosmetic(id),
				transaction_id VARCHAR(25),
				active BOOLEAN NOT NULL DEFAULT FALSE,
				PRIMARY KEY ("user", cosmetic)
			);

			INSERT INTO user_cosmetic ("user", cosmetic, transaction_id, active)
			SELECT
				poc.player_id,
				poc.cosmetic_id,
				t.provider_transaction_id,
				pec.cosmetic_id IS NOT NULL
			FROM player_owned_cosmetic poc
			LEFT JOIN "transaction" t ON t.id = poc.transaction_id
			LEFT JOIN player_equipped_cosmetic pec
				ON pec.player_id = poc.player_id AND pec.cosmetic_id = poc.cosmetic_id;

			DROP TABLE IF EXISTS player_equipped_cosmetic;
			DROP TABLE IF EXISTS player_owned_emote;
			DROP TABLE IF EXISTS player_owned_cosmetic;
			DROP TABLE IF EXISTS emote_package;
			DROP TABLE IF EXISTS cosmetic_allowed_slot;
			DROP TABLE IF EXISTS "transaction";
			DROP TABLE IF EXISTS emote;

			ALTER TABLE cosmetic
				DROP COLUMN IF EXISTS asset_id,
				DROP COLUMN IF EXISTS name,
				DROP COLUMN IF EXISTS enabled,
				DROP COLUMN IF EXISTS created_at,
				DROP COLUMN IF EXISTS updated_at;

			DROP TABLE IF EXISTS asset;

			ALTER TABLE "user"
				DROP COLUMN IF EXISTS role,
				DROP COLUMN IF EXISTS blacklisted,
				DROP COLUMN IF EXISTS created_at;

			DROP TYPE IF EXISTS transaction_status;
			DROP TYPE IF EXISTS transaction_provider;
			DROP TYPE IF EXISTS body_slot;
			DROP TYPE IF EXISTS asset_kind;
			DROP TYPE IF EXISTS player_role;
			"#,
		)
		.await?;

		Ok(())
	}
}
