# EndlessClient — deployment on Linux

Complete, copy-pasteable deployment of the Poly+ stack on a single Debian 12 or
Ubuntu 24.04 server, serving everything from **endlessclient.dev**.

Every command is meant to be run as-is. Replace only the values marked
`CHANGEME`.

For a Windows development machine, see [`startlocal/README.md`](startlocal/README.md).

---

## Contents

1. [Architecture](#1-architecture)
2. [DNS](#2-dns)
3. [Server preparation](#3-server-preparation)
4. [PostgreSQL](#4-postgresql)
5. [Object storage (MinIO)](#5-object-storage-minio)
6. [Build the backend](#6-build-the-backend)
7. [Render service](#7-render-service)
8. [Backend service](#8-backend-service)
9. [Shop](#9-shop)
10. [Admin dashboard](#10-admin-dashboard)
11. [nginx and TLS](#11-nginx-and-tls)
12. [Stripe](#12-stripe)
13. [First admin](#13-first-admin)
14. [Verify](#14-verify)
15. [The Minecraft client](#15-the-minecraft-client)
16. [Upgrades](#16-upgrades)
17. [Backups](#17-backups)
18. [Logs](#18-logs)
19. [Troubleshooting](#19-troubleshooting)
20. [Security checklist](#20-security-checklist)
21. [Known gaps](#21-known-gaps)

---

## 1. Architecture

| Host | Serves | Backed by | Local port |
| --- | --- | --- | --- |
| `endlessclient.dev` | Shop | `plus-website` (Next.js) | 3000 |
| `api.endlessclient.dev` | API + WebSocket | `plus-backend` (Rust) | 8080 |
| `admin.endlessclient.dev` | Admin dashboard | `plus-admin-dashboard` (static) | — |
| `cdn.endlessclient.dev` | Cosmetic assets | MinIO | 9000 |

Internal only, never exposed:

| Service | Port |
| --- | --- |
| PostgreSQL | 5432 |
| Render service | 8090 |
| MinIO console | 9001 |

```
 Minecraft ── PolyPlus ─────────┐
                                │  https + wss
 Browser ── endlessclient.dev ──┼──► api.endlessclient.dev ──► PostgreSQL
        └── admin.…             │            │
                                │            ├──► MinIO  (cdn.endlessclient.dev)
                                │            ├──► render-service :8090
                                │            └──► Stripe
                                │                      ▲
                                └──────────────────────┘ webhooks
```

**`cdn.endlessclient.dev` must be publicly reachable.** The API hands clients
presigned URLs built from the S3 endpoint it was configured with. If that
endpoint is `localhost`, every cosmetic image 404s in browsers and in game.

---

## 2. DNS

Records pointing at the server (add AAAA too if you have IPv6):

```
endlessclient.dev.        A   203.0.113.10     # CHANGEME
www.endlessclient.dev.    A   203.0.113.10
api.endlessclient.dev.    A   203.0.113.10
admin.endlessclient.dev.  A   203.0.113.10
cdn.endlessclient.dev.    A   203.0.113.10
```

Confirm before continuing — certbot will fail otherwise:

```bash
for h in endlessclient.dev www.endlessclient.dev api.endlessclient.dev \
         admin.endlessclient.dev cdn.endlessclient.dev; do
  printf '%-28s %s\n' "$h" "$(dig +short "$h" | tr '\n' ' ')"
done
```

---

## 3. Server preparation

```bash
sudo apt update && sudo apt -y upgrade
sudo apt -y install \
  build-essential pkg-config libssl-dev git curl ca-certificates \
  nginx ufw unzip jq postgresql postgresql-contrib
```

### Service user and layout

```bash
sudo useradd --system --create-home --home-dir /opt/endless --shell /usr/sbin/nologin endless
sudo mkdir -p /opt/endless/{src,bin,web,secrets}
sudo chown -R endless:endless /opt/endless
sudo chmod 750 /opt/endless/secrets
```

### Firewall

Only SSH and HTTPS. Everything else is reached through nginx on loopback.

```bash
sudo ufw allow OpenSSH
sudo ufw allow 'Nginx Full'
sudo ufw --force enable
sudo ufw status verbose
```

### Source

```bash
sudo -u endless git clone https://github.com/Th3DarkSand8tch/AstralClient.git /opt/endless/src
```

---

## 4. PostgreSQL

```bash
sudo -u postgres psql <<'SQL'
CREATE ROLE endless LOGIN PASSWORD 'CHANGEME_DB_PASSWORD';
CREATE DATABASE endless_plus OWNER endless;
SQL
```

The API installs the `pg_trgm` extension during migration, which requires
superuser. Install it once up front so the application role does not need that
privilege:

```bash
sudo -u postgres psql -d endless_plus -c 'CREATE EXTENSION IF NOT EXISTS pg_trgm;'
sudo -u postgres psql -d endless_plus -c '\dx'
```

Keep Postgres on loopback (the Debian default):

```bash
sudo -u postgres psql -c "SHOW listen_addresses;"   # expect: localhost
```

---

## 5. Object storage (MinIO)

Any S3-compatible store works. MinIO is the simplest to self-host.

```bash
curl -fsSLo /tmp/minio.deb https://dl.min.io/server/minio/release/linux-amd64/minio.deb
sudo dpkg -i /tmp/minio.deb

curl -fsSLo /tmp/mc https://dl.min.io/client/mc/release/linux-amd64/mc
sudo install -m 0755 /tmp/mc /usr/local/bin/mc

sudo useradd --system --home-dir /var/lib/minio --shell /usr/sbin/nologin minio-user || true
sudo mkdir -p /var/lib/minio
sudo chown -R minio-user:minio-user /var/lib/minio
```

```bash
sudo tee /etc/default/minio >/dev/null <<'EOF'
MINIO_VOLUMES="/var/lib/minio"
MINIO_OPTS="--address 127.0.0.1:9000 --console-address 127.0.0.1:9001"
MINIO_ROOT_USER=endless-root
MINIO_ROOT_PASSWORD=CHANGEME_MINIO_ROOT_PASSWORD
EOF
sudo chmod 600 /etc/default/minio

sudo systemctl enable --now minio
sudo systemctl status minio --no-pager
```

### Bucket and application credentials

```bash
mc alias set local http://127.0.0.1:9000 endless-root CHANGEME_MINIO_ROOT_PASSWORD

mc mb --ignore-existing local/endless-cosmetics

# A dedicated key for the API, rather than reusing the root credentials
mc admin user add local endless-api CHANGEME_S3_SECRET
mc admin policy attach local readwrite --user endless-api
```

Cosmetic assets are fetched by browsers and by the game through presigned URLs,
so the objects are read over a public hostname but stay private — the signature
is what authorises each read. Do **not** make the bucket anonymous.

Verify:

```bash
mc ls local/endless-cosmetics
```

---

## 6. Build the backend

The workspace pins Rust 1.92.0 (`rust-toolchain.toml`), so rustup selects it
automatically.

```bash
sudo -u endless bash -lc '
  curl --proto "=https" --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y --no-modify-path
  source $HOME/.cargo/env
  cd /opt/endless/src/plus-backend-main
  cargo build --release
'
```

That takes a while on first run. Install the binary:

```bash
sudo install -o endless -g endless -m 0755 \
  /opt/endless/src/plus-backend-main/target/release/plus-backend \
  /opt/endless/bin/plus-backend

/opt/endless/bin/plus-backend --version
```

<details>
<summary>Alternative: build with Nix</summary>

```bash
cd /opt/endless/src/plus-backend-main
nix build .#plus-backend .#render-service
```

This is what CI does (`.github/workflows/push.yaml`).
</details>

---

## 7. Render service

Generates cosmetic cover images with a headless browser. Optional — without it
uploads still work and simply record no cover.

```bash
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
sudo apt -y install nodejs chromium

node --version        # expect v22.x
which chromium        # /usr/bin/chromium
```

On Ubuntu the package may be `chromium-browser` and install as a snap, which
behaves poorly headless under systemd. Prefer the Debian-style package, and note
the binary path if you fall back to the snap:

```bash
sudo apt -y install chromium || sudo snap install chromium
```

Install dependencies without downloading a second browser:

```bash
sudo -u endless bash -lc '
  cd /opt/endless/src/plus-backend-main/render-service
  PUPPETEER_SKIP_DOWNLOAD=true npm ci
  node scripts/fetch-default-skin.mjs
'
```

```ini
# /etc/systemd/system/endless-render.service
[Unit]
Description=EndlessClient cosmetic render service
After=network-online.target
Wants=network-online.target

[Service]
User=endless
WorkingDirectory=/opt/endless/src/plus-backend-main/render-service
Environment=PORT=8090
Environment=PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium
ExecStart=/usr/bin/node src/server.js
Restart=on-failure
RestartSec=5s

NoNewPrivileges=true
ProtectSystem=strict
ProtectHome=true
PrivateTmp=true
ReadWritePaths=/opt/endless/src/plus-backend-main/render-service

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now endless-render
curl -fsS http://127.0.0.1:8090/ && echo    # expect: ok
```

---

## 8. Backend service

### Environment file

```bash
sudo tee /opt/endless/secrets/backend.env >/dev/null <<'EOF'
RUST_LOG=info,sea_orm=warn,sqlx=warn

BIND_ADDR=127.0.0.1:8080

DATABASE_URL=postgresql://endless:CHANGEME_DB_PASSWORD@127.0.0.1:5432/endless_plus

# Presigned URLs are built from this, so it must be the public hostname
S3_BUCKET_NAME=endless-cosmetics
S3_BUCKET_REGION=us-east-1
S3_BUCKET_ENDPOINT=https://cdn.endlessclient.dev
AWS_ACCESS_KEY_ID=endless-api
AWS_SECRET_ACCESS_KEY=CHANGEME_S3_SECRET

ADMIN_PASSWORD=CHANGEME_LONG_RANDOM_ADMIN_PASSWORD

STRIPE_SECRET=sk_test_CHANGEME
STRIPE_WEBHOOK_SECRET=whsec_CHANGEME
STRIPE_SUCCESS_URL=https://endlessclient.dev/checkout/success
STRIPE_CANCEL_URL=https://endlessclient.dev/checkout/cancel

RENDER_SERVICE_URL=http://127.0.0.1:8090

CORS_ORIGINS=https://endlessclient.dev,https://www.endlessclient.dev,https://admin.endlessclient.dev

# The API only ever sees nginx, so trust the address nginx forwards
CLIENT_IP_SOURCE=RightmostXForwardedFor
EOF

sudo chown root:endless /opt/endless/secrets/backend.env
sudo chmod 640 /opt/endless/secrets/backend.env
```

Generate a real admin password:

```bash
openssl rand -base64 36
```

Every `STRIPE_*` variable is **required and has no default**. The process refuses
to start if one is missing — an empty value is fine, an absent one is not.

### Unit

```ini
# /etc/systemd/system/endless-backend.service
[Unit]
Description=EndlessClient Poly+ API
After=network-online.target postgresql.service minio.service
Wants=network-online.target
Requires=postgresql.service

[Service]
User=endless
EnvironmentFile=/opt/endless/secrets/backend.env
ExecStart=/opt/endless/bin/plus-backend serve
Restart=on-failure
RestartSec=5s

NoNewPrivileges=true
ProtectSystem=strict
ProtectHome=true
PrivateTmp=true
ProtectKernelTunables=true
ProtectControlGroups=true
RestrictAddressFamilies=AF_INET AF_INET6

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now endless-backend
sudo journalctl -u endless-backend -n 40 --no-pager
```

Migrations run automatically at startup, before the socket binds. The log ends
with `Server listening on 127.0.0.1:8080`.

```bash
curl -fsS http://127.0.0.1:8080/cosmetics | jq
```

---

## 9. Shop

`NEXT_PUBLIC_BACKEND_URL` is inlined at **build** time, so it must be set before
building — changing it later requires a rebuild.

```bash
sudo -u endless bash -lc '
  cd /opt/endless/src/plus-website
  echo "NEXT_PUBLIC_BACKEND_URL=https://api.endlessclient.dev" > .env.local
  npm ci
  npm run build
'
```

```ini
# /etc/systemd/system/endless-shop.service
[Unit]
Description=EndlessClient shop
After=network-online.target
Wants=network-online.target

[Service]
User=endless
WorkingDirectory=/opt/endless/src/plus-website
Environment=NODE_ENV=production
Environment=PORT=3000
Environment=HOSTNAME=127.0.0.1
ExecStart=/usr/bin/npm run start
Restart=on-failure
RestartSec=5s

NoNewPrivileges=true
ProtectSystem=strict
ProtectHome=true
PrivateTmp=true
ReadWritePaths=/opt/endless/src/plus-website/.next

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now endless-shop
curl -fsS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:3000/
```

---

## 10. Admin dashboard

A static bundle — no service, nginx serves the files.

```bash
sudo -u endless bash -lc '
  cd /opt/endless/src/plus-admin-dashboard
  npm ci
  npm run build
'
sudo rm -rf /opt/endless/web/admin
sudo -u endless cp -r /opt/endless/src/plus-admin-dashboard/dist /opt/endless/web/admin
```

The dashboard is a full admin console. Its password field guards the *API*, not
the page, so put HTTP auth in front of the site as well:

```bash
sudo apt -y install apache2-utils
sudo htpasswd -c /etc/nginx/endless-admin.htpasswd youruser
sudo chmod 640 /etc/nginx/endless-admin.htpasswd
sudo chown root:www-data /etc/nginx/endless-admin.htpasswd
```

---

## 11. nginx and TLS

Get certificates first, over plain HTTP:

```bash
sudo apt -y install certbot python3-certbot-nginx
sudo certbot certonly --nginx \
  -d endlessclient.dev -d www.endlessclient.dev \
  -d api.endlessclient.dev -d admin.endlessclient.dev -d cdn.endlessclient.dev \
  --agree-tos -m admin@endlessclient.dev --no-eff-email
```

The WebSocket upgrade map, once, at the http level:

```bash
sudo tee /etc/nginx/conf.d/upgrade-map.conf >/dev/null <<'EOF'
map $http_upgrade $connection_upgrade {
    default upgrade;
    ''      close;
}
EOF
```

```bash
sudo tee /etc/nginx/sites-available/endlessclient >/dev/null <<'EOF'
# ─── redirect http → https ────────────────────────────────────────────────
server {
    listen 80;
    listen [::]:80;
    server_name endlessclient.dev www.endlessclient.dev api.endlessclient.dev
                admin.endlessclient.dev cdn.endlessclient.dev;
    location /.well-known/acme-challenge/ { root /var/www/html; }
    location / { return 301 https://$host$request_uri; }
}

# ─── API ──────────────────────────────────────────────────────────────────
server {
    listen 443 ssl;
    listen [::]:443 ssl;
    http2 on;
    server_name api.endlessclient.dev;

    ssl_certificate     /etc/letsencrypt/live/endlessclient.dev/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/endlessclient.dev/privkey.pem;

    # Cosmetic bundles are uploaded through this
    client_max_body_size 64m;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Overwrite rather than append: CLIENT_IP_SOURCE trusts the rightmost
        # value, so a client-supplied header must not survive
        proxy_set_header X-Forwarded-For   $remote_addr;

        # /websocket carries live equipment updates and playtime accounting
        proxy_http_version 1.1;
        proxy_set_header Upgrade    $http_upgrade;
        proxy_set_header Connection $connection_upgrade;
        proxy_read_timeout 3600s;
        proxy_send_timeout 3600s;
    }
}

# ─── shop ─────────────────────────────────────────────────────────────────
server {
    listen 443 ssl;
    listen [::]:443 ssl;
    http2 on;
    server_name endlessclient.dev;

    ssl_certificate     /etc/letsencrypt/live/endlessclient.dev/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/endlessclient.dev/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

server {
    listen 443 ssl;
    listen [::]:443 ssl;
    http2 on;
    server_name www.endlessclient.dev;
    ssl_certificate     /etc/letsencrypt/live/endlessclient.dev/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/endlessclient.dev/privkey.pem;
    return 301 https://endlessclient.dev$request_uri;
}

# ─── admin dashboard ──────────────────────────────────────────────────────
server {
    listen 443 ssl;
    listen [::]:443 ssl;
    http2 on;
    server_name admin.endlessclient.dev;

    ssl_certificate     /etc/letsencrypt/live/endlessclient.dev/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/endlessclient.dev/privkey.pem;

    auth_basic           "EndlessClient admin";
    auth_basic_user_file /etc/nginx/endless-admin.htpasswd;

    root /opt/endless/web/admin;
    index index.html;
    location / { try_files $uri $uri/ /index.html; }
}

# ─── object storage ───────────────────────────────────────────────────────
server {
    listen 443 ssl;
    listen [::]:443 ssl;
    http2 on;
    server_name cdn.endlessclient.dev;

    ssl_certificate     /etc/letsencrypt/live/endlessclient.dev/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/endlessclient.dev/privkey.pem;

    client_max_body_size 64m;

    location / {
        proxy_pass http://127.0.0.1:9000;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Signatures cover the request as sent; buffering rewrites can break them
        proxy_request_buffering off;
        proxy_buffering off;
    }
}
EOF

sudo ln -sf /etc/nginx/sites-available/endlessclient /etc/nginx/sites-enabled/endlessclient
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
```

Renewal is installed by the certbot package; confirm it:

```bash
sudo systemctl list-timers | grep certbot
sudo certbot renew --dry-run
```

---

## 12. Stripe

Cosmetics are Stripe products: creating one provisions a product and a price, so
**the API cannot add cosmetics without a working secret key**. Use a test key
until you are ready to charge real money.

### Webhook

In the Stripe dashboard, add an endpoint at:

```
https://api.endlessclient.dev/stripe/webhook
```

Subscribe to exactly these three events — every other event is a no-op:

```
checkout.session.completed
checkout.session.async_payment_succeeded
charge.refunded
```

Copy the endpoint's signing secret into `STRIPE_WEBHOOK_SECRET` and restart:

```bash
sudo systemctl restart endless-backend
```

A wrong secret fails in the worst way: payments succeed and cosmetics are never
granted. Send a test event and watch for it:

```bash
sudo journalctl -u endless-backend -f | grep -i webhook
```

---

## 13. First admin

Roles are `player`, `moderator`, `admin`. Grant yourself admin by Minecraft UUID
(dashes included):

```bash
sudo -u postgres psql -d endless_plus <<'SQL'
INSERT INTO "user" (minecraft_uuid, role)
VALUES ('CHANGEME-YOUR-MINECRAFT-UUID', 'admin')
ON CONFLICT (minecraft_uuid) DO UPDATE SET role = 'admin';
SQL
```

Your UUID:

```bash
curl -s https://api.mojang.com/users/profiles/minecraft/YOURNAME | jq -r .id
```

---

## 14. Verify

```bash
# Services
systemctl is-active endless-backend endless-render endless-shop minio postgresql nginx

# API through TLS
curl -fsS https://api.endlessclient.dev/openapi.json | jq '.paths | keys | length'
curl -fsS https://api.endlessclient.dev/cosmetics    | jq '.cosmetics | length'

# Admin auth: expect 401 without the header, 404 with it (route reached)
curl -s -o /dev/null -w '%{http_code}\n' -X POST \
  https://api.endlessclient.dev/cosmetics/manage/delete \
  -H 'Content-Type: application/json' -d '{"cosmetic_id":999999}'
curl -s -o /dev/null -w '%{http_code}\n' -X POST \
  https://api.endlessclient.dev/cosmetics/manage/delete \
  -H "Authorization: CHANGEME_LONG_RANDOM_ADMIN_PASSWORD" \
  -H 'Content-Type: application/json' -d '{"cosmetic_id":999999}'

# Front ends
curl -fsS -o /dev/null -w 'shop  %{http_code}\n'  https://endlessclient.dev/
curl -fsS -o /dev/null -w 'admin %{http_code}\n' -u youruser:pass https://admin.endlessclient.dev/

# WebSocket upgrade — expect 101 or 426, never 200
curl -s -o /dev/null -w '%{http_code}\n' \
  -H 'Connection: Upgrade' -H 'Upgrade: websocket' \
  -H 'Sec-WebSocket-Version: 13' -H 'Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==' \
  https://api.endlessclient.dev/websocket
```

Browsable API documentation: <https://api.endlessclient.dev/scalar>.

---

## 15. The Minecraft client

Two distinct mods:

- **OneConfig** (`modules/`, `minecraft/`) — the settings/HUD framework. It
  contains **no cosmetics code**, so rebuilding it never changes the in-game
  store.
- **PolyPlus** (`PolyPlus/`) — the cosmetics mod: store, wardrobe, emotes, and
  the connection to this API.

PolyPlus chooses its backend from a compiled-in enum:

```kotlin
// PolyPlus/src/main/kotlin/org/polyfrost/polyplus/BackendUrl.kt
enum class BackendUrl(val url: String) {
    PRODUCTION("https://plus.polyfrost.org"),
    STAGING("https://plus-staging.polyfrost.org"),
    LOCAL("http://localhost:8080");
}
```

**None of those is your server.** To ship a client that talks to
`api.endlessclient.dev` you must edit that file and build PolyPlus:

```kotlin
PRODUCTION("https://api.endlessclient.dev"),
```

Which value is used is an in-game dropdown ("API URL"), persisted per instance in
`config/polyplus.json`:

```json
"apiUrl": { "class": "org.polyfrost.polyplus.BackendUrl", "value": "PRODUCTION" }
```

PolyPlus pins the OneConfig version it builds against in
`PolyPlus/stonecutter.properties.toml` (`deps.oneconfig`). Building it against
the OneConfig in this repository requires that version to match, and the two are
not currently API-compatible — see [Known gaps](#21-known-gaps).

---

## 16. Upgrades

```bash
cd /opt/endless/src
sudo -u endless git pull

# Backend
sudo -u endless bash -lc 'source $HOME/.cargo/env && cd /opt/endless/src/plus-backend-main && cargo build --release'
sudo systemctl stop endless-backend
sudo install -o endless -g endless -m 0755 \
  /opt/endless/src/plus-backend-main/target/release/plus-backend /opt/endless/bin/plus-backend
sudo systemctl start endless-backend

# Shop
sudo -u endless bash -lc 'cd /opt/endless/src/plus-website && npm ci && npm run build'
sudo systemctl restart endless-shop

# Dashboard
sudo -u endless bash -lc 'cd /opt/endless/src/plus-admin-dashboard && npm ci && npm run build'
sudo rm -rf /opt/endless/web/admin
sudo -u endless cp -r /opt/endless/src/plus-admin-dashboard/dist /opt/endless/web/admin
```

Migrations apply on startup, so **never run two backend instances against one
database during a deploy**. Stop, replace, start.

Take a database dump before upgrading — migrations are not reversible in place.

---

## 17. Backups

Back up the database **and** the bucket together. Cosmetic rows reference object
keys; restoring one without the other leaves cosmetics whose assets 404, and the
API will not fail — it logs a warning per asset at startup and carries on.

Stripe products and prices live in Stripe and are in neither backup.

```bash
sudo mkdir -p /var/backups/endless
sudo tee /usr/local/bin/endless-backup >/dev/null <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
dest="/var/backups/endless"

sudo -u postgres pg_dump -Fc endless_plus > "$dest/endless_plus-$stamp.dump"
mc mirror --overwrite --remove local/endless-cosmetics "$dest/bucket"

find "$dest" -name 'endless_plus-*.dump' -mtime +14 -delete
EOF
sudo chmod 0755 /usr/local/bin/endless-backup
```

```ini
# /etc/systemd/system/endless-backup.service
[Unit]
Description=EndlessClient backup

[Service]
Type=oneshot
ExecStart=/usr/local/bin/endless-backup
```

```ini
# /etc/systemd/system/endless-backup.timer
[Unit]
Description=Nightly EndlessClient backup

[Timer]
OnCalendar=daily
Persistent=true

[Install]
WantedBy=timers.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now endless-backup.timer
sudo systemctl start endless-backup.service   # test it once
ls -lh /var/backups/endless
```

Copy `/var/backups/endless` off the machine — a backup on the same disk is not a
backup.

Restore:

```bash
sudo systemctl stop endless-backend
sudo -u postgres dropdb endless_plus
sudo -u postgres createdb -O endless endless_plus
sudo -u postgres psql -d endless_plus -c 'CREATE EXTENSION IF NOT EXISTS pg_trgm;'
sudo -u postgres pg_restore -d endless_plus /var/backups/endless/endless_plus-STAMP.dump
mc mirror --overwrite /var/backups/endless/bucket local/endless-cosmetics
sudo systemctl start endless-backend
```

---

## 18. Logs

Everything goes to journald:

```bash
sudo journalctl -u endless-backend -f
sudo journalctl -u endless-render  -n 100 --no-pager
sudo journalctl -u endless-shop    --since '1 hour ago'
```

`RUST_LOG=debug` prints every SQL statement — useful briefly, far too much
otherwise. Production wants:

```
RUST_LOG=info,sea_orm=warn,sqlx=warn
```

Cap the journal so it cannot fill the disk:

```bash
sudo sed -i 's/^#\?SystemMaxUse=.*/SystemMaxUse=2G/' /etc/systemd/journald.conf
sudo systemctl restart systemd-journald
```

---

## 19. Troubleshooting

| Symptom | Cause | Fix |
| --- | --- | --- |
| Backend exits immediately, no log | A `STRIPE_*` variable is absent | All four must exist; empty is allowed, missing is not |
| `database "…" does not exist` | Wrong `DATABASE_URL` | Check the name and credentials |
| Migration fails on `CREATE EXTENSION` | Role is not superuser | `CREATE EXTENSION pg_trgm` as `postgres` first |
| Cosmetic images 404 in browser and game | `S3_BUCKET_ENDPOINT` is not public | Set it to `https://cdn.endlessclient.dev` |
| Shop loads, catalogue empty | Only priced, enabled cosmetics are listed | Give each a `base_price` |
| Browser console shows a CORS error | Origin missing from `CORS_ORIGINS` | Add it, restart the backend |
| Upload returns 502 with a Stripe message | Bad or missing `STRIPE_SECRET` | Use a valid key |
| Upload returns 401 | Wrong admin password | It is the raw `Authorization` header, no `Bearer` |
| Purchases never grant cosmetics | `STRIPE_WEBHOOK_SECRET` wrong | Re-copy from the endpoint, restart |
| In-game store shows the wrong catalogue | PolyPlus points elsewhere | Set its API URL, or rebuild `BackendUrl.kt` |
| WebSocket keeps reconnecting | nginx missing `Upgrade` headers | Add the map and the proxy headers |
| Analytics show one IP for everyone | `CLIENT_IP_SOURCE` unset | `RightmostXForwardedFor`, with nginx overwriting the header |
| Covers never generate | Render service down or unreachable | `curl 127.0.0.1:8090` |

---

## 20. Security checklist

- [ ] `ADMIN_PASSWORD` is long and random. It is a plain string comparison on
      every `/cosmetics/manage/*` request, with no rate limiting and no lockout.
- [ ] `admin.endlessclient.dev` sits behind HTTP auth as well.
- [ ] Stripe keys are **test** keys until you intend to take real payments.
- [ ] `/opt/endless/secrets/backend.env` is `0640`, `root:endless`.
- [ ] No secret is committed to the repository.
- [ ] `CLIENT_IP_SOURCE` matches nginx, and nginx **overwrites**
      `X-Forwarded-For`.
- [ ] Postgres, MinIO and the render service listen on `127.0.0.1` only.
- [ ] `ufw` allows SSH and HTTPS, nothing else.
- [ ] The bucket is **not** anonymous; reads are authorised by presigned URLs.
- [ ] `CORS_ORIGINS` lists exactly your own origins.
- [ ] Certificate renewal is tested (`certbot renew --dry-run`).
- [ ] Backups are copied off the machine and a restore has been rehearsed.

---

## 21. Known gaps

- **Cosmetics cannot be created without Stripe.** Uploading provisions a product
  and a price, so a deployment with no valid key can serve an existing catalogue
  but cannot add to it.
- **Deletion is a soft delete.** `/cosmetics/manage/delete` disables the cosmetic
  (the whole group when grouped); rows, assets and Stripe products stay. Nothing
  in the API hard-deletes.
- **Partial refunds are not handled.** Only a full `charge.refunded` revokes
  ownership.
- **PolyPlus does not build against this repository's OneConfig.** It pins
  `deps.oneconfig = 1.1.4` while this tree is `1.1.7-dev`, and that version
  removed the API its screens use — `ComposeScreen` no longer takes a
  `RenderMode`, `Theme` no longer takes `pixelGrid`/`designWidth`/`designHeight`,
  and `pixelGridScale` is gone. Porting means giving up continuous repaint and
  pixel-grid snapping, which changes how the emote wheel and menus render.
- **`plus-backend-main/scripts/populate-db.sql` is stale.** It targets the
  `emote`, `player_owned_emote` and `emote_package` tables that
  `m20260704_000004_drop_emotes` dropped. It is dev-only; never run it against a
  real database.
- **`backend-1` is not a maven server.** It reads maven metadata and advertises
  download URLs; it cannot serve artifacts or back a Gradle repository.
