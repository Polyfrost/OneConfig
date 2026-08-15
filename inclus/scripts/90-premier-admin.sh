#!/usr/bin/env bash
# =============================================================================
#  Étape 90 — Premier administrateur
#
#  À lancer APRÈS le premier démarrage réussi du backend : c'est lui qui crée
#  la table « user » via ses migrations.
#
#  Rôles : player | moderator | admin
# =============================================================================
# shellcheck source=../lib.sh
. "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/lib.sh"

exiger_root
charger_config

titre "90 — Premier administrateur"

if [ -z "${MINECRAFT_UUID:-}" ] || case "$MINECRAFT_UUID" in *CHANGEME*) true ;; *) false ;; esac; then
	erreur "MINECRAFT_UUID n'est pas renseigné dans $CONFIG_FILE."
	echo
	echo "Récupérez le vôtre :"
	echo "    curl -s https://api.mojang.com/users/profiles/minecraft/VOTREPSEUDO | jq -r .id"
	echo
	echo "Puis renseignez-le et relancez ce script."
	exit 1
fi

# L'API Mojang renvoie l'UUID sans tirets ; la colonne est de type uuid.
MINECRAFT_UUID="$(uuid_avec_tirets "$MINECRAFT_UUID")"
export MINECRAFT_UUID
info "UUID normalisé : $MINECRAFT_UUID"

if ! printf '%s' "$MINECRAFT_UUID" | grep -qiE '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'; then
	mourir "« $MINECRAFT_UUID » n'est pas un UUID valide."
fi

# ---------------------------------------------------------------------------
info "Vérification de la présence de la table « user »"
if ! sudo -u postgres psql -d "$DB_NAME" -tAc "SELECT to_regclass('public.\"user\"');" | grep -q '^user$'; then
	mourir "La table « user » n'existe pas encore.
       Démarrez le backend une première fois (étape 50) : ce sont ses
       migrations qui créent le schéma."
fi

# ---------------------------------------------------------------------------
info "Promotion"
RENDU="$(mktemp)"
trap 'rm -f "$RENDU"' EXIT
rendre_modele "$INCLUS_DIR/sql/10-premier-admin.sql.modele" "$RENDU" 0600 "root:root"
sudo -u postgres psql -v ON_ERROR_STOP=1 -d "$DB_NAME" -f "$RENDU"
ok "Rôle « admin » attribué"

titre "90 terminé — lancez verifier.sh pour le contrôle final"
