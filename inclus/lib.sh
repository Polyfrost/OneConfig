#!/usr/bin/env bash
# =============================================================================
#  Fonctions communes à tous les scripts de inclus/scripts/.
#  Ce fichier n'est jamais exécuté directement : il est sourcé.
# =============================================================================

set -euo pipefail

# Répertoire `inclus/`, quel que soit l'endroit d'où le script est lancé.
INCLUS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export INCLUS_DIR

# ---------------------------------------------------------------------------
# Affichage
# ---------------------------------------------------------------------------
if [ -t 1 ]; then
	_C_RESET=$'\033[0m'; _C_OK=$'\033[32m'; _C_INFO=$'\033[36m'
	_C_WARN=$'\033[33m'; _C_ERR=$'\033[31m'; _C_BOLD=$'\033[1m'
else
	_C_RESET=''; _C_OK=''; _C_INFO=''; _C_WARN=''; _C_ERR=''; _C_BOLD=''
fi

titre() { printf '\n%s=== %s ===%s\n' "$_C_BOLD" "$*" "$_C_RESET"; }
ok()    { printf '%s[ OK ]%s %s\n'   "$_C_OK"   "$_C_RESET" "$*"; }
info()  { printf '%s[ .. ]%s %s\n'   "$_C_INFO" "$_C_RESET" "$*"; }
avert() { printf '%s[ !! ]%s %s\n'   "$_C_WARN" "$_C_RESET" "$*" >&2; }
erreur(){ printf '%s[FAIL]%s %s\n'   "$_C_ERR"  "$_C_RESET" "$*" >&2; }
mourir(){ erreur "$*"; exit 1; }

# ---------------------------------------------------------------------------
# Pré-requis d'exécution
# ---------------------------------------------------------------------------
exiger_root() {
	[ "$(id -u)" -eq 0 ] || mourir "Ce script doit être lancé en root : sudo $0"
}

exiger_commande() {
	command -v "$1" >/dev/null 2>&1 || mourir "Commande absente : $1"
}

exiger_debian() {
	[ -f /etc/os-release ] || mourir "/etc/os-release introuvable : distribution non reconnue."
	# shellcheck disable=SC1091
	. /etc/os-release
	case "${ID:-}:${ID_LIKE:-}" in
		debian:*|ubuntu:*|*:*debian*) ;;
		*) avert "Distribution « ${PRETTY_NAME:-inconnue} » non testée. Attendu : Debian 12/13 ou Ubuntu 24.04." ;;
	esac
}

# ---------------------------------------------------------------------------
# Chargement de la configuration
# ---------------------------------------------------------------------------
# Ordre de recherche :
#   1. $ENDLESS_CONFIG (si défini)
#   2. /opt/endless/secrets/config.env  (copie installée sur le serveur)
#   3. inclus/config.env                (copie de travail)
charger_config() {
	local fichier="${ENDLESS_CONFIG:-}"

	if [ -z "$fichier" ]; then
		local candidat
		for candidat in /opt/endless/secrets/config.env "$INCLUS_DIR/config.env"; do
			if [ -f "$candidat" ]; then fichier="$candidat"; break; fi
		done
	fi

	if [ -z "$fichier" ] || [ ! -f "$fichier" ]; then
		mourir "Aucun config.env trouvé.
       Créez-le :  cp $INCLUS_DIR/config.env.exemple $INCLUS_DIR/config.env
                   chmod 600 $INCLUS_DIR/config.env
                   nano $INCLUS_DIR/config.env"
	fi

	set -a
	# shellcheck disable=SC1090
	. "$fichier"
	set +a

	CONFIG_FILE="$fichier"
	export CONFIG_FILE

	_derive_config
	_valide_config
}

