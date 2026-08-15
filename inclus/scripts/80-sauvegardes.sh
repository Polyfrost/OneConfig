#!/usr/bin/env bash
# =============================================================================
#  Étape 80 — Sauvegardes automatiques
#
#  Installe /usr/local/bin/endless-backup, l'unité et le timer quotidien,
#  puis exécute une sauvegarde de test immédiatement.
# =============================================================================
# shellcheck source=../lib.sh
. "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/lib.sh"

exiger_root
charger_config

titre "80 — Sauvegardes"

info "Répertoire de destination"
install -d -o root -g root -m 0700 "$BACKUP_DIR"
ok "$BACKUP_DIR (0700 root:root)"

info "Script de sauvegarde"
rendre_modele "$INCLUS_DIR/scripts/endless-backup.modele" \
	/usr/local/bin/endless-backup 0755 "root:root"

info "Script de mise à jour"
rendre_modele "$INCLUS_DIR/scripts/endless-update.modele" \
	/usr/local/bin/endless-update 0755 "root:root"

info "Unité et timer"
rendre_modele "$INCLUS_DIR/systemd/endless-backup.service.modele" \
	/etc/systemd/system/endless-backup.service 0644 "root:root"
install -m 0644 "$INCLUS_DIR/systemd/endless-backup.timer" \
	/etc/systemd/system/endless-backup.timer

recharger_systemd
systemctl enable --now endless-backup.timer >/dev/null
ok "Timer activé"
systemctl list-timers endless-backup.timer --no-pager || true

# ---------------------------------------------------------------------------
info "Sauvegarde de test"
# Un dispositif de sauvegarde jamais exécuté n'est pas un dispositif de
# sauvegarde : on le déclenche tout de suite.
if systemctl start endless-backup.service; then
	ok "Sauvegarde de test terminée"
	ls -lh "$BACKUP_DIR" | head -10
else
	erreur "La sauvegarde de test a échoué."
	journalctl -u endless-backup -n 30 --no-pager || true
fi

avert "Copiez $BACKUP_DIR hors de la machine — sinon la perte du disque emporte tout."

titre "80 terminé — enchaînez avec 90-premier-admin.sh"
