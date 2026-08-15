#!/usr/bin/env bash
# =============================================================================
#  Installation complète, dans l'ordre.
#
#      sudo ./inclus/scripts/installer-tout.sh
#
#  Chaque étape est idempotente : en cas d'échec, corrigez et relancez ce
#  script, les étapes déjà passées se contentent de revalider l'existant.
#
#  Options :
#      --depuis 50     reprendre à partir de l'étape 50
#      --sans-tls      sauter l'étape 70 (utile derrière un autre reverse proxy)
#      --sans-admin    sauter l'étape 90 (premier administrateur)
# =============================================================================
# shellcheck source=../lib.sh
. "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/lib.sh"

exiger_root
# Chargée dès maintenant : mieux vaut échouer sur un CHANGEME oublié avant
# 40 minutes de compilation que pendant.
charger_config

DEPUIS=0
SANS_TLS=0
SANS_ADMIN=0

while [ $# -gt 0 ]; do
	case "$1" in
		--depuis)    DEPUIS="${2:-0}"; shift 2 ;;
		--sans-tls)  SANS_TLS=1; shift ;;
		--sans-admin) SANS_ADMIN=1; shift ;;
		-h|--help)   sed -n '2,20p' "$0"; exit 0 ;;
		*)           mourir "Option inconnue : $1" ;;
	esac
done

ETAPES=(
	"00:00-preparer-serveur.sh"
	"10:10-outils.sh"
	"20:20-postgresql.sh"
	"30:30-minio.sh"
	"40:40-render-service.sh"
	"50:50-backend.sh"
	"99:99-patch-clients.sh"
	"60:60-frontends.sh"
	"70:70-nginx-tls.sh"
	"80:80-sauvegardes.sh"
	"90:90-premier-admin.sh"
)

DEBUT="$(date -u +%s)"

for entree in "${ETAPES[@]}"; do
	numero="${entree%%:*}"
	script="${entree#*:}"

	# 99 (correctifs clients) doit passer avant 60 : il modifie des sources
	# que 60 compile. Son numéro élevé ne reflète que son caractère optionnel.
	rang="$numero"
	[ "$numero" = "99" ] && rang="55"

	if [ "$rang" -lt "$DEPUIS" ]; then
		info "Étape $numero ignorée (--depuis $DEPUIS)"
		continue
	fi
	if [ "$numero" = "70" ] && [ "$SANS_TLS" = "1" ]; then
		info "Étape 70 ignorée (--sans-tls)"
		continue
	fi
	if [ "$numero" = "90" ] && [ "$SANS_ADMIN" = "1" ]; then
		info "Étape 90 ignorée (--sans-admin)"
		continue
	fi

	titre "Étape $numero — $script"
	if ! bash "$INCLUS_DIR/scripts/$script"; then
		erreur "Échec à l'étape $numero ($script)."
		erreur "Corrigez, puis reprenez :  sudo $0 --depuis $numero"
		exit 1
	fi
done

DUREE=$(( $(date -u +%s) - DEBUT ))

titre "Installation terminée en $((DUREE / 60)) min $((DUREE % 60)) s"
echo
info "Contrôle final :"
echo "    sudo $INCLUS_DIR/scripts/verifier.sh"
echo
info "Il reste deux opérations manuelles, non automatisables :"
echo "    1. Déclarer le webhook Stripe → https://$DOMAIN_API/stripe/webhook"
echo "       avec exactement ces trois événements :"
echo "         checkout.session.completed"
echo "         checkout.session.async_payment_succeeded"
echo "         charge.refunded"
echo "    2. Recopier son secret de signature dans STRIPE_WEBHOOK_SECRET, puis :"
echo "       sudo $INCLUS_DIR/scripts/00-preparer-serveur.sh   # resynchronise le coffre"
echo "       sudo $INCLUS_DIR/scripts/50-backend.sh            # régénère backend.env"
