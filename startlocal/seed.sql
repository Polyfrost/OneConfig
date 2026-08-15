-- Dev fixtures for the local stack, matching the schema the migrations
-- currently produce.
--
-- This replaces plus-backend-main/scripts/populate-db.sql, which still writes to
-- the emote, player_owned_emote and emote_package tables that
-- m20260704_000004_drop_emotes removed. Emotes are ordinary cosmetic rows with
-- type = 'emote' now.
--
-- Every cosmetic and bundle here carries a base_price on purpose: the store
-- endpoints (/cosmetics/search, /bundles/search) filter on
-- `enabled = true AND base_price IS NOT NULL`, so anything unpriced is invisible
-- in the shop.
--
-- The storage_path values line up with the objects startlocal\s3.ps1 -Seed
-- writes into the bucket.

BEGIN;

TRUNCATE
    player_equipped_cosmetic,
    player_owned_cosmetic,
    cosmetic_package,
    cosmetic_allowed_slot,
    cosmetic_group_allowed_slot,
    tags_cosmetic,
    bundles_cosmetics,
    bundles,
    cosmetic,
    cosmetic_group,
    tags,
    collections,
    "transaction",
    asset,
    "user"
RESTART IDENTITY CASCADE;

-- Collections -----------------------------------------------------------------

INSERT INTO collections (id, name, description)
VALUES
    (1, 'Launch', 'The capes that shipped with Poly+.'),
    (2, 'Seasonal', 'Rotating cosmetics.');

SELECT setval(pg_get_serial_sequence('collections', 'id'), 2);

-- Tags ------------------------------------------------------------------------

INSERT INTO tags (id, name, display_name, description, tag_type)
VALUES
    (1, 'blue', 'Blue', 'Predominantly blue.', 'color'),
    (2, 'purple', 'Purple', 'Predominantly purple.', 'color'),
    (3, 'green', 'Green', 'Predominantly green.', 'color'),
    (4, 'red', 'Red', 'Predominantly red.', 'color'),
    (5, 'gold', 'Gold', 'Predominantly gold.', 'color'),
    (6, 'limited', 'Limited', 'Only around for a while.', 'custom'),
    (7, 'featured', 'Featured', 'Shown on the front page.', 'category');

SELECT setval(pg_get_serial_sequence('tags', 'id'), 7);

-- Assets ----------------------------------------------------------------------

INSERT INTO asset (id, storage_path, asset_kind, content_type, hash)
VALUES
    (1, 'capes/oneclient.png',   'image',  'image/png',       'dev-oneclient'),
    (2, 'capes/oneconfig.png',   'image',  'image/png',       'dev-oneconfig'),
    (3, 'capes/onelauncher.png', 'image',  'image/png',       'dev-onelauncher'),
    (4, 'capes/poly.png',        'image',  'image/png',       'dev-poly'),
    (5, 'capes/moon.png',        'image',  'image/png',       'dev-moon'),
    (6, 'emotes/player.zip',     'bundle', 'application/zip', 'dev-player');

SELECT setval(pg_get_serial_sequence('asset', 'id'), 6);

-- Cosmetic groups -------------------------------------------------------------
-- cosmetic and cosmetic_group share the cosmetic_entity_id_seq sequence, so the
-- group id is picked well clear of the cosmetic ids below and the sequence is
-- advanced past both at the end.

INSERT INTO cosmetic_group (id, type, name, enabled)
VALUES (100, 'cape', 'Poly Cape', true);

INSERT INTO cosmetic_group_allowed_slot (group_id, slot)
VALUES (100, 'cape');

-- Cosmetics -------------------------------------------------------------------
-- cover_asset_id points at the texture itself: the render service generates
-- proper covers on upload, but seeded rows have none, and a flat texture beats
-- an empty tile in the shop.

INSERT INTO cosmetic (
    id, type, asset_id, cover_asset_id, name, enabled, collection,
    description, base_price, discount_rate,
    group_id, variant_name, variant_order
)
VALUES
    (1, 'cape', 1, 1, 'OneClient Cape', true, 1,
     'The cape that ships with OneClient.', 4.99, NULL, NULL, NULL, 0),
    (2, 'cape', 2, 2, 'OneConfig Cape', true, 1,
     'For the people who actually read the settings menu.', 4.99, NULL, NULL, NULL, 0),
    (3, 'cape', 3, 3, 'OneLauncher Cape', true, 1,
     'Worn by anyone who has waited on a modpack download.', 4.99, 20, NULL, NULL, 0),
    -- Two variants of one buyable cape: buying either grants the whole group.
    (4, 'cape', 4, 4, 'Poly Cape', true, 2,
     'A cape in two colourways. Buy once, wear either.', 6.99, NULL, 100, 'Crimson', 0),
    (5, 'cape', 5, 5, 'Poly Cape', true, 2,
     'A cape in two colourways. Buy once, wear either.', 6.99, NULL, 100, 'Moonlight', 1),
    -- Emotes are cosmetics with a bundle asset and no body slot.
    (6, 'emote', 6, NULL, 'Wave Emote', true, 2,
     'A friendly wave.', 2.99, NULL, NULL, NULL, 0);

SELECT setval(
    pg_get_serial_sequence('cosmetic', 'id'),
    GREATEST(
        (SELECT COALESCE(MAX(id), 1) FROM cosmetic),
        (SELECT COALESCE(MAX(id), 1) FROM cosmetic_group)
    )
);

INSERT INTO cosmetic_allowed_slot (cosmetic_id, slot)
VALUES
    (1, 'cape'),
    (2, 'cape'),
    (3, 'cape'),
    (4, 'cape'),
    (5, 'cape');

INSERT INTO tags_cosmetic (tag_id, cosmetic_id)
VALUES
    (1, 1), (7, 1),
    (2, 2),
    (3, 3), (6, 3),
    (4, 4), (7, 4),
    (5, 5),
    (7, 6);

-- Bundles ---------------------------------------------------------------------

INSERT INTO bundles (id, name, description, asset_id, enabled, collection, base_price, discount_rate)
VALUES (1, 'Launch Bundle', 'All three launch capes, cheaper together.', 1, true, 1, 9.99, 25);

SELECT setval(pg_get_serial_sequence('bundles', 'id'), 1);

INSERT INTO bundles_cosmetics (bundle_id, cosmetic_id)
VALUES (1, 1), (1, 2), (1, 3);

-- Player ----------------------------------------------------------------------
-- Wyvest, https://api.mojang.com/users/profiles/minecraft/Wyvest

INSERT INTO "user" (id, minecraft_uuid, role)
VALUES (1, 'a5331404-0e77-440e-8bef-24c071dac1ae', 'admin');

SELECT setval(pg_get_serial_sequence('user', 'id'), 1);

INSERT INTO "transaction" (id, player_id, provider, status, raw_metadata, amount)
VALUES (1, 1, 'admin_grant', 'completed', '{"reason":"dev_seed"}', 0);

SELECT setval(pg_get_serial_sequence('transaction', 'id'), 1);

-- Owns one cape and the emote, so /cosmetics/player has something to return and
-- the shop still has unowned stock to show.
INSERT INTO player_owned_cosmetic (player_id, cosmetic_id, acquired_via, transaction_id)
VALUES
    (1, 1, 'admin_grant', 1),
    (1, 6, 'admin_grant', 1);

INSERT INTO player_equipped_cosmetic (player_id, slot, cosmetic_id)
VALUES (1, 'cape', 1);

COMMIT;
