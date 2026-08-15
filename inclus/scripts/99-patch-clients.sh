#!/usr/bin/env bash
# =============================================================================
#  Étape 99 — Pointer les clients sur VOTRE serveur
#
#  Deux composants embarquent des URL en dur vers l'infrastructure Polyfrost.
#  Tant qu'ils ne sont pas corrigés, ils ne parlent PAS à votre API :
#
#   1. plus-admin-dashboard/src/lib/settings.ts
#      ENV_OPTIONS ne propose que Local / Staging / Production Polyfrost.
#      DEFAULT_ENV vaut ENV_OPTIONS[0] : on insère votre serveur en tête,
#      il devient le choix par défaut.
#
#   2. PolyPlus/src/main/kotlin/org/polyfrost/polyplus/BackendUrl.kt
#      L'enum compilée dans le mod. Aucune de ses trois valeurs n'est votre
#      serveur : sans ce correctif, le magasin en jeu affiche le catalogue
#      de Polyfrost.
#
#  À lancer AVANT 60-frontends.sh (le tableau de bord est compilé là-bas).
#  Une copie .orig est conservée à côté de chaque fichier modifié.
#
#  Note : PolyPlus n'est de toute façon pas compilable contre le OneConfig de
#  ce dépôt (voir la section « Limites connues » de INSTALLATION-DEBIAN.md).
#  Le correctif est appliqué quand même, pour le jour où ce sera résolu.
# =============================================================================
# shellcheck source=../lib.sh
. "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/lib.sh"

charger_config

titre "99 — Correctifs clients"

API_URL="https://$DOMAIN_API"

sauvegarder() {
	[ -f "$1.orig" ] || cp -p "$1" "$1.orig"
}

# ===========================================================================
#  1. Tableau de bord admin
# ===========================================================================
REGLAGES="$ADMIN_SRC/src/lib/settings.ts"

if [ ! -f "$REGLAGES" ]; then
	avert "Fichier absent : $REGLAGES — étape ignorée."
elif grep -qF "$API_URL" "$REGLAGES"; then
	ok "settings.ts contient déjà $API_URL"
else
	sauvegarder "$REGLAGES"
	# Insère l'entrée juste après l'ouverture du tableau : elle devient
	# ENV_OPTIONS[0], donc DEFAULT_ENV.
	sed -i "s|^export const ENV_OPTIONS = \[|export const ENV_OPTIONS = [\n    { label: \"$DOMAIN\", value: \"$API_URL\" },|" \
		"$REGLAGES"
	if grep -qF "$API_URL" "$REGLAGES"; then
		ok "settings.ts : « $DOMAIN » ajouté en tête (devient l'environnement par défaut)"
		grep -n 'ENV_OPTIONS' -A 6 "$REGLAGES" | head -10
	else
		cp -p "$REGLAGES.orig" "$REGLAGES"
		avert "Le motif attendu n'a pas été trouvé : settings.ts restauré, à corriger à la main."
	fi
fi

# ===========================================================================
#  2. PolyPlus (mod Minecraft)
# ===========================================================================
BACKEND_KT="$SRC_DIR/PolyPlus/src/main/kotlin/org/polyfrost/polyplus/BackendUrl.kt"

if [ ! -f "$BACKEND_KT" ]; then
	avert "Fichier absent : $BACKEND_KT — étape ignorée."
elif grep -qF "$API_URL" "$BACKEND_KT"; then
	ok "BackendUrl.kt pointe déjà sur $API_URL"
else
	sauvegarder "$BACKEND_KT"
	sed -i "s|PRODUCTION(\"[^\"]*\")|PRODUCTION(\"$API_URL\")|" "$BACKEND_KT"
	if grep -qF "$API_URL" "$BACKEND_KT"; then
		ok "BackendUrl.kt : PRODUCTION pointe désormais sur $API_URL"
		grep -n 'PRODUCTION\|STAGING\|LOCAL' "$BACKEND_KT"
	else
		cp -p "$BACKEND_KT.orig" "$BACKEND_KT"
		avert "Le motif attendu n'a pas été trouvé : BackendUrl.kt restauré, à corriger à la main."
	fi
fi

echo
info "Côté joueur, la valeur retenue vient d'un menu déroulant « API URL »,"
info "persisté par instance dans config/polyplus.json :"
echo '    "apiUrl": { "class": "org.polyfrost.polyplus.BackendUrl", "value": "PRODUCTION" }'

titre "99 terminé"
