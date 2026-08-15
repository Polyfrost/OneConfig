#!/usr/bin/env bash
# =============================================================================
#  Étape 20 — PostgreSQL
#
#  - rôle applicatif + base
#  - extension pg_trgm installée EN AMONT
#
#  Pourquoi pg_trgm ici : la migration
#  m20260717_000000_add_cosmetic_trigram_search fait
#  « CREATE EXTENSION IF NOT EXISTS pg_trgm », ce qui exige le superutilisateur.
#  En la créant maintenant avec le rôle postgres, le rôle applicatif n'a jamais
#  besoin de ce privilège : la migration voit l'extension déjà là et passe.
# =============================================================================
# shellcheck source=../lib.sh
. "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/lib.sh"

exiger_root
charger_config

titre "20 — PostgreSQL"

systemctl enable --now postgresql
ok "postgresql actif — version $(sudo -u postgres psql -tAc 'SHOW server_version;' 2>/dev/null || echo inconnue)"

# ---------------------------------------------------------------------------
info "Rôle applicatif « $DB_USER »"
RENDU="$(mktemp)"
trap 'rm -f "$RENDU"' EXIT
rendre_modele "$INCLUS_DIR/sql/00-base.sql.modele" "$RENDU" 0600 "root:root"
sudo -u postgres psql -v ON_ERROR_STOP=1 -f "$RENDU" >/dev/null
ok "Rôle créé ou mot de passe mis à jour"

# ---------------------------------------------------------------------------
info "Base « $DB_NAME »"
if sudo -u postgres psql -tAc "SELECT 1 FROM pg_database WHERE datname = '$DB_NAME';" | grep -q 1; then
	ok "La base existe déjà"
else
	sudo -u postgres createdb -O "$DB_USER" "$DB_NAME"
	ok "Base créée, propriétaire $DB_USER"
fi

# ---------------------------------------------------------------------------
info "Extension pg_trgm"
sudo -u postgres psql -v ON_ERROR_STOP=1 -d "$DB_NAME" \
	-c 'CREATE EXTENSION IF NOT EXISTS pg_trgm;' >/dev/null
sudo -u postgres psql -d "$DB_NAME" -c '\dx'
ok "pg_trgm disponible"

# ---------------------------------------------------------------------------
info "Vérification de l'écoute réseau"
ECOUTE="$(sudo -u postgres psql -tAc 'SHOW listen_addresses;')"
case "$ECOUTE" in
	localhost|127.0.0.1|'') ok "listen_addresses = « $ECOUTE » (boucle locale)" ;;
	*) avert "listen_addresses = « $ECOUTE » : PostgreSQL écoute au-delà de la boucle locale."
	   avert "ufw bloque le port 5432, mais remettez « localhost » dans postgresql.conf." ;;
esac

# ---------------------------------------------------------------------------
info "Test de connexion avec le rôle applicatif"
if PGPASSWORD="$DB_PASSWORD" psql -h 127.0.0.1 -U "$DB_USER" -d "$DB_NAME" -tAc 'SELECT 1;' >/dev/null 2>&1; then
	ok "Connexion applicative fonctionnelle"
else
	mourir "Impossible de se connecter avec $DB_USER.
       Vérifiez DB_PASSWORD, puis pg_hba.conf : une ligne
         host  all  all  127.0.0.1/32  scram-sha-256
       doit exister (rechargez avec : systemctl reload postgresql)."
fi

titre "20 terminé — enchaînez avec 30-minio.sh"
