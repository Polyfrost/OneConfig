# `inclus/` — tout ce qu'il faut pour déployer sur Debian

Ce dossier contient les fichiers réellement déposés sur le serveur : unités
systemd, vhosts nginx, fichiers d'environnement, SQL et scripts d'installation.

Le mode d'emploi complet est dans [`../INSTALLATION-DEBIAN.md`](../INSTALLATION-DEBIAN.md).

---

## Démarrage rapide

```bash
git clone https://github.com/Th3DarkSand8tch/AstralClient.git
cd AstralClient

chmod +x inclus/scripts/*.sh    # le bit d'exécution ne survit pas toujours au clone

cp inclus/config.env.exemple inclus/config.env
chmod 600 inclus/config.env
nano inclus/config.env          # remplissez tous les CHANGEME

sudo bash inclus/scripts/installer-tout.sh
sudo bash inclus/scripts/verifier.sh
```

---

## Contenu

```
inclus/
├── config.env.exemple            Modèle de configuration — TOUT part d'ici
├── lib.sh                        Fonctions communes (chargement, rendu, contrôles)
│
├── env/
│   ├── backend.env.modele        → /opt/endless/secrets/backend.env   (0640 root:endless)
│   ├── minio.default.modele      → /etc/default/minio                 (0600 root:root)
│   └── website.env.local.modele  → plus-website/.env.local
│
├── systemd/
│   ├── endless-backend.service.modele    API Rust
│   ├── endless-render.service.modele     Rendu des vignettes (facultatif)
│   ├── endless-shop.service.modele       Boutique Next.js
│   ├── endless-backup.service.modele     Sauvegarde ponctuelle
│   └── endless-backup.timer              Déclenchement quotidien
│
├── nginx/
│   ├── upgrade-map.conf          → /etc/nginx/conf.d/       (map WebSocket)
│   └── endlessclient.conf.modele → /etc/nginx/sites-available/endlessclient
│
├── journald/
│   └── 99-endless.conf           → /etc/systemd/journald.conf.d/  (plafond 2 Go)
│
├── sql/
│   ├── 00-base.sql.modele        Rôle applicatif
│   └── 10-premier-admin.sql.modele  Promotion du premier admin
│
└── scripts/
    ├── installer-tout.sh         Enchaîne toutes les étapes
    ├── 00-preparer-serveur.sh    Paquets, utilisateur, ufw, journald, clone
    ├── 10-outils.sh              Rust, Node 22, Chromium
    ├── 20-postgresql.sh          Rôle, base, extension pg_trgm
    ├── 30-minio.sh               Serveur, bucket, clé applicative
    ├── 40-render-service.sh      Service de rendu
    ├── 50-backend.sh             Compilation, environnement, démarrage
    ├── 60-frontends.sh           Boutique + tableau de bord + htpasswd
    ├── 70-nginx-tls.sh           DNS, certificats, vhosts
    ├── 80-sauvegardes.sh         endless-backup, endless-update, timer
    ├── 90-premier-admin.sh       Attribution du rôle admin
    ├── 99-patch-clients.sh       Pointe le tableau de bord et PolyPlus sur VOTRE API
    ├── verifier.sh               Contrôle complet, ne modifie rien
    ├── endless-backup.modele     → /usr/local/bin/endless-backup
    └── endless-update.modele     → /usr/local/bin/endless-update
```

---

## Comment fonctionnent les modèles

Les fichiers `*.modele` contiennent des marqueurs `@@NOM@@` remplacés à
l'installation par les valeurs de `config.env`.

Le choix de `@@NOM@@` plutôt que `$NOM` n'est pas cosmétique : les
configurations nginx sont pleines de variables (`$host`, `$remote_addr`,
`$connection_upgrade`) qu'une substitution shell détruirait.

Le rendu échoue bruyamment si un marqueur reste non résolu — pas de fichier de
configuration à moitié rempli déposé en production.

---

## Idempotence

Tous les scripts sont relançables. Ils vérifient l'existant avant d'agir :
un utilisateur déjà créé, un bucket déjà présent ou un certificat déjà émis
sont simplement constatés.

En cas d'échec au milieu du parcours :

```bash
sudo ./inclus/scripts/installer-tout.sh --depuis 50
```

---

## Sécurité

`config.env` contient **tous** les secrets de la production. Il est ignoré par
git (voir [`.gitignore`](.gitignore)) et copié sur le serveur en `0600 root:root`
dans `/opt/endless/secrets/`.

Ne le commitez pas. Ne le passez pas par un canal non chiffré.
