#!/usr/bin/env bash
set -euo pipefail

# invalid locale makes psql crash on macos
export LC_ALL="${LC_ALL:-en_US.UTF-8}"
export LANG="${LANG:-en_US.UTF-8}"

# Attempt to find project root
declare root
if hash jj 2>/dev/null; then
    root="$(jj root || true)"
elif hash git 2>/dev/null; then
    root="$(git rev-parse --show-toplevel || true)"
fi
root="$PWD"

echo Starting s3 server...

mkdir -p "$root/.local/s3"

bucket="$root/.local/s3/local"
if [ ! -d "$bucket" ]; then
    mkdir "$bucket"
    # Add some example files
    # uuid="424ef6d047744f8c8bef8f62ebdac9c0"
    # cosmetics="$(curl --silent "https://api.cosmetica.cc/v2/get/info?uuid=$uuid")"
    # jq -r .cape.image <<< "$cosmetics" | cut -d ',' -f 2 | base64 -d > "$bucket/cape.png"
    curl --output "$bucket/cape.png" http://s.optifine.net/capes/AwesomeTy79.png
fi

rclone serve s3 --addr 127.0.0.1:8081 --auth-key local,local "$root"/.local/s3 &
RCLONE_PID="$!"

trap 'kill "$RCLONE_PID" || true' EXIT

sleep 1

echo Starting postgres server...

db="$root/.local/postgresql"
if [ ! -d "$db" ]; then
    mkdir "$db"

    pg_ctl -D "$db" init
fi

pg_ctl \
    -D "$db" \
    -o "-c unix_socket_directories='$db'" \
    -o "-c log_destination='stderr'" \
    -o "-c log_connections=on" \
    start

psql -d postgres -h "$db" -c 'CREATE DATABASE local;' || true

if command -v sea-orm-cli >/dev/null 2>&1; then
    DATABASE_URL="postgresql://localhost:5432/local" sea-orm-cli migrate up \
        -d "$root/database/migrations" || true
fi

if [ -x "$root/scripts/seed-dev-assets.sh" ]; then
    "$root/scripts/seed-dev-assets.sh" || echo "warning: seed-dev-assets.sh failed" >&2
fi

if [ -f "$root/scripts/populate-db.sql" ]; then
  psql -d local -h "$db" -f "$root/scripts/populate-db.sql" || \
    echo "warning: populate-db.sql failed" >&2
fi

# Ensure processes are stopped on exit
trap 'pg_ctl -D "$db" stop; kill "$RCLONE_PID" || true' EXIT

wait
