#!/usr/bin/env bash
# Starts PostgreSQL, S3 (rclone), seeds data, and runs plus-backend for local PolyPlus testing.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root"

export PATH="/opt/homebrew/opt/postgresql@17/bin:${PATH:-}"
# invalid locale makes postgres crash on macos (postmaster became multithreaded)
export LC_ALL="${LC_ALL:-en_US.UTF-8}"
export LANG="${LANG:-en_US.UTF-8}"

db="$root/.local/postgresql"
bucket="$root/.local/s3/local"

if ! command -v rclone >/dev/null 2>&1; then
	echo "error: install rclone (brew install rclone)" >&2
	exit 1
fi

if ! command -v pg_ctl >/dev/null 2>&1; then
	echo "error: install PostgreSQL 17 (brew install postgresql@17)" >&2
	exit 1
fi

mkdir -p "$bucket/capes" "$bucket/emotes"

if [ ! -f "$db/PG_VERSION" ]; then
	initdb -D "$db" --locale=en_US.UTF-8 -E UTF-8
fi

if ! pg_ctl -D "$db" -o "-c unix_socket_directories='$db'" -o "-c port=5432" status >/dev/null 2>&1; then
	pg_ctl -D "$db" -o "-c unix_socket_directories='$db'" -o "-c port=5432" start
	sleep 2
fi

psql -d postgres -h localhost -p 5432 -c 'CREATE DATABASE local;' 2>/dev/null || true

if ! curl -sf -o /dev/null "http://127.0.0.1:8081/" 2>/dev/null; then
	echo "Starting S3 (rclone) on :8081..."
	rclone serve s3 --addr 127.0.0.1:8081 --auth-key local,local "$root/.local/s3" &
	echo $! >"$root/.local/rclone.pid"
	sleep 1
fi

DATABASE_URL="postgresql://localhost:5432/local" sea-orm-cli migrate up -d "$root/database/migrations"
"$root/scripts/seed-dev-assets.sh"
psql -d local -h localhost -p 5432 -f "$root/scripts/populate-db.sql"

if [ ! -f "$root/.env" ]; then
	cp "$root/.env.example" "$root/.env"
	echo "ADMIN_PASSWORD=dev" >>"$root/.env"
fi

echo ""
echo "Dev stack ready. Starting API on http://127.0.0.1:8080"
echo "Wyvest UUID: a5331404-0e77-440e-8bef-24c071dac1ae"
echo "PolyPlus: set apiUrl to http://127.0.0.1:8080"
echo "WebSocket: use SubscribePlayers / UnsubscribePlayers (no lobbies)"
echo "OpenAPI: http://127.0.0.1:8080/scalar"
echo ""

set -a
# shellcheck disable=SC1091
source "$root/.env"
set +a

# Start the skinview3d render sidecar that generates cosmetic cover images.
render_dir="$root/render-service"
if command -v node >/dev/null 2>&1; then
	if [ ! -d "$render_dir/node_modules" ]; then
		echo "Installing render-service dependencies..."
		(cd "$render_dir" && npm install)
	fi
	if [ ! -f "$render_dir/assets/default-skin.png" ]; then
		(cd "$render_dir" && node scripts/fetch-default-skin.mjs)
	fi
	if ! curl -sf -o /dev/null "http://127.0.0.1:8090/" 2>/dev/null; then
		echo "Starting render-service on :8090..."
		(cd "$render_dir" && PORT=8090 node src/server.js) &
		echo $! >"$root/.local/render-service.pid"
	fi
	export RENDER_SERVICE_URL="http://127.0.0.1:8090"
else
	echo "warning: node not found; cosmetic cover generation disabled" >&2
	export RENDER_SERVICE_URL=""
fi

exec cargo run -- serve