# Valeurs dérivées : on ne demande à l'opérateur que le strict nécessaire.
_derive_config() {
	: "${SERVICE_USER:=endless}"
	: "${INSTALL_ROOT:=/opt/endless}"
	: "${GIT_BRANCH:=v2}"

	: "${DOMAIN_WWW:=www.${DOMAIN}}"
	: "${DOMAIN_API:=api.${DOMAIN}}"
	: "${DOMAIN_ADMIN:=admin.${DOMAIN}}"
	: "${DOMAIN_CDN:=cdn.${DOMAIN}}"

	: "${STRIPE_SUCCESS_URL:=https://${DOMAIN}/checkout/success}"
	: "${STRIPE_CANCEL_URL:=https://${DOMAIN}/checkout/cancel}"

	: "${DB_NAME:=endless_plus}"
	: "${DB_USER:=endless}"
	: "${S3_BUCKET:=endless-cosmetics}"
	: "${S3_REGION:=us-east-1}"
	: "${S3_ACCESS_KEY:=endless-api}"

	: "${RUST_LOG:=info,sea_orm=warn,sqlx=warn}"

	: "${BACKEND_PORT:=8080}"
	: "${RENDER_PORT:=8090}"
	: "${SHOP_PORT:=3000}"
	: "${MINIO_PORT:=9000}"
	: "${MINIO_CONSOLE_PORT:=9001}"

	: "${ADMIN_HTTP_USER:=admin}"
	: "${BACKUP_DIR:=/var/backups/endless}"
	: "${BACKUP_RETENTION_DAYS:=14}"
	: "${MINECRAFT_UUID:=}"

	# DATABASE_URL est une URL : un mot de passe contenant @ / : ? # & la
	# couperait au mauvais endroit. On en garde une version encodée.
	DB_PASSWORD_URL="$(_url_encode "${DB_PASSWORD-}")"
	export DB_PASSWORD_URL

	# Chemins standards
	SRC_DIR="$INSTALL_ROOT/src"
	BIN_DIR="$INSTALL_ROOT/bin"
	WEB_DIR="$INSTALL_ROOT/web"
	SECRETS_DIR="$INSTALL_ROOT/secrets"
	BACKEND_SRC="$SRC_DIR/plus-backend-main"
	RENDER_SRC="$BACKEND_SRC/render-service"
	SHOP_SRC="$SRC_DIR/plus-website"
	ADMIN_SRC="$SRC_DIR/plus-admin-dashboard"

	export SRC_DIR BIN_DIR WEB_DIR SECRETS_DIR \
	       BACKEND_SRC RENDER_SRC SHOP_SRC ADMIN_SRC \
	       DOMAIN_WWW DOMAIN_API DOMAIN_ADMIN DOMAIN_CDN \
	       STRIPE_SUCCESS_URL STRIPE_CANCEL_URL
}

# Refuse de continuer si une valeur obligatoire manque ou contient CHANGEME.
# STRIPE_* est volontairement toléré (on avertit seulement), tout le reste
# est bloquant.
_valide_config() {
	local nom valeur manquantes=() changeme=()

	local obligatoires=(
		DOMAIN CERTBOT_EMAIL
		DB_NAME DB_USER DB_PASSWORD
		MINIO_ROOT_USER MINIO_ROOT_PASSWORD
		S3_BUCKET S3_ACCESS_KEY S3_SECRET_KEY
		ADMIN_PASSWORD
		ADMIN_HTTP_USER ADMIN_HTTP_PASSWORD
	)

	for nom in "${obligatoires[@]}"; do
		valeur="${!nom-}"
		if [ -z "$valeur" ]; then
			manquantes+=("$nom")
		elif case "$valeur" in *CHANGEME*) true ;; *) false ;; esac; then
			changeme+=("$nom")
		fi
	done

	if [ ${#manquantes[@]} -gt 0 ]; then
		mourir "Variables absentes de $CONFIG_FILE : ${manquantes[*]}"
	fi
	if [ ${#changeme[@]} -gt 0 ]; then
		mourir "Variables encore à la valeur CHANGEME dans $CONFIG_FILE : ${changeme[*]}"
	fi

	# Avertissements non bloquants
	case "${STRIPE_SECRET-}" in
		''|*CHANGEME*) avert "STRIPE_SECRET n'est pas renseigné : l'API démarrera, mais la création de cosmétiques échouera." ;;
		sk_live_*)     avert "STRIPE_SECRET est une clé LIVE : les paiements seront réels." ;;
	esac
	case "${STRIPE_WEBHOOK_SECRET-}" in
		''|*CHANGEME*) avert "STRIPE_WEBHOOK_SECRET n'est pas renseigné : les achats ne débloqueront aucun cosmétique." ;;
	esac
	if [ "${#ADMIN_PASSWORD}" -lt 24 ]; then
		avert "ADMIN_PASSWORD fait ${#ADMIN_PASSWORD} caractères. Il est comparé en clair, sans limite de tentatives : visez 40+ (openssl rand -base64 36)."
	fi
}

# ---------------------------------------------------------------------------
# Rendu des modèles
# ---------------------------------------------------------------------------
# Les modèles utilisent des marqueurs @@NOM@@ plutôt que $NOM, pour ne pas
# entrer en collision avec les variables nginx ($host, $remote_addr, ...).
_echappe_sed() {
	printf '%s' "${1-}" | sed -e 's/[\\&|]/\\&/g'
}

