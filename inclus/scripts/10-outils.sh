#!/usr/bin/env bash
# =============================================================================
#  Étape 10 — Chaînes d'outils
#
#  - Rust (rustup, pour l'utilisateur de service)
#  - Node.js 22 (dépôt NodeSource — celui de Debian est trop ancien pour
#    Next.js 16)
#  - Chromium (moteur du service de rendu)
#
#  La version de Rust n'est PAS choisie ici : plus-backend-main/rust-toolchain.toml
#  épingle 1.92.0 et rustup la télécharge automatiquement au premier build.
# =============================================================================
# shellcheck source=../lib.sh
. "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/lib.sh"

exiger_root
charger_config

titre "10 — Chaînes d'outils"

# ---------------------------------------------------------------------------
info "Rust (rustup) pour $SERVICE_USER"
if en_tant_que_service 'command -v rustup >/dev/null 2>&1'; then
	ok "rustup déjà installé"
	en_tant_que_service 'rustup --version'
else
	en_tant_que_service '
		curl --proto "=https" --tlsv1.2 -sSf https://sh.rustup.rs \
			| sh -s -- -y --no-modify-path --default-toolchain none
	'
	# --no-modify-path : on ajoute la ligne nous-mêmes, une seule fois.
	PROFIL="$INSTALL_ROOT/.bashrc"
	touch "$PROFIL"
	chown "$SERVICE_USER:$SERVICE_USER" "$PROFIL"
	if ! grep -q 'cargo/env' "$PROFIL"; then
		printf '\n. "$HOME/.cargo/env"\n' >> "$PROFIL"
	fi
	ok "rustup installé"
fi

# ---------------------------------------------------------------------------
info "Node.js 22"
if command -v node >/dev/null 2>&1 && node --version | grep -qE '^v(2[2-9]|[3-9][0-9])\.'; then
	ok "Node $(node --version) déjà présent"
else
	curl -fsSL https://deb.nodesource.com/setup_22.x | bash -
	DEBIAN_FRONTEND=noninteractive apt-get -y install nodejs
	ok "Node $(node --version) installé"
fi
info "npm $(npm --version)"

# ---------------------------------------------------------------------------
info "Chromium (service de rendu)"
if command -v chromium >/dev/null 2>&1; then
	ok "chromium présent : $(command -v chromium)"
elif command -v chromium-browser >/dev/null 2>&1; then
	avert "Seul « chromium-browser » est présent ($(command -v chromium-browser))."
	avert "Ajustez PUPPETEER_EXECUTABLE_PATH dans l'unité endless-render en conséquence."
else
	if DEBIAN_FRONTEND=noninteractive apt-get -y install chromium; then
		ok "chromium installé"
	else
		avert "Échec de l'installation de chromium via apt."
		avert "Le service de rendu est facultatif : sans lui, les cosmétiques se"
		avert "téléversent normalement mais n'ont pas d'image de couverture."
	fi
fi

# Sous Ubuntu, chromium peut arriver en snap : le mode headless se comporte
# mal sous systemd avec ProtectSystem=strict. Préférez le paquet .deb.
if command -v chromium >/dev/null 2>&1 && readlink -f "$(command -v chromium)" | grep -q '/snap/'; then
	avert "chromium provient d'un snap. Attendez-vous à des échecs de rendu sous systemd."
fi

titre "10 terminé — enchaînez avec 20-postgresql.sh"
