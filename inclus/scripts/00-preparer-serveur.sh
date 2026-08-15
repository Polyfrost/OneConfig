#!/usr/bin/env bash
# =============================================================================
#  Étape 00 — Préparation du serveur
#
#  - paquets système
#  - utilisateur de service et arborescence /opt/endless
#  - pare-feu ufw (SSH + HTTPS uniquement)
#  - plafonnement du journal systemd
#  - clonage du dépôt
#  - copie de config.env dans le coffre du serveur
#
#  Idempotent : relançable autant de fois que nécessaire.
# =============================================================================
# shellcheck source=../lib.sh
. "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/lib.sh"

exiger_root
exiger_debian
charger_config

titre "00 — Préparation du serveur"

# ---------------------------------------------------------------------------
info "Installation des paquets système"
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get -y upgrade
apt-get -y install \
	build-essential pkg-config libssl-dev \
	git curl wget ca-certificates gnupg \
	nginx ufw unzip jq openssl \
	apache2-utils \
	certbot python3-certbot-nginx \
	postgresql postgresql-contrib \
	dnsutils
ok "Paquets installés"

# ---------------------------------------------------------------------------
info "Utilisateur de service « $SERVICE_USER »"
if id -u "$SERVICE_USER" >/dev/null 2>&1; then
	ok "L'utilisateur existe déjà"
else
	useradd --system --create-home --home-dir "$INSTALL_ROOT" \
		--shell /usr/sbin/nologin "$SERVICE_USER"
	ok "Utilisateur créé"
fi

info "Arborescence sous $INSTALL_ROOT"
install -d -o "$SERVICE_USER" -g "$SERVICE_USER" -m 0755 \
	"$INSTALL_ROOT" "$SRC_DIR" "$BIN_DIR" "$WEB_DIR"
# Le coffre appartient à root ; le service n'a qu'un accès en lecture par groupe.
install -d -o root -g "$SERVICE_USER" -m 0750 "$SECRETS_DIR"
ok "Répertoires en place"

# ---------------------------------------------------------------------------
info "Pare-feu"
# Rien d'autre que SSH et HTTPS : PostgreSQL, MinIO, le service de rendu et
# l'API ne sont joignables que par la boucle locale, à travers nginx.
ufw allow OpenSSH        >/dev/null
ufw allow 'Nginx Full'   >/dev/null
if ufw status | grep -q '^Status: active'; then
	ok "ufw déjà actif"
else
	ufw --force enable
	ok "ufw activé"
fi
ufw status verbose

# ---------------------------------------------------------------------------
info "Plafonnement du journal systemd"
install -d -m 0755 /etc/systemd/journald.conf.d
install -m 0644 "$INCLUS_DIR/journald/99-endless.conf" \
	/etc/systemd/journald.conf.d/99-endless.conf
systemctl restart systemd-journald
ok "journald plafonné à 2 Go"

# ---------------------------------------------------------------------------
info "Sources dans $SRC_DIR"
if [ -d "$SRC_DIR/.git" ]; then
	en_tant_que_service "cd '$SRC_DIR' && git fetch --all --prune && git checkout '$GIT_BRANCH' && git pull --ff-only"
	ok "Dépôt mis à jour (branche $GIT_BRANCH)"
else
	en_tant_que_service "git clone --branch '$GIT_BRANCH' '$GIT_REPO' '$SRC_DIR'"
	ok "Dépôt cloné (branche $GIT_BRANCH)"
fi

for composant in plus-backend-main plus-website plus-admin-dashboard; do
	[ -d "$SRC_DIR/$composant" ] || avert "Composant absent du dépôt : $composant"
done

# ---------------------------------------------------------------------------
info "Copie de la configuration dans le coffre"
if [ "$CONFIG_FILE" != "$SECRETS_DIR/config.env" ]; then
	install -o root -g root -m 0600 "$CONFIG_FILE" "$SECRETS_DIR/config.env"
	ok "Configuration copiée dans $SECRETS_DIR/config.env"
	info "Les étapes suivantes la reliront depuis cet emplacement."
else
	ok "La configuration est déjà dans le coffre"
fi

titre "00 terminé — enchaînez avec 10-outils.sh"
