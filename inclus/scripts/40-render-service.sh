#!/usr/bin/env bash
# =============================================================================
#  Étape 40 — Service de rendu des vignettes
#
#  Génère les images de couverture des cosmétiques avec un Chromium headless.
#  FACULTATIF : sans lui, les téléversements aboutissent quand même, ils
#  n'enregistrent simplement aucune couverture.
# =============================================================================
# shellcheck source=../lib.sh
. "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/lib.sh"

exiger_root
charger_config

titre "40 — Service de rendu"

[ -d "$RENDER_SRC" ] || mourir "Répertoire absent : $RENDER_SRC (l'étape 00 a-t-elle bien cloné le dépôt ?)"

# ---------------------------------------------------------------------------
info "Dépendances Node"
# PUPPETEER_SKIP_DOWNLOAD=true : on utilise le Chromium du système plutôt que
# de télécharger un second navigateur de ~150 Mo dans node_modules.
en_tant_que_service "
	cd '$RENDER_SRC' &&
	PUPPETEER_SKIP_DOWNLOAD=true npm ci
"
ok "node_modules installé"

# ---------------------------------------------------------------------------
info "Texture par défaut"
if [ -s "$RENDER_SRC/assets/default-skin.png" ]; then
	ok "assets/default-skin.png déjà présent"
else
	en_tant_que_service "cd '$RENDER_SRC' && node scripts/fetch-default-skin.mjs"
	ok "Texture téléchargée"
fi

# Sans ce fichier, le service retombe sur un fetch vers textures.minecraft.net
# à chaque rendu : lent, et cassé si la machine n'a pas d'accès sortant.

# ---------------------------------------------------------------------------
info "Chemin de Chromium"
CHROMIUM=""
for candidat in /usr/bin/chromium /usr/bin/chromium-browser /snap/bin/chromium; do
	[ -x "$candidat" ] && { CHROMIUM="$candidat"; break; }
done
if [ -z "$CHROMIUM" ]; then
	avert "Aucun Chromium trouvé : le service démarrera mais échouera à chaque rendu."
	CHROMIUM=/usr/bin/chromium
else
	ok "Chromium : $CHROMIUM"
fi

# ---------------------------------------------------------------------------
info "Unité systemd"
rendre_modele "$INCLUS_DIR/systemd/endless-render.service.modele" \
	/etc/systemd/system/endless-render.service 0644 "root:root"

# Ajuste le chemin de Chromium si ce n'est pas /usr/bin/chromium.
if [ "$CHROMIUM" != /usr/bin/chromium ]; then
	sed -i "s|PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium|PUPPETEER_EXECUTABLE_PATH=$CHROMIUM|" \
		/etc/systemd/system/endless-render.service
	ok "PUPPETEER_EXECUTABLE_PATH ajusté sur $CHROMIUM"
fi

recharger_systemd
systemctl enable endless-render >/dev/null
systemctl restart endless-render

if attendre_port "$RENDER_PORT" 30; then
	REPONSE="$(curl -fsS "http://127.0.0.1:$RENDER_PORT/" || true)"
	ok "Service de rendu joignable — réponse : ${REPONSE:-<vide>}"
else
	avert "Le service de rendu ne répond pas sur $RENDER_PORT."
	journalctl -u endless-render -n 30 --no-pager || true
	avert "Ce n'est pas bloquant : l'API démarrera sans couvertures d'images."
fi

titre "40 terminé — enchaînez avec 50-backend.sh"
