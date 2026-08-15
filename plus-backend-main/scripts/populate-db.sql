--() { :; }; exec psql -d local -h "$PWD/.local/postgresql" -f "$0"

-- Wyvest (https://api.mojang.com/users/profiles/minecraft/Wyvest)
\set wyvest_uuid 'a5331404-0e77-440e-8bef-24c071dac1ae'

TRUNCATE
    player_equipped_cosmetic,
    player_owned_emote,
    player_owned_cosmetic,
    emote_package,
    cosmetic_package,
    cosmetic_allowed_slot,
    emote,
    cosmetic,
    "transaction",
    asset,
    "user"
RESTART IDENTITY CASCADE;

INSERT INTO asset (id, storage_path, asset_kind, content_type, hash)
VALUES
    (1, 'capes/oneclient.png', 'image', 'image/png', 'dev-oneclient'),
    (2, 'capes/oneconfig.png', 'image', 'image/png', 'dev-oneconfig'),
    (3, 'capes/onelauncher.png', 'image', 'image/png', 'dev-onelauncher'),
    (4, 'capes/poly.png', 'image', 'image/png', 'dev-poly'),
    (5, 'capes/moon.png', 'image', 'image/png', 'dev-moon'),
    (6, 'emotes/player.zip', 'bundle', 'application/zip', 'dev-player'),
    (7, 'emotes/wowtext.zip', 'bundle', 'application/zip', 'dev-wowtext'),
    (8, 'emotes/santaguise.zip', 'bundle', 'application/zip', 'dev-santaguise');

SELECT setval(
    pg_get_serial_sequence('asset', 'id'),
    (SELECT COALESCE(MAX(id), 1) FROM asset)
);

INSERT INTO cosmetic (id, asset_id, type, name, enabled)
VALUES
    (1, 1, 'cape', 'OneClient Cape', true),
    (2, 2, 'cape', 'OneConfig Cape', true),
    (3, 3, 'cape', 'OneLauncher Cape', true),
    (4, 4, 'cape', 'Poly Cape', true),
    (5, 5, 'cape', 'Moon Cape', true);

SELECT setval(
    pg_get_serial_sequence('cosmetic', 'id'),
    (SELECT COALESCE(MAX(id), 1) FROM cosmetic)
);

INSERT INTO emote (id, asset_id, name, enabled)
VALUES
    (1000006, 6, 'Player Emote', true),
    (1000007, 7, 'Wow Text', true),
    (1000008, 8, 'Santa Guise', true);

SELECT setval(
    pg_get_serial_sequence('emote', 'id'),
    (SELECT COALESCE(MAX(id), 1) FROM emote)
);

INSERT INTO cosmetic_allowed_slot (cosmetic_id, slot)
SELECT id, 'cape'::body_slot
FROM cosmetic
WHERE type = 'cape'
ON CONFLICT DO NOTHING;

INSERT INTO "user" (minecraft_uuid, role)
VALUES (:'wyvest_uuid'::uuid, 'admin')
ON CONFLICT (minecraft_uuid) DO NOTHING;

INSERT INTO "transaction" (id, player_id, provider, status, raw_metadata)
SELECT
    1,
    u.id,
    'admin_grant',
    'completed',
    '{"source":"local_seed"}'::jsonb
FROM "user" u
WHERE u.minecraft_uuid = :'wyvest_uuid'::uuid;

SELECT setval(
    pg_get_serial_sequence('"transaction"', 'id'),
    (SELECT COALESCE(MAX(id), 1) FROM "transaction")
);

INSERT INTO player_owned_cosmetic (player_id, cosmetic_id, acquired_via, transaction_id)
SELECT u.id, c.id, 'admin_grant', 1
FROM "user" u
CROSS JOIN cosmetic c
WHERE u.minecraft_uuid = :'wyvest_uuid'::uuid
ON CONFLICT DO NOTHING;

INSERT INTO player_owned_emote (player_id, emote_id, acquired_via, transaction_id)
SELECT u.id, e.id, 'admin_grant', 1
FROM "user" u
CROSS JOIN emote e
WHERE u.minecraft_uuid = :'wyvest_uuid'::uuid
ON CONFLICT DO NOTHING;

INSERT INTO player_equipped_cosmetic (player_id, slot, cosmetic_id)
SELECT u.id, 'cape', 1
FROM "user" u
WHERE u.minecraft_uuid = :'wyvest_uuid'::uuid
ON CONFLICT (player_id, slot) DO UPDATE SET cosmetic_id = EXCLUDED.cosmetic_id;

INSERT INTO cosmetic_package (package_id, cosmetic_id)
VALUES
    (1001, 1),
    (1002, 2),
    (1003, 3),
    (1004, 4),
    (1005, 5)
ON CONFLICT DO NOTHING;

INSERT INTO emote_package (package_id, emote_id)
VALUES
    (2001, 1000006),
    (2002, 1000007),
    (2003, 1000008)
ON CONFLICT DO NOTHING;
