# Installation sur Debian — guide détaillé

Installation complète de la pile Poly+ / EndlessClient sur **un seul serveur
Debian 12 (bookworm) ou 13 (trixie)**, également valable sur Ubuntu 24.04.

Ce document est le pendant francophone et pas-à-pas de
[`DEPLOYMENT.md`](DEPLOYMENT.md). La différence tient en une phrase :

| | [`DEPLOYMENT.md`](DEPLOYMENT.md) | Ce document |
| --- | --- | --- |
| Langue | anglais | français |
| Forme | commandes à copier-coller | commandes **+** explication de chaque décision |
| Fichiers | recopiés dans le texte | livrés dans [`inclus/`](inclus/), prêts à déposer |
| Installation | manuelle | scriptée, idempotente, ou manuelle au choix |

Tout ce qui est décrit ici existe sous forme de fichier dans
[`inclus/`](inclus/). Vous pouvez donc **soit** lancer un script, **soit**
suivre le mode manuel : les deux colonnes produisent exactement le même
résultat.

---

## Sommaire

1. [Ce que vous allez installer](#1-ce-que-vous-allez-installer)
2. [Pré-requis](#2-pré-requis)
3. [Installation express](#3-installation-express)
4. [Le fichier `config.env`](#4-le-fichier-configenv)
5. [Étape 00 — Préparation du serveur](#5-étape-00--préparation-du-serveur)
6. [Étape 10 — Chaînes d'outils](#6-étape-10--chaînes-doutils)
7. [Étape 20 — PostgreSQL](#7-étape-20--postgresql)
8. [Étape 30 — Stockage objet (MinIO)](#8-étape-30--stockage-objet-minio)
9. [Étape 40 — Service de rendu](#9-étape-40--service-de-rendu)
10. [Étape 50 — L'API](#10-étape-50--lapi)
11. [Étape 99 — Pointer les clients sur votre serveur](#11-étape-99--pointer-les-clients-sur-votre-serveur)
12. [Étape 60 — Boutique et tableau de bord](#12-étape-60--boutique-et-tableau-de-bord)
13. [Étape 70 — nginx et TLS](#13-étape-70--nginx-et-tls)
14. [Stripe](#14-stripe)
15. [Étape 80 — Sauvegardes](#15-étape-80--sauvegardes)
16. [Étape 90 — Premier administrateur](#16-étape-90--premier-administrateur)
17. [Vérification finale](#17-vérification-finale)
18. [Le client Minecraft](#18-le-client-minecraft)
19. [Exploitation au quotidien](#19-exploitation-au-quotidien)
20. [Dépannage](#20-dépannage)
21. [Sécurité](#21-sécurité)
22. [Limites connues](#22-limites-connues)
23. [Annexes](#23-annexes)

---

## 1. Ce que vous allez installer

Quatre noms d'hôte publics, cinq services, une base de données.

| Nom d'hôte | Rôle | Composant | Port local |
| --- | --- | --- | --- |
| `endlessclient.dev` | Boutique | `plus-website` (Next.js) | 3000 |
| `api.endlessclient.dev` | API + WebSocket | `plus-backend` (Rust) | 8080 |
| `admin.endlessclient.dev` | Console d'administration | `plus-admin-dashboard` (statique) | — |
| `cdn.endlessclient.dev` | Fichiers des cosmétiques | MinIO | 9000 |

Jamais exposés au réseau public :

| Service | Port |
| --- | --- |
| PostgreSQL | 5432 |
| Service de rendu | 8090 |
| Console MinIO | 9001 |

```
 Minecraft ── PolyPlus ─────────┐
                                │  https + wss
 Navigateur ── endlessclient.dev┼──► api.endlessclient.dev ──► PostgreSQL
            └─ admin.…          │            │
                                │            ├──► MinIO   (cdn.endlessclient.dev)
                                │            ├──► rendu   :8090
                                │            └──► Stripe
                                │                     ▲
                                └─────────────────────┘ webhooks
```

> **`cdn.<votre-domaine>` doit être joignable publiquement.**
> L'API distribue aux clients des URL présignées construites à partir de
> l'adresse S3 qu'on lui a donnée. Si cette adresse est `localhost`, **toutes**
> les images de cosmétiques renvoient 404, dans le navigateur comme en jeu.
> C'est de loin l'erreur de configuration la plus fréquente.

---

## 2. Pré-requis

### Machine

| | Minimum | Confortable |
| --- | --- | --- |
| Processeur | 2 cœurs | 4 cœurs |
| Mémoire | 4 Go | 8 Go |
| Disque | 40 Go SSD | 80 Go SSD |
| Système | Debian 12 | Debian 13 |

La mémoire compte surtout pour **la compilation** : `cargo build --release` sur
cet arbre de dépendances consomme facilement 3 à 4 Go. Sur une machine à 2 Go,
la compilation se fait tuer par le noyau (OOM). Deux solutions : ajouter du
swap, ou compiler ailleurs et ne transférer que le binaire.

Prévoir 15 à 40 minutes pour la première compilation, selon le processeur.

### Réseau et noms

- Un nom de domaine dont vous contrôlez la zone DNS.
- Les ports **80** et **443** joignables depuis Internet (Let's Encrypt valide
  par HTTP).
- Quatre enregistrements `A` (plus les `AAAA` si vous avez de l'IPv6) :

```
endlessclient.dev.        A   203.0.113.10
www.endlessclient.dev.    A   203.0.113.10
api.endlessclient.dev.    A   203.0.113.10
admin.endlessclient.dev.  A   203.0.113.10
cdn.endlessclient.dev.    A   203.0.113.10
```

Contrôlez la propagation **avant** de commencer — certbot échouera sinon :

```bash
for h in endlessclient.dev www.endlessclient.dev api.endlessclient.dev \
         admin.endlessclient.dev cdn.endlessclient.dev; do
  printf '%-28s %s\n' "$h" "$(dig +short "$h" | tr '\n' ' ')"
done
```

### Comptes externes

- **Stripe** — obligatoire. Créer un cosmétique provisionne un produit et un
  prix chez Stripe : sans clé valide, l'API sert le catalogue existant mais
  **ne peut rien y ajouter**. Une clé de test (`sk_test_…`) suffit pour tout
  monter.

### Accès

Un compte avec `sudo`, ou root directement. Tous les scripts se lancent en root.

---

## 3. Installation express

Si vous savez déjà ce que vous faites :

```bash
# En root sur le serveur
apt update && apt -y install git
git clone https://github.com/Th3DarkSand8tch/AstralClient.git /root/astral-src
cd /root/astral-src
chmod +x inclus/scripts/*.sh        # le bit d'exécution ne survit pas toujours au clone

cp inclus/config.env.exemple inclus/config.env
chmod 600 inclus/config.env
nano inclus/config.env              # remplacez TOUS les CHANGEME

bash inclus/scripts/installer-tout.sh   # 30 à 60 min, compilation comprise
bash inclus/scripts/verifier.sh
```

Il reste ensuite **deux opérations manuelles** que rien ne peut automatiser :
déclarer le webhook Stripe (§14) et recopier son secret de signature.

Le reste de ce document explique chaque étape, ce qu'elle fait, comment la
faire à la main, et ce qui peut mal tourner.

---

## 4. Le fichier `config.env`

Tout part de là. Unités systemd, vhosts nginx, fichiers d'environnement, SQL :
tout est engendré à partir de ce seul fichier.

```bash
cp inclus/config.env.exemple inclus/config.env
chmod 600 inclus/config.env
nano inclus/config.env
```

### Les valeurs qu'il faut vraiment fabriquer

Générez chaque secret séparément, ne les réutilisez pas entre eux :

```bash
openssl rand -base64 36    # DB_PASSWORD
openssl rand -base64 36    # MINIO_ROOT_PASSWORD
openssl rand -base64 36    # S3_SECRET_KEY
openssl rand -base64 48    # ADMIN_PASSWORD  — voir ci-dessous
openssl rand -base64 24    # ADMIN_HTTP_PASSWORD
```

`ADMIN_PASSWORD` mérite une attention particulière : il est comparé **en clair,
à chaque requête** `/cosmetics/manage/*`, **sans limitation de débit ni
verrouillage**. C'est une chaîne partagée, pas un mot de passe haché. Prenez-le
long et aléatoire ; ce n'est pas un endroit où faire preuve d'imagination.

### Guillemets et caractères spéciaux

`config.env` est lu par le shell. Les secrets sont livrés **entre guillemets
simples** dans le modèle : gardez-les. Sans eux, un mot de passe contenant `&`,
`$`, `#`, une espace ou un point-virgule casse le chargement de la
configuration — parfois avec un message obscur du type `1: command not found`.

Si votre secret contient lui-même une apostrophe :

```bash
ADMIN_PASSWORD='mot'\''de'\''passe'
```

Le plus simple reste `openssl rand -base64`, qui ne produit rien de dangereux.

Un point est traité pour vous : `DB_PASSWORD` est **encodé pour-cent** avant
d'être inséré dans `DATABASE_URL`. Un mot de passe contenant `@` ou `/`
couperait sinon l'URL au mauvais endroit et le backend chercherait une base
inexistante sur un hôte inexistant.

### Ce que les scripts refusent

`inclus/lib.sh` valide la configuration avant toute action :

- une variable obligatoire absente → **arrêt** ;
- une variable contenant encore `CHANGEME` → **arrêt** ;
- `STRIPE_SECRET` ou `STRIPE_WEBHOOK_SECRET` vide → **avertissement** seulement,
  l'installation continue (on peut vouloir monter la pile avant d'ouvrir un
  compte Stripe) ;
- `ADMIN_PASSWORD` de moins de 24 caractères → avertissement.

### Où il finit

`00-preparer-serveur.sh` en dépose une copie dans
`/opt/endless/secrets/config.env` (`0600 root:root`). Les étapes suivantes
lisent cette copie en priorité. Concrètement : **après modification, relancez
l'étape 00** pour resynchroniser le coffre, puis l'étape concernée.

La référence exhaustive des variables est en [annexe B](#annexe-b--référence-des-variables).

---

## 5. Étape 00 — Préparation du serveur

```bash
sudo ./inclus/scripts/00-preparer-serveur.sh
```

### Ce que fait l'étape

**Paquets.** Chaîne de compilation C (les crates Rust `openssl-sys` et consorts
ont besoin de `libssl-dev` et `pkg-config`), nginx, ufw, PostgreSQL, certbot,
`apache2-utils` pour `htpasswd`, `dnsutils` pour les contrôles DNS.

**Utilisateur de service.** Un compte système `endless`, shell `nologin`,
`$HOME` = `/opt/endless`. Aucun service ne tourne en root.

**Arborescence.**

```
/opt/endless/
├── src/        les sources (dépôt git)
├── bin/        le binaire compilé
├── web/        les fichiers statiques servis par nginx
└── secrets/    0750 root:endless — l'environnement et config.env
```

Le coffre appartient à root et n'est lisible que par le groupe : le service
peut lire son fichier d'environnement, mais un shell compromis sous `endless`
ne peut pas le réécrire.

**Pare-feu.** SSH et HTTPS, rien d'autre :

```bash
ufw allow OpenSSH
ufw allow 'Nginx Full'
ufw --force enable
```

C'est la seule protection réseau du service de rendu (§9), qui écoute sur
toutes les interfaces. N'ouvrez jamais 8090, 8080, 9000, 9001 ni 5432.

**Journal.** Un plafond de 2 Go sur journald
([`inclus/journald/99-endless.conf`](inclus/journald/99-endless.conf)). Sans
lui, un `RUST_LOG=debug` oublié — qui journalise **chaque requête SQL** —
remplit le disque en quelques heures.

**Sources.** Clone (ou `git pull --ff-only`) dans `/opt/endless/src`.

### En manuel

```bash
sudo apt update && sudo apt -y upgrade
sudo apt -y install build-essential pkg-config libssl-dev git curl \
  ca-certificates nginx ufw unzip jq openssl apache2-utils \
  certbot python3-certbot-nginx postgresql postgresql-contrib dnsutils

sudo useradd --system --create-home --home-dir /opt/endless \
  --shell /usr/sbin/nologin endless
sudo mkdir -p /opt/endless/{src,bin,web,secrets}
sudo chown -R endless:endless /opt/endless
sudo chown root:endless /opt/endless/secrets && sudo chmod 750 /opt/endless/secrets

sudo ufw allow OpenSSH && sudo ufw allow 'Nginx Full' && sudo ufw --force enable

sudo install -D -m 0644 inclus/journald/99-endless.conf \
  /etc/systemd/journald.conf.d/99-endless.conf
sudo systemctl restart systemd-journald

sudo -u endless git clone --branch v2 \
  https://github.com/Th3DarkSand8tch/AstralClient.git /opt/endless/src
```

### Contrôle

```bash
id endless
ls -la /opt/endless
sudo ufw status verbose
journalctl --disk-usage
```

---

## 6. Étape 10 — Chaînes d'outils

```bash
sudo ./inclus/scripts/10-outils.sh
```

### Rust

Installé via **rustup**, pour l'utilisateur `endless`, avec
`--default-toolchain none`.

Ce n'est pas un oubli : [`plus-backend-main/rust-toolchain.toml`](plus-backend-main/rust-toolchain.toml)
épingle la version **1.92.0**. Au premier `cargo build`, rustup lit ce fichier
et télécharge exactement cette chaîne. Installer une version « stable »
maintenant ne ferait que consommer 500 Mo pour rien.

Le rustc de Debian (1.63 sur bookworm) est très loin du compte : l'arbre est en
`edition = "2024"`.

### Node.js 22

Depuis le dépôt NodeSource, pas depuis Debian. La boutique est en **Next.js
16.2.9** avec **React 19**, qui exigent Node 20 minimum ; bookworm livre Node 18.

### Chromium

Le moteur du service de rendu. Le script préfère le paquet Debian
(`/usr/bin/chromium`) et **avertit** si seul un snap est disponible : sous
systemd avec `ProtectSystem=strict`, le Chromium en snap se comporte mal en
mode headless.

### En manuel

```bash
sudo -u endless bash -lc '
  curl --proto "=https" --tlsv1.2 -sSf https://sh.rustup.rs \
    | sh -s -- -y --no-modify-path --default-toolchain none
  echo ". \$HOME/.cargo/env" >> ~/.bashrc
'

curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
sudo apt -y install nodejs chromium
```

### Contrôle

```bash
sudo -u endless bash -lc 'rustup --version'
node --version      # v22.x attendu
which chromium
```

---

## 7. Étape 20 — PostgreSQL

```bash
sudo ./inclus/scripts/20-postgresql.sh
```

### Ce que fait l'étape

Un rôle applicatif `endless` (sans privilège superutilisateur), une base
`endless_plus` dont il est propriétaire, et **l'extension `pg_trgm` créée en
amont**.

Ce dernier point est le seul qui demande une explication. La migration
[`m20260717_000000_add_cosmetic_trigram_search`](plus-backend-main/database/migrations/src/m20260717_000000_add_cosmetic_trigram_search.rs)
exécute `CREATE EXTENSION IF NOT EXISTS pg_trgm`, ce qui **exige le
superutilisateur**. Deux options : donner ce privilège au rôle applicatif —
non — ou créer l'extension une bonne fois avec le rôle `postgres`. La migration
la trouve alors déjà en place et passe sans rien demander.

Le script vérifie ensuite que `listen_addresses` vaut bien `localhost` (défaut
Debian) et **teste la connexion applicative** : mieux vaut découvrir un
`pg_hba.conf` récalcitrant maintenant que dans les logs du backend.

### En manuel

```bash
sudo -u postgres psql <<'SQL'
CREATE ROLE endless LOGIN PASSWORD 'VOTRE_MOT_DE_PASSE';
CREATE DATABASE endless_plus OWNER endless;
SQL

sudo -u postgres psql -d endless_plus -c 'CREATE EXTENSION IF NOT EXISTS pg_trgm;'
sudo -u postgres psql -d endless_plus -c '\dx'
```

### Contrôle

```bash
sudo -u postgres psql -c "SHOW listen_addresses;"          # localhost
PGPASSWORD='…' psql -h 127.0.0.1 -U endless -d endless_plus -c 'SELECT 1;'
```

---

## 8. Étape 30 — Stockage objet (MinIO)

```bash
sudo ./inclus/scripts/30-minio.sh
```

### Ce que fait l'étape

Installe le serveur MinIO et le client `mc`, crée le service, le bucket, et
**une clé d'accès dédiée à l'API** — le compte racine ne sert qu'à
l'administration.

Le service n'écoute que sur la boucle locale
([`inclus/env/minio.default.modele`](inclus/env/minio.default.modele)) :

```
MINIO_OPTS="--address 127.0.0.1:9000 --console-address 127.0.0.1:9001"
```

L'accès public passe uniquement par nginx sur `cdn.<domaine>`. La console
d'administration n'est jamais publiée ; pour y accéder, ouvrez un tunnel :

```bash
ssh -L 9001:127.0.0.1:9001 utilisateur@serveur
# puis http://127.0.0.1:9001 dans votre navigateur
```

### Le bucket reste privé

Les navigateurs et le jeu lisent les fichiers via des **URL présignées**
engendrées par l'API : c'est la signature qui autorise la lecture, pas une
politique publique. Ne passez jamais le bucket en accès anonyme — vous
exposeriez l'intégralité du catalogue en lecture directe.

Le script vérifie ce point et le signale s'il détecte une politique anonyme.

### En manuel

```bash
ARCH=$(dpkg --print-architecture)
curl -fsSLo /tmp/minio.deb "https://dl.min.io/server/minio/release/linux-$ARCH/minio.deb"
sudo dpkg -i /tmp/minio.deb
curl -fsSLo /tmp/mc "https://dl.min.io/client/mc/release/linux-$ARCH/mc"
sudo install -m 0755 /tmp/mc /usr/local/bin/mc

sudo useradd --system --home-dir /var/lib/minio --shell /usr/sbin/nologin minio-user
sudo install -d -o minio-user -g minio-user -m 0750 /var/lib/minio

# /etc/default/minio : voir inclus/env/minio.default.modele
sudo systemctl enable --now minio

mc alias set local http://127.0.0.1:9000 endless-root VOTRE_MDP_ROOT
mc mb --ignore-existing local/endless-cosmetics
mc admin user add local endless-api VOTRE_SECRET_S3
mc admin policy attach local readwrite --user endless-api
```

### Contrôle

```bash
systemctl status minio --no-pager
mc admin user info local endless-api
mc ls local/endless-cosmetics
```

---

## 9. Étape 40 — Service de rendu

```bash
sudo ./inclus/scripts/40-render-service.sh
```

### À quoi il sert

Il fabrique les images de couverture des cosmétiques en pilotant un Chromium
headless (skinview3d). **Il est facultatif** : sans lui, les téléversements
aboutissent normalement, ils n'enregistrent simplement aucune couverture.

### Deux détails qui comptent

**`PUPPETEER_SKIP_DOWNLOAD=true`.** Sans cette variable, `npm ci` télécharge un
second Chromium (~150 Mo) dans `node_modules`, alors que le système en a déjà un.

**La texture par défaut.** `node scripts/fetch-default-skin.mjs` télécharge la
peau « Steve » dans `assets/default-skin.png`. Sans ce fichier, le service
retombe à chaque rendu sur un appel réseau vers `textures.minecraft.net` : lent,
et cassé net si la machine n'a pas d'accès sortant.

### Ce que la sécurité ne couvre pas

Le service fait `server.listen(PORT)` **sans adresse d'écoute** : il est donc
joignable sur *toutes* les interfaces, contrairement aux autres composants.
Rien ne le protège du réseau public à part **ufw**.

C'est une divergence assumée avec la liste de contrôle de
[`DEPLOYMENT.md`](DEPLOYMENT.md), qui affirme que le service de rendu n'écoute
que sur `127.0.0.1` — ce n'est pas le cas dans le code actuel. Vérifiez :

```bash
sudo ss -lntp | grep 8090        # attendu : *:8090, et non 127.0.0.1:8090
sudo ufw status | grep 8090      # attendu : aucune ligne
```

Durcir davantage est possible (`IPAddressDeny=any` + `IPAddressAllow=localhost`
dans l'unité), mais cela coupe aussi le repli vers `textures.minecraft.net`.
À ne faire que si la texture par défaut est bien présente sur disque.

### En manuel

```bash
sudo -u endless bash -lc '
  cd /opt/endless/src/plus-backend-main/render-service
  PUPPETEER_SKIP_DOWNLOAD=true npm ci
  node scripts/fetch-default-skin.mjs
'
sudo install -m 0644 <unité rendue> /etc/systemd/system/endless-render.service
sudo systemctl daemon-reload && sudo systemctl enable --now endless-render
```

### Contrôle

```bash
curl -fsS http://127.0.0.1:8090/ && echo      # attendu : ok
journalctl -u endless-render -n 30 --no-pager
```

---

## 10. Étape 50 — L'API

```bash
sudo ./inclus/scripts/50-backend.sh
```

C'est l'étape longue, et celle où la plupart des installations achoppent.

### Compilation

```bash
sudo -u endless bash -lc '
  . $HOME/.cargo/env
  cd /opt/endless/src/plus-backend-main
  cargo build --release
'
```

15 à 40 minutes la première fois. Si le processus se fait tuer sans message,
c'est le tueur de mémoire du noyau : ajoutez du swap.

```bash
sudo fallocate -l 4G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
```

Le binaire est ensuite installé dans `/opt/endless/bin/plus-backend`. Le script
**arrête le service avant de remplacer le fichier** : on n'écrase pas un
exécutable en cours d'utilisation, et deux instances face à la même base
pendant une migration corrompraient l'état.

### Le fichier d'environnement

Engendré depuis [`inclus/env/backend.env.modele`](inclus/env/backend.env.modele)
vers `/opt/endless/secrets/backend.env`, en `0640 root:endless`.

Quatre points méritent qu'on s'y arrête.

**1. Les quatre variables `STRIPE_*` sont obligatoires.**
Elles n'ont aucune valeur par défaut dans
[`src/commands/mod.rs`](plus-backend-main/src/commands/mod.rs). Une valeur
**vide est acceptée**, une variable **absente fait refuser le démarrage** — et
le processus sort si vite qu'il ne journalise parfois rien du tout. Si le
backend meurt sans laisser de trace, c'est presque toujours ça.

**2. `S3_BUCKET_ENDPOINT` doit être le nom d'hôte public.**
`https://cdn.<domaine>`, jamais `localhost`. Les URL présignées sont
construites à partir de cette valeur et distribuées telles quelles aux
navigateurs et au jeu.

**3. `BIND_ADDR=127.0.0.1:8080`.**
Sans cette variable, le binaire écoute par défaut sur `[::]:8080` **et**
`0.0.0.0:8080` — c'est-à-dire publiquement. nginx doit rester le seul point
d'entrée.

**4. `CLIENT_IP_SOURCE=RightmostXForwardedFor` va de pair avec nginx.**
Ce réglage lit la *dernière* valeur de `X-Forwarded-For`. Il n'est sûr que
parce que le vhost **écrase** cet en-tête :

```nginx
proxy_set_header X-Forwarded-For $remote_addr;
```

Avec un `$proxy_add_x_forwarded_for` classique, un en-tête forgé par le client
survivrait et n'importe qui pourrait usurper une adresse IP dans les
statistiques. **Changer l'un des deux sans l'autre casse la sécurité ou les
analyses.**

### Migrations

Elles s'exécutent au démarrage, **avant** l'ouverture du socket. Le journal se
termine par :

```
Server listening on 127.0.0.1:8080
```

L'unité prévoit `TimeoutStartSec=600` : sur une grosse base, une montée de
version peut prendre plusieurs minutes et systemd ne doit pas tuer le processus
en pleine migration.

### Contrôle

```bash
systemctl status endless-backend --no-pager
journalctl -u endless-backend -n 40 --no-pager
curl -fsS http://127.0.0.1:8080/cosmetics | jq
```

---

## 11. Étape 99 — Pointer les clients sur votre serveur

```bash
sudo ./inclus/scripts/99-patch-clients.sh
```

**À lancer avant l'étape 60**, qui compile le tableau de bord.

Deux composants embarquent des adresses en dur vers l'infrastructure Polyfrost.
Tant qu'ils ne sont pas corrigés, **ils ne parlent pas à votre serveur**.

### Le tableau de bord

[`plus-admin-dashboard/src/lib/settings.ts`](plus-admin-dashboard/src/lib/settings.ts) :

```ts
export const ENV_OPTIONS = [
    { label: "Local",      value: "http://127.0.0.1:8080" },
    { label: "Staging",    value: "https://plus-staging.polyfrost.org" },
    { label: "Production", value: "https://plus.polyfrost.org" },
] as const;

export const DEFAULT_ENV = ENV_OPTIONS[0].value;
```

Le script insère votre serveur **en première position**. Comme `DEFAULT_ENV`
vaut `ENV_OPTIONS[0]`, il devient l'environnement par défaut du menu déroulant.

### PolyPlus

[`PolyPlus/src/main/kotlin/org/polyfrost/polyplus/BackendUrl.kt`](PolyPlus/src/main/kotlin/org/polyfrost/polyplus/BackendUrl.kt) :

```kotlin
enum class BackendUrl(val url: String) {
    PRODUCTION("https://plus.polyfrost.org"),
    STAGING("https://plus-staging.polyfrost.org"),
    LOCAL("http://localhost:8080");
}
```

Le script réécrit `PRODUCTION` vers `https://api.<votre-domaine>`.

Côté joueur, la valeur retenue vient d'un menu « API URL » persisté par
instance dans `config/polyplus.json` :

```json
"apiUrl": { "class": "org.polyfrost.polyplus.BackendUrl", "value": "PRODUCTION" }
```

Une copie `.orig` est conservée à côté de chaque fichier modifié, et le script
restaure l'original si le motif attendu n'est pas trouvé.

> Voir aussi §18 et §22 : PolyPlus ne se compile pas en l'état contre le
> OneConfig de ce dépôt.

---

## 12. Étape 60 — Boutique et tableau de bord

```bash
sudo ./inclus/scripts/60-frontends.sh
```

### Boutique (Next.js)

`NEXT_PUBLIC_BACKEND_URL` est **inliné dans le bundle au moment du build**. La
modifier après coup ne change rien : il faut recompiler. C'est la cause n°1 de
« la boutique tape encore sur l'ancienne API ».

Le script écrit donc `.env.local` **avant** `npm run build`.

L'unité tourne `npm run start`, avec deux réglages nécessités par
`ProtectSystem=strict` :

```ini
Environment=NPM_CONFIG_CACHE=/tmp/npm-cache
ReadWritePaths=/opt/endless/src/plus-website/.next
```

`/opt` est en lecture seule pour le service ; sans le cache déporté, le moindre
log npm ferait échouer le démarrage, et sans `ReadWritePaths` le cache de pages
et d'images de Next.js ne pourrait pas s'écrire.

> `next.config.ts` déclare `output: "standalone"`. On peut donc aussi servir
> `.next/standalone/server.js` directement, ce qui évite `npm` au démarrage —
> mais il faut alors recopier `public/` et `.next/static/` dans
> `.next/standalone/`. Le mode `npm run start` retenu ici est plus simple et
> équivalent.

### Tableau de bord (Vite)

Bundle statique, servi par nginx — pas de service. Compilé, puis publié dans
`/opt/endless/web/admin`.

### L'authentification HTTP n'est pas facultative

Le champ mot de passe du tableau de bord protège **l'API**, pas la page. Sans
`auth_basic` devant, n'importe qui charge la console d'administration complète
et n'a plus qu'à deviner `ADMIN_PASSWORD` — sans limitation de débit (§21).

```bash
sudo htpasswd -cbB /etc/nginx/endless-admin.htpasswd admin 'MOT_DE_PASSE'
sudo chown root:www-data /etc/nginx/endless-admin.htpasswd
sudo chmod 640 /etc/nginx/endless-admin.htpasswd
```

### Contrôle

```bash
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:3000/    # 200
ls /opt/endless/web/admin/index.html
```

---

## 13. Étape 70 — nginx et TLS

```bash
sudo ./inclus/scripts/70-nginx-tls.sh
```

### L'ordre est imposé

Les certificats doivent exister **avant** l'installation du vhost : sinon
`nginx -t` échoue sur des `ssl_certificate` introuvables. Le script contrôle
d'abord le DNS, obtient les certificats, puis seulement dépose la
configuration.

Il compare aussi chaque résolution à l'IP publique du serveur et refuse de
continuer si un nom ne résout pas du tout.

### La map WebSocket

[`inclus/nginx/upgrade-map.conf`](inclus/nginx/upgrade-map.conf) va dans
`conf.d/` parce qu'une directive `map` doit se trouver au niveau `http{}` :

```nginx
map $http_upgrade $connection_upgrade {
    default upgrade;
    ''      close;
}
```

Sans elle, `/websocket` est proxifié en HTTP simple. Le symptôme : le jeu se
reconnecte en boucle, les changements d'équipement ne remontent jamais et le
temps de jeu n'est pas comptabilisé.

Le contrôle qui tranche :

```bash
curl -s -o /dev/null -w '%{http_code}\n' \
  -H 'Connection: Upgrade' -H 'Upgrade: websocket' \
  -H 'Sec-WebSocket-Version: 13' -H 'Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==' \
  https://api.endlessclient.dev/websocket
```

**101** ou **426** : correct. **200** : la map manque.

### Les cinq blocs `server`

Le fichier rendu depuis
[`inclus/nginx/endlessclient.conf.modele`](inclus/nginx/endlessclient.conf.modele)
contient :

| Bloc | Particularité |
| --- | --- |
| `:80` | redirection, sauf `/.well-known/acme-challenge/` |
| API | `client_max_body_size 64m`, en-têtes Upgrade, `X-Forwarded-For` **écrasé** |
| Boutique | proxy simple vers `127.0.0.1:3000` |
| `www` | redirection 301 vers l'apex |
| Admin | `auth_basic` + racine statique + repli SPA sur `index.html` |
| CDN | `proxy_buffering off` et `proxy_request_buffering off` |

Ce dernier point : la signature d'une URL présignée couvre la requête **telle
qu'envoyée**. Toute réécriture liée au tampon la casse, et MinIO répond
`403 SignatureDoesNotMatch`.

### Renouvellement

Installé par le paquet certbot. Le script le teste :

```bash
sudo certbot renew --dry-run
systemctl list-timers | grep certbot
```

---

## 14. Stripe

**Cette partie est manuelle** : rien ne peut la scripter à votre place.

### Le webhook

Dans le tableau de bord Stripe, ajoutez un point de terminaison :

```
https://api.<votre-domaine>/stripe/webhook
```

Abonnez-le à **exactement ces trois événements** — tous les autres sont ignorés
par l'API :

```
checkout.session.completed
checkout.session.async_payment_succeeded
charge.refunded
```

### Le secret de signature

Recopiez-le dans `STRIPE_WEBHOOK_SECRET` (`config.env`), puis :

```bash
sudo ./inclus/scripts/00-preparer-serveur.sh   # resynchronise le coffre
sudo ./inclus/scripts/50-backend.sh            # régénère backend.env et redémarre
```

### Pourquoi c'est critique

Un mauvais secret échoue **de la pire manière possible** : les paiements
aboutissent et les cosmétiques ne sont jamais attribués. Rien ne casse
visiblement — les clients paient et ne reçoivent rien.

Envoyez un événement de test depuis Stripe et observez :

```bash
sudo journalctl -u endless-backend -f | grep -i webhook
```

### Clés de test

Gardez `sk_test_…` tant que vous n'encaissez pas réellement. Toute la pile
fonctionne en mode test, cosmétiques compris.

---

## 15. Étape 80 — Sauvegardes

```bash
sudo ./inclus/scripts/80-sauvegardes.sh
```

### Base **et** bucket, ensemble

Les lignes de cosmétiques référencent des clés d'objets. Restaurer l'un sans
l'autre laisse des cosmétiques dont les fichiers renvoient 404 — et **l'API ne
bronche pas** : elle journalise un avertissement par asset au démarrage et
continue.

`/usr/local/bin/endless-backup` fait donc les deux :

```bash
sudo -u postgres pg_dump -Fc endless_plus > "$DEST/endless_plus-$HORODATAGE.dump"
mc mirror --overwrite --remove local/endless-cosmetics "$DEST/bucket"
```

Rotation à 14 jours par défaut (`BACKUP_RETENTION_DAYS`).

Le timer est quotidien, avec `Persistent=true` (rattrapage si la machine était
éteinte) et `RandomizedDelaySec=30m`.

Le script **exécute une sauvegarde de test immédiatement** : un dispositif de
sauvegarde jamais lancé n'est pas un dispositif de sauvegarde.

### Ce qui n'est pas sauvegardé

Les **produits et prix Stripe** vivent chez Stripe et ne sont dans aucune des
deux sauvegardes. Une restauration de base sans les produits correspondants
laisse des cosmétiques inachetables.

### Sortez les copies de la machine

```bash
# Exemple avec rsync vers une machine distante
rsync -az --delete /var/backups/endless/ sauvegarde@ailleurs:/srv/endless/
```

Une sauvegarde sur le même disque n'est pas une sauvegarde.

### Restauration

```bash
sudo systemctl stop endless-backend
sudo -u postgres dropdb endless_plus
sudo -u postgres createdb -O endless endless_plus
sudo -u postgres psql -d endless_plus -c 'CREATE EXTENSION IF NOT EXISTS pg_trgm;'
sudo -u postgres pg_restore -d endless_plus /var/backups/endless/endless_plus-HORODATAGE.dump
mc mirror --overwrite /var/backups/endless/bucket local/endless-cosmetics
sudo systemctl start endless-backend
```

Répétez cette procédure une fois, à froid, avant d'en avoir besoin.

---

## 16. Étape 90 — Premier administrateur

```bash
sudo ./inclus/scripts/90-premier-admin.sh
```

**Après** le premier démarrage réussi du backend : ce sont ses migrations qui
créent la table `user`.

Récupérez votre UUID Minecraft :

```bash
curl -s https://api.mojang.com/users/profiles/minecraft/VOTREPSEUDO | jq -r .id
```

L'API Mojang le renvoie **sans tirets** ; la colonne est de type `uuid` et les
exige. Le script les réinsère automatiquement.

Rôles disponibles : `player`, `moderator`, `admin`.

```sql
INSERT INTO "user" (minecraft_uuid, role)
VALUES ('votre-uuid-avec-tirets', 'admin')
ON CONFLICT (minecraft_uuid) DO UPDATE SET role = 'admin';
```

---

## 17. Vérification finale

```bash
sudo ./inclus/scripts/verifier.sh
```

Le script ne modifie rien et sort en erreur si un contrôle échoue. Il couvre :

- l'état des six services ;
- les cinq écoutes locales attendues ;
- `/cosmetics` et `/openapi.json` en local ;
- les quatre noms d'hôte en HTTPS ;
- la bascule WebSocket (101/426, jamais 200) ;
- l'authentification d'administration (401 sans en-tête, autre chose avec) ;
- les permissions de `backend.env` et la valeur de `S3_BUCKET_ENDPOINT` ;
- ufw, y compris l'absence de port interne ouvert ;
- l'expiration du certificat ;
- la présence d'un dump récent.

### À la main

```bash
systemctl is-active endless-backend endless-render endless-shop minio postgresql nginx

curl -fsS https://api.endlessclient.dev/openapi.json | jq '.paths | keys | length'
curl -fsS https://api.endlessclient.dev/cosmetics    | jq '.cosmetics | length'

curl -s -o /dev/null -w 'boutique %{http_code}\n' https://endlessclient.dev/
curl -s -o /dev/null -w 'admin    %{http_code}\n' https://admin.endlessclient.dev/   # 401
```

Documentation d'API navigable : `https://api.<votre-domaine>/scalar`.

---

## 18. Le client Minecraft

Deux mods distincts, souvent confondus :

- **OneConfig** ([`modules/`](modules/), [`minecraft/`](minecraft/)) — le socle
  de configuration et de HUD. Il ne contient **aucun code de cosmétiques** : le
  recompiler ne change jamais le magasin en jeu.
- **PolyPlus** ([`PolyPlus/`](PolyPlus/)) — le mod des cosmétiques : magasin,
  garde-robe, emotes, et la liaison avec cette API.

Pour livrer un client qui parle à votre serveur, il faut modifier
`BackendUrl.kt` (voir §11) **et** recompiler PolyPlus.

> **Ce n'est pas possible en l'état.** PolyPlus épingle
> `deps.oneconfig = "1.1.4"` dans
> [`PolyPlus/stonecutter.properties.toml`](PolyPlus/stonecutter.properties.toml)
> alors que ce dépôt est en `1.1.7-dev`
> ([`gradle.properties`](gradle.properties)), et cette version a supprimé l'API
> qu'utilisent ses écrans. Voir §22.

---

## 19. Exploitation au quotidien

### Journaux

Tout part dans journald :

```bash
sudo journalctl -u endless-backend -f
sudo journalctl -u endless-render  -n 100 --no-pager
sudo journalctl -u endless-shop    --since '1 hour ago'
sudo journalctl -u endless-backup  --since yesterday
```

Niveau de production :

```
RUST_LOG=info,sea_orm=warn,sqlx=warn
```

`RUST_LOG=debug` journalise **chaque requête SQL**. Utile dix minutes, ingérable
au-delà.

### Mise à jour

```bash
sudo endless-update
```

Ce script — installé par l'étape 80 — enchaîne : sauvegarde, `git pull`,
recompilation, arrêt du backend, remplacement du binaire, redémarrage, rebuild
des deux interfaces.

L'ordre n'est pas négociable : les migrations s'appliquent au démarrage, donc
**jamais deux instances contre la même base pendant un déploiement**. Arrêter,
remplacer, démarrer.

Les migrations ne sont pas réversibles en place : d'où le dump systématique en
tête de script.

### Redémarrages ciblés

```bash
sudo systemctl restart endless-backend      # après un changement d'environnement
sudo systemctl restart endless-shop         # après un rebuild de la boutique
sudo systemctl reload  nginx                # après un changement de vhost
```

### Changer un secret

1. éditer `inclus/config.env` ;
2. `sudo ./inclus/scripts/00-preparer-serveur.sh` (resynchronise le coffre) ;
3. relancer l'étape concernée (`50-backend.sh` pour l'API, `30-minio.sh` pour S3…).

---

## 20. Dépannage

| Symptôme | Cause | Correction |
| --- | --- | --- |
| Le backend sort aussitôt, sans journal | Une variable `STRIPE_*` est **absente** | Les quatre doivent exister ; vide est accepté, absente non |
| `database "…" does not exist` | `DATABASE_URL` erroné | Vérifier nom, utilisateur, mot de passe |
| La migration échoue sur `CREATE EXTENSION` | Le rôle n'est pas superutilisateur | `CREATE EXTENSION pg_trgm` en tant que `postgres` (étape 20) |
| Images de cosmétiques en 404 partout | `S3_BUCKET_ENDPOINT` n'est pas public | `https://cdn.<domaine>` |
| `403 SignatureDoesNotMatch` sur le CDN | nginx tamponne les requêtes | `proxy_request_buffering off` **et** `proxy_buffering off` |
| La boutique s'affiche, catalogue vide | Seuls les cosmétiques **activés et pourvus d'un prix** sont listés | Donner un `base_price` à chacun |
| Erreur CORS dans la console | Origine absente de `CORS_ORIGINS` | L'ajouter, relancer l'étape 50 |
| Téléversement en 502 avec un message Stripe | `STRIPE_SECRET` invalide ou absent | Utiliser une clé valide |
| Téléversement en 401 | Mauvais mot de passe admin | En-tête `Authorization` **brut**, sans `Bearer` |
| Les achats n'attribuent rien | `STRIPE_WEBHOOK_SECRET` erroné | Recopier depuis le point de terminaison, relancer 50 |
| Le magasin en jeu montre un autre catalogue | PolyPlus pointe ailleurs | Menu « API URL », ou recompiler `BackendUrl.kt` (§11) |
| Le WebSocket se reconnecte en boucle | La map `Upgrade` manque dans nginx | Étape 70 ; le test doit rendre 101 ou 426 |
| Toutes les analyses montrent une seule IP | `CLIENT_IP_SOURCE` non défini | `RightmostXForwardedFor` + nginx qui **écrase** `X-Forwarded-For` |
| Aucune couverture n'est engendrée | Service de rendu arrêté ou injoignable | `curl 127.0.0.1:8090` ; `journalctl -u endless-render` |
| `cargo build` tué sans message | Mémoire insuffisante (OOM) | Ajouter du swap (§10) |
| `nginx -t` échoue sur `ssl_certificate` | Vhost installé avant les certificats | Lancer certbot d'abord (étape 70 le fait dans le bon ordre) |
| Le tableau de bord ne propose pas votre API | `settings.ts` non corrigé | Étape 99, **puis** rebuild (étape 60) |
| La boutique appelle l'ancienne API | `NEXT_PUBLIC_BACKEND_URL` est inliné au build | Réécrire `.env.local` **et** recompiler |
| `endless-shop` ne démarre pas, erreur d'écriture | `ProtectSystem=strict` | Vérifier `NPM_CONFIG_CACHE` et `ReadWritePaths` dans l'unité |

### Réflexes

```bash
# Que dit le service ?
sudo journalctl -u endless-backend -n 100 --no-pager

# Qui écoute où ?
sudo ss -lntp

# La configuration lue par le service
sudo systemctl show endless-backend -p EnvironmentFiles
sudo cat /opt/endless/secrets/backend.env

# Contrôle global
sudo ./inclus/scripts/verifier.sh
```

---

## 21. Sécurité

Liste à parcourir avant d'ouvrir au public.

- [ ] `ADMIN_PASSWORD` long et aléatoire. Il est comparé **en clair** sur chaque
      `/cosmetics/manage/*`, **sans limitation de débit ni verrouillage**.
- [ ] `admin.<domaine>` est bien derrière `auth_basic`. Le champ mot de passe de
      la page protège l'API, pas la page.
- [ ] Les clés Stripe sont des clés **de test** tant que vous n'encaissez pas.
- [ ] `/opt/endless/secrets/backend.env` est en `0640 root:endless`.
- [ ] `config.env` n'est **pas** dans git (`inclus/.gitignore` s'en charge).
- [ ] `CLIENT_IP_SOURCE` correspond à nginx, et nginx **écrase**
      `X-Forwarded-For`.
- [ ] PostgreSQL, MinIO et l'API écoutent sur `127.0.0.1`.
- [ ] Le service de rendu écoute partout — **ufw** est sa seule protection.
      Vérifiez qu'aucune règle n'ouvre 8090.
- [ ] `ufw` n'autorise que SSH et HTTPS.
- [ ] Le bucket n'est **pas** anonyme ; les lectures passent par des URL
      présignées.
- [ ] `CORS_ORIGINS` liste exactement vos origines, rien de plus.
- [ ] Le renouvellement des certificats a été testé (`certbot renew --dry-run`).
- [ ] Les sauvegardes sont copiées hors de la machine **et** une restauration a
      été répétée.

Contrôle rapide :

```bash
sudo ss -lntp | grep -E '8080|8090|9000|9001|5432'   # tout doit être en 127.0.0.1, sauf 8090
sudo ufw status verbose
stat -c '%a %U:%G' /opt/endless/secrets/backend.env  # 640 root:endless
```

---

## 22. Limites connues

Ce ne sont pas des bogues d'installation mais des propriétés du logiciel. Les
connaître évite d'y passer une soirée.

**Pas de cosmétique sans Stripe.** Le téléversement provisionne un produit et un
prix : une installation sans clé valide sert un catalogue existant mais ne peut
rien y ajouter.

**La suppression est logique, pas physique.** `/cosmetics/manage/delete`
désactive le cosmétique (le groupe entier s'il est groupé). Les lignes, les
fichiers et les produits Stripe restent. Rien dans l'API ne supprime réellement.

**Les remboursements partiels ne sont pas gérés.** Seul un `charge.refunded`
complet retire la propriété.

**PolyPlus ne se compile pas contre le OneConfig de ce dépôt.** Il épingle
`deps.oneconfig = "1.1.4"` alors que l'arbre est en `1.1.7-dev`, et cette
version a retiré l'API qu'utilisent ses écrans : `ComposeScreen` ne prend plus
de `RenderMode`, `Theme` ne prend plus `pixelGrid` / `designWidth` /
`designHeight`, et `pixelGridScale` a disparu. Le portage impose de renoncer au
repaint continu et à l'alignement sur la grille de pixels, ce qui change le
rendu de la roue d'emotes et des menus.

**`plus-backend-main/scripts/populate-db.sql` est périmé.** Il vise les tables
`emote`, `player_owned_emote` et `emote_package`, supprimées par la migration
`m20260704_000004_drop_emotes`. Réservé au développement — **ne le lancez jamais
sur une base réelle**.

**`backend-1` n'est pas un serveur maven.** Il lit des métadonnées maven et
annonce des URL de téléchargement ; il ne peut ni servir d'artefacts ni servir
de dépôt Gradle.

---

## 23. Annexes

### Annexe A — Arborescence de `inclus/`

Voir [`inclus/README.md`](inclus/README.md) pour le détail commenté.

```
inclus/
├── config.env.exemple    → à copier en config.env
├── lib.sh                → fonctions communes
├── env/                  → fichiers d'environnement
├── systemd/              → unités et timer
├── nginx/                → map WebSocket et vhosts
├── journald/             → plafond du journal
├── sql/                  → rôle et premier admin
└── scripts/              → 00 à 99 + verifier.sh
```

### Annexe B — Référence des variables

| Variable | Obligatoire | Rôle |
| --- | --- | --- |
| `DOMAIN` | oui | Domaine racine ; les sous-domaines en dérivent |
| `DOMAIN_API` / `DOMAIN_ADMIN` / `DOMAIN_CDN` / `DOMAIN_WWW` | non | Surcharges si vos noms diffèrent de `api.` / `admin.` / `cdn.` / `www.` |
| `CERTBOT_EMAIL` | oui | Avis d'expiration Let's Encrypt |
| `SERVICE_USER` | non (`endless`) | Compte système des services |
| `INSTALL_ROOT` | non (`/opt/endless`) | Racine d'installation |
| `GIT_REPO` / `GIT_BRANCH` | non | Source clonée |
| `DB_NAME` / `DB_USER` / `DB_PASSWORD` | oui | PostgreSQL |
| `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` | oui | Compte d'administration MinIO |
| `S3_BUCKET` / `S3_REGION` | non | Bucket des cosmétiques |
| `S3_ACCESS_KEY` / `S3_SECRET_KEY` | oui | Clé applicative dédiée |
| `ADMIN_PASSWORD` | oui | En-tête `Authorization` des routes d'administration |
| `RUST_LOG` | non | Verbosité de l'API |
| `STRIPE_SECRET` | recommandé | Sans elle, aucun cosmétique ne peut être créé |
| `STRIPE_WEBHOOK_SECRET` | recommandé | Sans elle, aucun achat n'attribue de cosmétique |
| `STRIPE_SUCCESS_URL` / `STRIPE_CANCEL_URL` | non | Dérivées de `DOMAIN` |
| `ADMIN_HTTP_USER` / `ADMIN_HTTP_PASSWORD` | oui | `auth_basic` devant le tableau de bord |
| `MINECRAFT_UUID` | pour l'étape 90 | Votre UUID, tirets facultatifs |
| `BACKEND_PORT` / `RENDER_PORT` / `SHOP_PORT` / `MINIO_PORT` / `MINIO_CONSOLE_PORT` | non | Ports internes |
| `BACKUP_DIR` / `BACKUP_RETENTION_DAYS` | non | Sauvegardes |

### Annexe C — Emplacements sur le serveur

| Chemin | Contenu |
| --- | --- |
| `/opt/endless/src` | Sources (dépôt git) |
| `/opt/endless/bin/plus-backend` | Binaire de l'API |
| `/opt/endless/web/admin` | Tableau de bord compilé |
| `/opt/endless/secrets/backend.env` | Environnement de l'API — `0640 root:endless` |
| `/opt/endless/secrets/config.env` | Configuration maîtresse — `0600 root:root` |
| `/etc/systemd/system/endless-*.service` | Unités |
| `/etc/systemd/system/endless-backup.timer` | Timer de sauvegarde |
| `/etc/default/minio` | Configuration MinIO — `0600 root:root` |
| `/etc/nginx/conf.d/upgrade-map.conf` | Map WebSocket |
| `/etc/nginx/sites-available/endlessclient` | Vhosts |
| `/etc/nginx/endless-admin.htpasswd` | Auth HTTP du tableau de bord |
| `/etc/systemd/journald.conf.d/99-endless.conf` | Plafond du journal |
| `/usr/local/bin/endless-backup` | Script de sauvegarde |
| `/usr/local/bin/endless-update` | Script de mise à jour |
| `/var/lib/minio` | Données du stockage objet |
| `/var/backups/endless` | Sauvegardes |

### Annexe D — Services

| Unité | Fournit | Écoute | Facultatif |
| --- | --- | --- | --- |
| `postgresql` | Base de données | `127.0.0.1:5432` | non |
| `minio` | Stockage objet | `127.0.0.1:9000`, `127.0.0.1:9001` | non |
| `endless-backend` | API + WebSocket | `127.0.0.1:8080` | non |
| `endless-render` | Vignettes | `*:8090` (protégé par ufw) | **oui** |
| `endless-shop` | Boutique | `127.0.0.1:3000` | non |
| `nginx` | TLS et routage | `:80`, `:443` | non |
| `endless-backup.timer` | Sauvegarde quotidienne | — | non |

### Annexe E — Documents liés

- [`DEPLOYMENT.md`](DEPLOYMENT.md) — la version anglaise, plus concise
- [`inclus/README.md`](inclus/README.md) — index commenté des fichiers livrés
- [`README_WINDOWS.md`](README_WINDOWS.md) et [`startlocal/`](startlocal/) —
  environnement de développement sous Windows
- [`SECURITY.md`](SECURITY.md) — signalement de vulnérabilités
