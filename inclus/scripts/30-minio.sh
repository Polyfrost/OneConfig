#!/usr/bin/env bash
# =============================================================================
#  Étape 30 — Stockage objet (MinIO)
#
#  - installation du serveur et du client mc
#  - service systemd écoutant sur 127.0.0.1 uniquement
#  - bucket + clé d'accès applicative dédiée
#
#  N'importe quel stockage compatible S3 conviendrait ; MinIO est simplement
#  le plus simple à auto-héberger. Si vous utilisez un service externe,
#  sautez cette étape et renseignez S3_* dans config.env.
# =============================================================================
# shellcheck source=../lib.sh
. "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/lib.sh"

exiger_root
charger_config

titre "30 — Stockage objet (MinIO)"

ARCH="$(dpkg --print-architecture)"
case "$ARCH" in
	amd64|arm64) ok "Architecture : $ARCH" ;;
	*) mourir "Architecture non gérée par les paquets MinIO officiels : $ARCH" ;;
esac

# ---------------------------------------------------------------------------
info "Serveur MinIO"
if command -v minio >/dev/null 2>&1; then
	ok "minio déjà installé — $(minio --version 2>&1 | head -1)"
else
	curl -fsSLo /tmp/minio.deb "https://dl.min.io/server/minio/release/linux-${ARCH}/minio.deb"
	dpkg -i /tmp/minio.deb
	rm -f /tmp/minio.deb
	ok "minio installé"
fi

info "Client mc"
if command -v mc >/dev/null 2>&1; then
	ok "mc déjà installé"
else
	curl -fsSLo /tmp/mc "https://dl.min.io/client/mc/release/linux-${ARCH}/mc"
	install -m 0755 /tmp/mc /usr/local/bin/mc
	rm -f /tmp/mc
	ok "mc installé"
fi

# ---------------------------------------------------------------------------
info "Utilisateur et volume"
if ! id -u minio-user >/dev/null 2>&1; then
	useradd --system --home-dir /var/lib/minio --shell /usr/sbin/nologin minio-user
fi
install -d -o minio-user -g minio-user -m 0750 /var/lib/minio
ok "/var/lib/minio prêt"

# ---------------------------------------------------------------------------
info "Configuration du service"
rendre_modele "$INCLUS_DIR/env/minio.default.modele" /etc/default/minio 0600 "root:root"

systemctl enable minio >/dev/null
systemctl restart minio

if attendre_port "$MINIO_PORT" 30; then
	ok "MinIO répond sur 127.0.0.1:$MINIO_PORT"
else
	erreur "MinIO n'écoute pas sur $MINIO_PORT après 30 s."
	journalctl -u minio -n 30 --no-pager || true
	mourir "Consultez le journal ci-dessus."
fi

# ---------------------------------------------------------------------------
info "Bucket et clé applicative"
# L'alias est écrit dans ~/.mc/config.json de root : gardez ce fichier privé.
mc alias set local "http://127.0.0.1:$MINIO_PORT" \
	"$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null
ok "Alias mc « local » configuré"

mc mb --ignore-existing "local/$S3_BUCKET" >/dev/null
ok "Bucket « $S3_BUCKET » présent"

# Le bucket reste PRIVÉ : les lectures publiques passent par des URL présignées
# générées par l'API. Ne le passez jamais en accès anonyme.
if mc anonymous get "local/$S3_BUCKET" 2>/dev/null | grep -qi 'public\|download'; then
	avert "Le bucket a une politique anonyme. Retirez-la : mc anonymous set none local/$S3_BUCKET"
fi

# `mc admin user add` met à jour le secret si l'utilisateur existe déjà.
mc admin user add local "$S3_ACCESS_KEY" "$S3_SECRET_KEY" >/dev/null
ok "Clé applicative « $S3_ACCESS_KEY » créée ou mise à jour"

# Selon la version de mc : « policy attach » (récent) ou « policy set » (ancien).
if mc admin policy attach local readwrite --user "$S3_ACCESS_KEY" >/dev/null 2>&1; then
	ok "Politique readwrite attachée"
elif mc admin policy set local readwrite "user=$S3_ACCESS_KEY" >/dev/null 2>&1; then
	ok "Politique readwrite attachée (syntaxe héritée)"
else
	# Déjà attachée : mc renvoie une erreur, ce n'est pas un problème.
	avert "Impossible d'attacher la politique — probablement déjà en place. Vérifiez :"
	avert "    mc admin user info local $S3_ACCESS_KEY"
fi

# ---------------------------------------------------------------------------
info "Test d'écriture avec la clé applicative"
mc alias set verif-app "http://127.0.0.1:$MINIO_PORT" \
	"$S3_ACCESS_KEY" "$S3_SECRET_KEY" >/dev/null
TEMOIN="$(mktemp)"
printf 'endlessclient\n' > "$TEMOIN"
if mc cp "$TEMOIN" "verif-app/$S3_BUCKET/.verification" >/dev/null 2>&1; then
	mc rm "verif-app/$S3_BUCKET/.verification" >/dev/null 2>&1 || true
	ok "La clé applicative peut écrire dans le bucket"
else
	rm -f "$TEMOIN"
	mc alias remove verif-app >/dev/null 2>&1 || true
	mourir "La clé applicative ne peut pas écrire dans $S3_BUCKET : vérifiez la politique."
fi
rm -f "$TEMOIN"
mc alias remove verif-app >/dev/null 2>&1 || true

titre "30 terminé — enchaînez avec 40-render-service.sh"