# Encodage pour-cent, pour insérer une valeur dans une URL (mot de passe de
# connexion PostgreSQL notamment). Suppose une chaîne ASCII — ce que produit
# « openssl rand -base64 ».
_url_encode() {
	local chaine="${1-}" i c hex sortie=''
	for (( i = 0; i < ${#chaine}; i++ )); do
		c="${chaine:i:1}"
		case "$c" in
			[a-zA-Z0-9.~_-]) sortie+="$c" ;;
			*) printf -v hex '%%%02X' "'$c"; sortie+="$hex" ;;
		esac
	done
	printf '%s' "$sortie"
}

# rendre_modele <source> <destination> [mode] [proprietaire]
rendre_modele() {
	local src="$1" dst="$2" mode="${3:-0644}" proprio="${4:-root:root}"
	[ -f "$src" ] || mourir "Modèle introuvable : $src"

	local tmp
	tmp="$(mktemp)"

	local nom
	local marqueurs=(
		DOMAIN DOMAIN_WWW DOMAIN_API DOMAIN_ADMIN DOMAIN_CDN
		SERVICE_USER INSTALL_ROOT
		SRC_DIR BIN_DIR WEB_DIR SECRETS_DIR
		BACKEND_SRC RENDER_SRC SHOP_SRC ADMIN_SRC
		DB_NAME DB_USER DB_PASSWORD DB_PASSWORD_URL
		MINIO_ROOT_USER MINIO_ROOT_PASSWORD
		S3_BUCKET S3_REGION S3_ACCESS_KEY S3_SECRET_KEY
		ADMIN_PASSWORD RUST_LOG
		STRIPE_SECRET STRIPE_WEBHOOK_SECRET STRIPE_SUCCESS_URL STRIPE_CANCEL_URL
		MINECRAFT_UUID
		BACKEND_PORT RENDER_PORT SHOP_PORT MINIO_PORT MINIO_CONSOLE_PORT
		BACKUP_DIR BACKUP_RETENTION_DAYS
		CERTBOT_EMAIL ADMIN_HTTP_USER
		GIT_REPO GIT_BRANCH
	)

	local expressions=()
	for nom in "${marqueurs[@]}"; do
		expressions+=(-e "s|@@${nom}@@|$(_echappe_sed "${!nom-}")|g")
	done

	sed "${expressions[@]}" "$src" > "$tmp"

	if grep -q '@@[A-Z_]\+@@' "$tmp"; then
		local restants
		restants="$(grep -o '@@[A-Z_]\+@@' "$tmp" | sort -u | tr '\n' ' ')"
		rm -f "$tmp"
		mourir "Marqueurs non résolus dans $src : $restants"
	fi

	install -m "$mode" -o "${proprio%%:*}" -g "${proprio##*:}" "$tmp" "$dst"
	rm -f "$tmp"
	ok "Écrit : $dst  ($mode $proprio)"
}

# ---------------------------------------------------------------------------
# Utilitaires
# ---------------------------------------------------------------------------
# Exécute une commande en tant qu'utilisateur de service, avec un shell de
# connexion. Le shell est forcé (-s) parce que le compte a /usr/sbin/nologin,
# et le mode login (-l) est nécessaire pour que ~/.cargo/env soit chargé.
en_tant_que_service() {
	runuser -l "$SERVICE_USER" -s /bin/bash -c "$1"
}

recharger_systemd() {
	systemctl daemon-reload
	ok "systemd rechargé"
}

# Attend qu'un port local réponde (défaut : 30 s).
attendre_port() {
	local port="$1" limite="${2:-30}" i=0
	while [ "$i" -lt "$limite" ]; do
		if (exec 3<>"/dev/tcp/127.0.0.1/$port") 2>/dev/null; then
			exec 3>&- 3<&- 2>/dev/null || true
			return 0
		fi
		i=$((i + 1))
		sleep 1
	done
	return 1
}

# Normalise un UUID Minecraft sans tirets en UUID canonique 8-4-4-4-12.
uuid_avec_tirets() {
	local u="${1//-/}"
	if [ "${#u}" -ne 32 ]; then
		printf '%s' "$1"
		return
	fi
	printf '%s-%s-%s-%s-%s' \
		"${u:0:8}" "${u:8:4}" "${u:12:4}" "${u:16:4}" "${u:20:12}"
}
