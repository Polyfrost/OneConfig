#!/usr/bin/env bash
# =============================================================================
#  Contrôle de bon fonctionnement — lisible sans être root, plus complet en root.
#
#      sudo ./inclus/scripts/verifier.sh
#
#  Ne modifie rien. Code de retour non nul si au moins un contrôle échoue.
# =============================================================================
# shellcheck source=../lib.sh
. "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/lib.sh"

charger_config

ECHECS=0
verifier() {
	local libelle="$1"; shift
	if "$@" >/dev/null 2>&1; then
		ok "$libelle"
	else
		erreur "$libelle"
		ECHECS=$((ECHECS + 1))
	fi
}

# ===========================================================================
titre "Services"
for unite in postgresql minio endless-backend endless-render endless-shop nginx; do
	if systemctl is-active --quiet "$unite"; then
		ok "$(printf '%-20s' "$unite") actif"
	elif [ "$unite" = "endless-render" ]; then
		avert "$(printf '%-20s' "$unite") inactif (facultatif : pas de vignettes)"
	else
		erreur "$(printf '%-20s' "$unite") INACTIF"
		ECHECS=$((ECHECS + 1))
	fi
done

# ===========================================================================
titre "Écoutes locales"
for couple in "5432:PostgreSQL" "$MINIO_PORT:MinIO" "$BACKEND_PORT:API" \
              "$RENDER_PORT:rendu" "$SHOP_PORT:boutique"; do
	port="${couple%%:*}"; nom="${couple#*:}"
	if attendre_port "$port" 1; then
		ok "$(printf '%-12s' "$nom") 127.0.0.1:$port"
	else
		erreur "$(printf '%-12s' "$nom") rien sur 127.0.0.1:$port"
		ECHECS=$((ECHECS + 1))
	fi
done

# ===========================================================================
titre "API en local"
verifier "GET /cosmetics"    curl -fsS "http://127.0.0.1:$BACKEND_PORT/cosmetics"
verifier "GET /openapi.json" curl -fsS "http://127.0.0.1:$BACKEND_PORT/openapi.json"

if command -v jq >/dev/null 2>&1; then
	NB="$(curl -fsS "http://127.0.0.1:$BACKEND_PORT/openapi.json" 2>/dev/null | jq '.paths | keys | length' 2>/dev/null || echo '?')"
	info "Routes exposées : $NB"
	CATALOGUE="$(curl -fsS "http://127.0.0.1:$BACKEND_PORT/cosmetics" 2>/dev/null | jq '.cosmetics | length' 2>/dev/null || echo '?')"
	info "Cosmétiques au catalogue : $CATALOGUE"
	[ "$CATALOGUE" = "0" ] && avert "Catalogue vide : seuls les cosmétiques ACTIVÉS et POURVUS D'UN PRIX sont listés."
fi

# ===========================================================================
titre "Accès public (TLS)"
verifier "https://$DOMAIN_API/cosmetics" curl -fsS --max-time 10 "https://$DOMAIN_API/cosmetics"
verifier "https://$DOMAIN/ (boutique)"   curl -fsS --max-time 10 -o /dev/null "https://$DOMAIN/"

CODE_CDN="$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' "https://$DOMAIN_CDN/" || echo 000)"
case "$CODE_CDN" in
	000) erreur "https://$DOMAIN_CDN/ injoignable — les images de cosmétiques renverront 404 partout"
	     ECHECS=$((ECHECS + 1)) ;;
	*)   ok "https://$DOMAIN_CDN/ répond (HTTP $CODE_CDN — 403 est normal, le bucket est privé)" ;;
esac

CODE_ADMIN="$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' "https://$DOMAIN_ADMIN/" || echo 000)"
if [ "$CODE_ADMIN" = "401" ]; then
	ok "https://$DOMAIN_ADMIN/ protégé par authentification HTTP (401)"
else
	erreur "https://$DOMAIN_ADMIN/ renvoie $CODE_ADMIN — l'auth HTTP devrait donner 401"
	ECHECS=$((ECHECS + 1))
fi

# ===========================================================================
titre "Bascule WebSocket"
# 101 = bascule acceptée, 426 = « mettez à niveau » ; 200 signifie que nginx
# a proxifié en HTTP simple et que la map Upgrade manque.
CODE_WS="$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' \
	-H 'Connection: Upgrade' -H 'Upgrade: websocket' \
	-H 'Sec-WebSocket-Version: 13' -H 'Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==' \
	"https://$DOMAIN_API/websocket" || echo 000)"
