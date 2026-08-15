#!/usr/bin/env bash
# Downloads capes and packages PolyCosmetics emotes into the local S3 bucket.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
bucket="$root/.local/s3/local"
polycosmetics="${POLYCOSMETICS_ROOT:-$root/../PolyCosmetics}"
capes_json="$root/scripts/dev-capes.json"

if [ ! -d "$bucket" ]; then
	echo "error: S3 bucket directory $bucket does not exist (run start-dev-env.sh first)" >&2
	exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
	echo "error: jq is required" >&2
	exit 1
fi

mkdir -p "$bucket/capes" "$bucket/emotes"

cape_name() {
	case "$1" in
		1) echo oneclient ;;
		2) echo oneconfig ;;
		3) echo onelauncher ;;
		4) echo poly ;;
		5) echo moon ;;
		*) echo "cape$1" ;;
	esac
}

while IFS=$'\t' read -r id url; do
	name="$(cape_name "$id")"
	dest="$bucket/capes/${name}.png"
	if [ -f "$dest" ]; then
		echo "cape already present: $name"
		continue
	fi
	echo "downloading cape: $name"
	if ! curl -fsSL "$url" -o "$dest"; then
		echo "warning: failed to download cape $name from $url (skipping)" >&2
		rm -f "$dest"
	fi
done < <(jq -r '.cosmetics[] | select(.type=="cape") | [.id, .url] | @tsv' "$capes_json")

assets_root="$polycosmetics/src/client/resources/assets/polycosmetics"
emotes_src="$assets_root/emotes"
textures_emotes_src="$assets_root/textures/emotes"

if [ ! -d "$emotes_src" ]; then
	echo "error: PolyCosmetics emotes not found at $emotes_src" >&2
	echo "set POLYCOSMETICS_ROOT to your PolyCosmetics checkout" >&2
	exit 1
fi

if [ ! -d "$textures_emotes_src" ]; then
	echo "error: PolyCosmetics emote textures not found at $textures_emotes_src" >&2
	exit 1
fi

copy_emote_texture() {
	local name="$1"
	local dest="$2"
	local src="$textures_emotes_src/${name}.png"
	if [ ! -f "$src" ]; then
		echo "error: missing emote texture $src" >&2
		exit 1
	fi
	cp "$src" "$dest"
	echo "  texture: $src"
}

build_emote_zip() {
	local name="$1"
	local dest="$bucket/emotes/${name}.zip"
	# Pack emotes are always rebuilt so textures stay in sync with PolyCosmetics.
	if [ "$name" = "player" ] && [ -f "$dest" ]; then
		echo "emote zip already present: $name"
		return
	fi
	rm -f "$dest"
	local staging
	staging="$(mktemp -d)"
	case "$name" in
		player)
			mkdir -p "$staging/emotes" "$staging/models"
			cp "$emotes_src/player.animation.json" "$emotes_src/player.emote.json" \
				"$staging/emotes/"
			cp "$assets_root/models/player.geo.json" "$staging/models/"
			;;
		wowtext | santaguise)
			mkdir -p "$staging/emotes/$name" "$staging/textures/emotes"
			cp "$emotes_src/$name/"* "$staging/emotes/$name/"
			copy_emote_texture "$name" "$staging/textures/emotes/${name}.png"
			;;
		*)
			echo "unknown emote pack: $name" >&2
			rm -rf "$staging"
			return 1
			;;
	esac
	(
		cd "$staging"
		zip -qr "$dest" .
	)
	rm -rf "$staging"
	echo "built emote zip: $name"
}

build_emote_zip player
build_emote_zip wowtext
build_emote_zip santaguise

echo "dev assets ready under $bucket"
