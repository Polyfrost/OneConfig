#!/usr/bin/env bash
# =============================================================================
#  Étape 60 — Interfaces web
#
#  - boutique (Next.js) : build + service systemd
#  - tableau de bord admin (Vite) : build statique servi par nginx
#  - fichier htpasswd protégeant le tableau de bord
#
#  /!\ NEXT_PUBLIC_BACKEND_URL est inliné DANS LE BUNDLE au moment du build.
#      Changer le domaine plus tard impose de relancer ce script.
# =============================================================================
# shellcheck source=../lib.sh
. "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/lib.sh"

exiger_root
charger_config

titre "60 — Interfaces web"

# ===========================================================================
#  Boutique
# ===========================================================================
[ -d "$SHOP_SRC" ] || mourir "Répertoire absent : $SHOP_SRC"

info "Configuration de la boutique"
rendre_modele "$INCLUS_DIR/env/website.env.local.modele" \
	"$SHOP_SRC/.env.local" 0644 "$SERVICE_USER:$SERVICE_USER"

info "Build de la boutique (npm ci && npm run build)"
en_tant_que_service "
	set -e
	cd '$SHOP_SRC'
	npm ci
	npm run build
"
ok "Boutique compilée"

info "Unité systemd de la boutique"
rendre_modele "$INCLUS_DIR/systemd/endless-shop.service.modele" \
	/etc/systemd/system/endless-shop.service 0644 "root:root"
recharger_systemd
systemctl enable endless-shop >/dev/null
systemctl restart endless-shop

if attendre_port "$SHOP_PORT" 60; then
	CODE="$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:$SHOP_PORT/")"
	ok "Boutique joignable sur 127.0.0.1:$SHOP_PORT (HTTP $CODE)"
else
	erreur "La boutique n'écoute pas sur $SHOP_PORT."
	journalctl -u endless-shop -n 40 --no-pager || true
	mourir "Consultez le journal ci-dessus."
fi

# ===========================================================================
#  Tableau de bord admin
# ===========================================================================
[ -d "$ADMIN_SRC" ] || mourir "Répertoire absent : $ADMIN_SRC"

# Le tableau de bord embarque une liste d'environnements EN DUR
# (src/lib/settings.ts) qui pointe vers les serveurs Polyfrost. Sans le
# correctif de 99-patch-clients.sh, le menu déroulant ne proposera pas
# votre API — il faudra saisir l'URL à la main dans le navigateur.
if grep -q 'plus\.polyfrost\.org' "$ADMIN_SRC/src/lib/settings.ts" 2>/dev/null; then
	avert "src/lib/settings.ts pointe encore sur les serveurs Polyfrost."
	avert "Lancez 99-patch-clients.sh AVANT ce script pour y ajouter https://$DOMAIN_API."
fi

info "Build du tableau de bord (npm ci && npm run build)"
en_tant_que_service "
	set -e
	cd '$ADMIN_SRC'
	npm ci
	npm run build
"
[ -d "$ADMIN_SRC/dist" ] || mourir "Le build n'a produit aucun répertoire dist/"
ok "Tableau de bord compilé"

info "Publication dans $WEB_DIR/admin"
rm -rf "$WEB_DIR/admin.ancien"
[ -d "$WEB_DIR/admin" ] && mv "$WEB_DIR/admin" "$WEB_DIR/admin.ancien"
cp -r "$ADMIN_SRC/dist" "$WEB_DIR/admin"
chown -R "$SERVICE_USER:$SERVICE_USER" "$WEB_DIR/admin"
# nginx (www-data) doit pouvoir traverser et lire.
chmod -R a+rX "$WEB_DIR/admin"
rm -rf "$WEB_DIR/admin.ancien"
ok "Fichiers statiques publiés"

# ---------------------------------------------------------------------------
info "Authentification HTTP du tableau de bord"
# Le champ mot de passe du tableau de bord protège l'API, pas la page :
# n'importe qui pourrait charger la console sans cette couche.
htpasswd -cbB /etc/nginx/endless-admin.htpasswd \
	"$ADMIN_HTTP_USER" "$ADMIN_HTTP_PASSWORD" >/dev/null
chown root:www-data /etc/nginx/endless-admin.htpasswd
chmod 640 /etc/nginx/endless-admin.htpasswd
ok "htpasswd créé pour « $ADMIN_HTTP_USER »"

titre "60 terminé — enchaînez avec 70-nginx-tls.sh"