case "$CODE_WS" in
	101|426) ok "/websocket → HTTP $CODE_WS" ;;
	200)     erreur "/websocket → 200 : la map Upgrade est absente de nginx"
	         ECHECS=$((ECHECS + 1)) ;;
	*)       avert "/websocket → HTTP $CODE_WS (inattendu)" ;;
esac

# ===========================================================================
titre "Authentification d'administration de l'API"
SANS="$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -X POST \
	"https://$DOMAIN_API/cosmetics/manage/delete" \
	-H 'Content-Type: application/json' -d '{"cosmetic_id":999999}' || echo 000)"
AVEC="$(curl -s --max-time 10 -o /dev/null -w '%{http_code}' -X POST \
	"https://$DOMAIN_API/cosmetics/manage/delete" \
	-H "Authorization: $ADMIN_PASSWORD" \
	-H 'Content-Type: application/json' -d '{"cosmetic_id":999999}' || echo 000)"
if [ "$SANS" = "401" ]; then
	ok "Sans en-tête → 401"
else
	erreur "Sans en-tête → $SANS (401 attendu)"
	ECHECS=$((ECHECS + 1))
fi
if [ "$AVEC" != "401" ]; then
	ok "Avec le mot de passe → $AVEC (la route est atteinte ; 404 est normal pour un id inexistant)"
else
	erreur "Avec le mot de passe → 401 : ADMIN_PASSWORD ne correspond pas à celui du service"
	ECHECS=$((ECHECS + 1))
fi

# ===========================================================================
titre "Configuration"
if [ -r "$SECRETS_DIR/backend.env" ]; then
	PERMS="$(stat -c '%a %U:%G' "$SECRETS_DIR/backend.env")"
	if [ "$PERMS" = "640 root:$SERVICE_USER" ]; then
		ok "backend.env : $PERMS"
	else
		avert "backend.env : $PERMS (attendu : 640 root:$SERVICE_USER)"
	fi

	POINT_S3="$(grep '^S3_BUCKET_ENDPOINT=' "$SECRETS_DIR/backend.env" | cut -d= -f2-)"
	case "$POINT_S3" in
		https://"$DOMAIN_CDN") ok "S3_BUCKET_ENDPOINT = $POINT_S3" ;;
		*localhost*|*127.0.0.1*)
			erreur "S3_BUCKET_ENDPOINT = $POINT_S3 — les URL présignées seront inutilisables hors du serveur"
			ECHECS=$((ECHECS + 1)) ;;
		*) avert "S3_BUCKET_ENDPOINT = $POINT_S3" ;;
	esac
else
	info "backend.env illisible (lancez avec sudo pour ce contrôle)"
fi

# ===========================================================================
titre "Pare-feu"
if command -v ufw >/dev/null 2>&1 && ufw status 2>/dev/null | grep -q '^Status: active'; then
	ok "ufw actif"
	if ufw status | grep -qE "$BACKEND_PORT|$RENDER_PORT|$MINIO_PORT|5432"; then
		erreur "Un port interne est ouvert dans ufw — ils doivent tous rester fermés"
		ufw status
		ECHECS=$((ECHECS + 1))
	else
		ok "Aucun port interne exposé"
	fi
else
	avert "ufw inactif ou indisponible"
fi

# ===========================================================================
titre "Certificats"
if [ -r "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" ]; then
	FIN="$(openssl x509 -enddate -noout -in "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" | cut -d= -f2)"
	ok "Certificat valable jusqu'au $FIN"
else
	info "Certificat illisible (lancez avec sudo pour ce contrôle)"
fi

# ===========================================================================
titre "Sauvegardes"
if systemctl is-enabled --quiet endless-backup.timer 2>/dev/null; then
	ok "Timer de sauvegarde activé"
	if [ -d "$BACKUP_DIR" ]; then
		DERNIER="$(ls -t "$BACKUP_DIR"/*.dump 2>/dev/null | head -1 || true)"
		[ -n "$DERNIER" ] && info "Dernier dump : $(basename "$DERNIER") ($(du -h "$DERNIER" | cut -f1))" \
		                  || avert "Aucun dump dans $BACKUP_DIR"
	fi
else
	avert "Timer de sauvegarde non activé"
fi

# ===========================================================================
echo
if [ "$ECHECS" -eq 0 ]; then
	titre "Tous les contrôles sont passés"
	exit 0
else
	titre "$ECHECS contrôle(s) en échec"
	echo "Voir la section « Dépannage » de INSTALLATION-DEBIAN.md."
	exit 1
fi
