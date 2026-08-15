#!/usr/bin/env bash
# =============================================================================
#  Étape 50 — API Poly+ (Rust)
#
#  - compilation en mode release
#  - installation du binaire dans $BIN_DIR
#  - génération de backend.env
#  - unité systemd, démarrage, migrations
#
#  La première compilation est longue (15 à 40 min selon la machine) : rustup
#  télécharge d'abord la chaîne 1.92.0 épinglée par rust-toolchain.toml, puis
#  cargo construit l'intégralité de l'arbre de dépendances.
# =============================================================================
# shellcheck source=../lib.sh
. "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/lib.sh"

exiger_root
charger_config

titre "50 — API Poly+"

[ -d "$BACKEND_SRC" ] || mourir "Répertoire absent : $BACKEND_SRC"

# ---------------------------------------------------------------------------
info "Compilation (cargo build --release) — soyez patient"
en_tant_que_service "
	set -e
	[ -f \"\$HOME/.cargo/env\" ] && . \"\$HOME/.cargo/env\"
	cd '$BACKEND_SRC'
	cargo build --release
"
ok "Compilation terminée"

BINAIRE="$BACKEND_SRC/target/release/plus-backend"
[ -x "$BINAIRE" ] || mourir "Binaire introuvable après compilation : $BINAIRE"

# ---------------------------------------------------------------------------
info "Installation du binaire"
# Le service est arrêté le temps du remplacement : impossible d'écraser un
# exécutable en cours d'utilisation, et deux instances contre la même base
# pendant une migration corrompraient l'état.
if systemctl is-active --quiet endless-backend; then
	systemctl stop endless-backend
	ARRETE=1
else
	ARRETE=0
fi

install -o "$SERVICE_USER" -g "$SERVICE_USER" -m 0755 "$BINAIRE" "$BIN_DIR/plus-backend"
ok "Installé : $BIN_DIR/plus-backend"
"$BIN_DIR/plus-backend" --version || true

# ---------------------------------------------------------------------------
info "Fichier d'environnement"
rendre_modele "$INCLUS_DIR/env/backend.env.modele" \
	"$SECRETS_DIR/backend.env" 0640 "root:$SERVICE_USER"

# ---------------------------------------------------------------------------
info "Unité systemd"
rendre_modele "$INCLUS_DIR/systemd/endless-backend.service.modele" \
	/etc/systemd/system/endless-backend.service 0644 "root:root"
recharger_systemd
systemctl enable endless-backend >/dev/null

info "Démarrage (les migrations tournent avant l'ouverture du socket)"
systemctl restart endless-backend

if attendre_port "$BACKEND_PORT" 180; then
	ok "L'API écoute sur 127.0.0.1:$BACKEND_PORT"
else
	erreur "L'API n'écoute toujours pas après 180 s."
	journalctl -u endless-backend -n 60 --no-pager || true
	echo
	erreur "Causes fréquentes :"
	erreur "  - une variable STRIPE_* absente du fichier d'environnement"
	erreur "    (vide = accepté, absente = refus de démarrer)"
	erreur "  - DATABASE_URL incorrect"
	erreur "  - migration bloquée sur CREATE EXTENSION (relancez l'étape 20)"
	mourir "Corrigez, puis relancez ce script."
fi

# ---------------------------------------------------------------------------
info "Vérification fonctionnelle"
if curl -fsS "http://127.0.0.1:$BACKEND_PORT/cosmetics" >/dev/null 2>&1; then
	ok "GET /cosmetics répond"
else
	avert "GET /cosmetics ne répond pas encore comme attendu — vérifiez le journal."
fi

[ "$ARRETE" = "1" ] && info "Le service avait été arrêté pour la mise à jour, il est redémarré."

titre "50 terminé — enchaînez avec 60-frontends.sh"
