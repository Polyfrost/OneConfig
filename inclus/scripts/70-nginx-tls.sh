#!/usr/bin/env bash
# =============================================================================
#  Étape 70 — nginx et TLS
#
#  - contrôle des enregistrements DNS
#  - obtention des certificats Let's Encrypt
#  - installation de la map WebSocket et du vhost complet
#
#  Ordre imposé : les certificats DOIVENT exister avant l'installation du
#  vhost, sinon « nginx -t » échoue sur des ssl_certificate introuvables.
# =============================================================================
# shellcheck source=../lib.sh
. "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/lib.sh"

exiger_root
charger_config

titre "70 — nginx et TLS"

DOMAINES=("$DOMAIN" "$DOMAIN_WWW" "$DOMAIN_API" "$DOMAIN_ADMIN" "$DOMAIN_CDN")

# ---------------------------------------------------------------------------
info "Contrôle DNS"
# On compare à l'IP publique vue depuis l'extérieur ; en cas d'échec de la
# détection on se contente d'afficher les résolutions.
IP_PUBLIQUE="$(curl -fsS --max-time 5 https://api.ipify.org 2>/dev/null || true)"
[ -n "$IP_PUBLIQUE" ] && info "IP publique détectée : $IP_PUBLIQUE"

PROBLEME=0
for hote in "${DOMAINES[@]}"; do
	RESOLU="$(dig +short "$hote" A | tr '\n' ' ' | sed 's/ $//')"
	if [ -z "$RESOLU" ]; then
		erreur "$(printf '%-32s' "$hote") aucun enregistrement A"
		PROBLEME=1
	elif [ -n "$IP_PUBLIQUE" ] && ! printf '%s' "$RESOLU" | grep -qw "$IP_PUBLIQUE"; then
		avert "$(printf '%-32s' "$hote") $RESOLU  (≠ $IP_PUBLIQUE)"
	else
		ok "$(printf '%-32s' "$hote") $RESOLU"
	fi
done

if [ "$PROBLEME" = "1" ]; then
	mourir "Certains noms ne résolvent pas : certbot échouerait. Corrigez le DNS d'abord.
       cdn.$DOMAIN doit impérativement être joignable publiquement : l'API
       construit les URL présignées à partir de ce nom d'hôte."
fi

# ---------------------------------------------------------------------------
info "nginx doit tourner pour la validation HTTP-01"
systemctl enable nginx >/dev/null
systemctl start nginx
ok "nginx actif"

# ---------------------------------------------------------------------------
info "Certificats Let's Encrypt"
if [ -f "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" ]; then
	ok "Certificat déjà présent pour $DOMAIN"
	certbot certificates 2>/dev/null | sed -n '/Certificate Name/,/Expiry/p' | head -20 || true
else
	ARGS=()
	for hote in "${DOMAINES[@]}"; do ARGS+=(-d "$hote"); done
	certbot certonly --nginx "${ARGS[@]}" \
		--cert-name "$DOMAIN" \
		--agree-tos -m "$CERTBOT_EMAIL" --no-eff-email --non-interactive
	ok "Certificat émis pour ${DOMAINES[*]}"
fi

# ---------------------------------------------------------------------------
info "Map WebSocket (niveau http)"
install -m 0644 "$INCLUS_DIR/nginx/upgrade-map.conf" /etc/nginx/conf.d/upgrade-map.conf
ok "/etc/nginx/conf.d/upgrade-map.conf"

info "Vhosts"
rendre_modele "$INCLUS_DIR/nginx/endlessclient.conf.modele" \
	/etc/nginx/sites-available/endlessclient 0644 "root:root"
ln -sf /etc/nginx/sites-available/endlessclient /etc/nginx/sites-enabled/endlessclient

if [ -e /etc/nginx/sites-enabled/default ]; then
	rm -f /etc/nginx/sites-enabled/default
	ok "Site « default » désactivé"
fi

# ---------------------------------------------------------------------------
info "Validation de la configuration"
if nginx -t; then
	systemctl reload nginx
	ok "nginx rechargé"
else
	mourir "Configuration nginx invalide — rien n'a été rechargé, le site précédent tourne toujours."
fi

# ---------------------------------------------------------------------------
info "Renouvellement automatique"
systemctl list-timers 2>/dev/null | grep -i certbot || avert "Aucun timer certbot listé."
info "Test à blanc du renouvellement (peut prendre ~30 s)"
if certbot renew --dry-run >/dev/null 2>&1; then
	ok "Le renouvellement automatique fonctionne"
else
	avert "Le test de renouvellement a échoué. Relancez « certbot renew --dry-run » pour voir l'erreur."
fi

titre "70 terminé — enchaînez avec 80-sauvegardes.sh"
