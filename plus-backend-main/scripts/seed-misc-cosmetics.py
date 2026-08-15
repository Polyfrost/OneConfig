#!/usr/bin/env python3
"""Dev seeder: pushes every cosmetic in misc/n7qzpqt.zip into a running API.

Layout of the zip:  n7qzpqt/<collection>/<cosmetic>/<files...>
where <collection> is "1-13" or "14-26" (13 cosmetic folders each, 26 total).

Mapping (confirmed with the maintainer):
  * Each top folder becomes a collection ("1-13", "14-26").
  * Each cosmetic folder becomes a cosmetic; it is a group when it yields more
    than one variant.
  * Type/slots are inferred from the folder name:
        *cape*        -> cape     (cape)
        *wings        -> wings    (wings)
        *boots        -> boots    (boots)
        *particles*   -> aura     (aura)
        *shoulder*    -> shoulder (shoulder)
        *sword|gauntlet|glove -> glove (left_hand, right_hand)
        everything else       -> hat   (hat)          [halo/ears/horns/crown/fedora]
  * A variant is "one per model": each variant is uploaded as a zip bundle of
    its model(.bbmodel)+geo+animation+texture, EXCEPT pure-texture cosmetics
    (capes, no .bbmodel) whose variants are the raw .png.
  * base_price starts at $1.00 and rises $0.10 per cosmetic folder (per group);
    it is sent on every variant but only the first variant of a group actually
    creates the Stripe product/price, the rest reuse it.

Only the Python standard library is used.
"""

import io
import os
import sys
import json
import zipfile
import secrets
import urllib.request
import urllib.error

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ZIP_PATH = os.path.join(ROOT, "misc", "n7qzpqt.zip")
API_BASE = os.environ.get("API_BASE", "http://127.0.0.1:8080")
START_PRICE = 1.00
PRICE_STEP = 0.10
COLLECTION_ORDER = ["1-13", "14-26"]

SHAPE_TOKENS = {"slim", "wide"}
# Tokens that describe a piece of one model (bundled together), not a variant.
SIDE_TOKENS = {"left", "right", "reversed", "fold"}


def admin_password():
	password = os.environ.get("ADMIN_PASSWORD")
	if password:
		return password
	env_path = os.path.join(ROOT, ".env")
	if os.path.exists(env_path):
		with open(env_path) as handle:
			for line in handle:
				line = line.strip()
				if line.startswith("ADMIN_PASSWORD="):
					return line.split("=", 1)[1].strip()
	sys.exit("error: ADMIN_PASSWORD not set (env or .env)")


AUTH = admin_password()


def titleize(text):
	return " ".join(part.capitalize() for part in text.replace("-", "_").split("_") if part)


def type_and_slots(folder):
	name = folder.lower()
	if "cape" in name:
		return "cape", ["cape"]
	if name.endswith("wings"):
		return "wings", ["wings"]
	if name.endswith("boots"):
		return "boots", ["boots"]
	if "particles" in name:
		return "aura", ["aura"]
	if "shoulder" in name:
		return "shoulder", ["shoulder"]
	if name.endswith("sword") or "gauntlet" in name or "glove" in name:
		return "glove", ["left_hand", "right_hand"]
	return "hat", ["hat"]


def tokens_after(stem, base):
	if stem.startswith(base):
		rest = stem[len(base):].lstrip("_")
	else:
		rest = stem
	return [tok for tok in rest.split("_") if tok]


def texture_label(stem, base):
	if stem == base:
		return ""
	if stem.startswith(base + "_"):
		return stem[len(base) + 1:]
	if stem.endswith("_cape"):
		return stem[:-len("_cape")]
	return stem


def build_bundle(files, bbmodels, base, shape, png):
	"""Zip a variant's model(s) + geo + animation + its texture png."""
	selected = set()
	for model in bbmodels:
		model_stem = model[:-len(".bbmodel")]
		model_tokens = tokens_after(model_stem, base)
		if shape is None or shape in model_tokens:
			selected.add(model)
			for ext in (".geo.json", ".animation.json"):
				sibling = model_stem + ext
				if sibling in files:
					selected.add(sibling)
	# Folder-level shared geometry/animation (named after the folder itself).
	for ext in (".geo.json", ".animation.json"):
		sibling = base + ext
		if sibling in files:
			selected.add(sibling)
	selected.add(png)

	buffer = io.BytesIO()
	with zipfile.ZipFile(buffer, "w", zipfile.ZIP_DEFLATED) as archive:
		for name in sorted(selected):
			archive.writestr(name, files[name])
	return buffer.getvalue()


def decompose(folder, files):
	"""Return an ordered list of variant dicts for a cosmetic folder."""
	names = list(files.keys())
	pngs = sorted(name for name in names if name.lower().endswith(".png"))
	bbmodels = sorted(name for name in names if name.lower().endswith(".bbmodel"))
	variants = []

	if not bbmodels:
		# Pure-texture cosmetic (capes): each png is an image variant.
		for order, png in enumerate(pngs):
			label = texture_label(png[:-len(".png")], folder)
			variants.append({
				"variant_name": titleize(label) if label else None,
				"model_variant": None,
				"order": order,
				"filename": png,
				"content_type": "image/png",
				"data": files[png],
			})
		return variants

	# Model cosmetic: each texture png drives one variant, bundled with its model.
	for order, png in enumerate(pngs):
		tokens = tokens_after(png[:-len(".png")], folder)
		shape = next((tok for tok in tokens if tok in SHAPE_TOKENS), None)
		color = "_".join(
			tok for tok in tokens
			if tok not in SHAPE_TOKENS and tok not in SIDE_TOKENS
		)
		if color:
			variant_name = titleize(color)
		elif shape:
			variant_name = titleize(shape)
		else:
			variant_name = None
		variants.append({
			"variant_name": variant_name,
			"model_variant": shape,
			"order": order,
			"filename": f"{folder}_{color or shape or 'default'}.zip",
			"content_type": "application/zip",
			"data": build_bundle(files, bbmodels, folder, shape, png),
		})
	return variants


def read_zip():
	"""Return {collection: {cosmetic: {filename: bytes}}} from the source zip."""
	tree = {}
	with zipfile.ZipFile(ZIP_PATH) as archive:
		for info in archive.infolist():
			if info.is_dir():
				continue
			parts = info.filename.split("/")
			if parts[0] != "n7qzpqt" or "__MACOSX" in parts:
				continue
			if len(parts) != 4:
				continue
			_, collection, cosmetic, filename = parts
			if filename.startswith(".") or collection not in COLLECTION_ORDER:
				continue
			tree.setdefault(collection, {}).setdefault(cosmetic, {})[filename] = \
				archive.read(info.filename)
	return tree


def encode_multipart(fields, file_field):
	"""Build a multipart/form-data body. fields is a list of (name, value)."""
	boundary = "----polyseed" + secrets.token_hex(16)
	body = io.BytesIO()

	def write(text):
		body.write(text.encode("utf-8"))

	for name, value in fields:
		write(f"--{boundary}\r\n")
		write(f'Content-Disposition: form-data; name="{name}"\r\n\r\n')
		write(f"{value}\r\n")

	name, filename, content_type, data = file_field
	write(f"--{boundary}\r\n")
	write(
		f'Content-Disposition: form-data; name="{name}"; filename="{filename}"\r\n'
	)
	write(f"Content-Type: {content_type}\r\n\r\n")
	body.write(data)
	write("\r\n")
	write(f"--{boundary}--\r\n")

	return f"multipart/form-data; boundary={boundary}", body.getvalue()


def request(method, path, *, content_type=None, body=None):
	req = urllib.request.Request(f"{API_BASE}{path}", data=body, method=method)
	req.add_header("Authorization", AUTH)
	if content_type:
		req.add_header("Content-Type", content_type)
	try:
		with urllib.request.urlopen(req) as response:
			return response.status, response.read().decode("utf-8", "replace")
	except urllib.error.HTTPError as error:
		return error.code, error.read().decode("utf-8", "replace")
	except urllib.error.URLError as error:
		sys.exit(f"error: cannot reach {API_BASE}: {error}")


def ensure_collections():
	"""Create the two collections if missing; return {name: id}."""
	status, text = request("GET", "/collections/list")
	existing = {}
	if status == 200:
		for collection in json.loads(text).get("collections", []):
			existing[collection["name"]] = collection["id"]

	ids = {}
	for name in COLLECTION_ORDER:
		if name in existing:
			ids[name] = existing[name]
			print(f"collection exists: {name} (id {existing[name]})")
			continue
		content_type, body = encode_multipart(
			[("name", name), ("description", f"Seeded cosmetics {name}")],
			("file", "", "application/octet-stream", b""),
		)
		status, text = request(
			"POST", "/collections/create", content_type=content_type, body=body
		)
		if status not in (200, 201):
			sys.exit(f"error: failed to create collection {name}: {status} {text}")
		ids[name] = json.loads(text)["id"]
		print(f"created collection: {name} (id {ids[name]})")
	return ids


def main():
	if not os.path.exists(ZIP_PATH):
		sys.exit(f"error: {ZIP_PATH} not found")

	# Optional folder allowlist (for re-running only specific cosmetics). The
	# global price index is still advanced for skipped folders so prices stay
	# stable across partial runs.
	only = set(sys.argv[1:])

	tree = read_zip()
	collection_ids = ensure_collections()

	created, failed, folder_index = 0, 0, 0
	for collection in COLLECTION_ORDER:
		for folder in sorted(tree.get(collection, {})):
			cosmetic_type, slots = type_and_slots(folder)
			if only and folder not in only:
				folder_index += 1
				continue
			display = titleize(folder)
			price = round(START_PRICE + folder_index * PRICE_STEP, 2)
			folder_index += 1
			variants = decompose(folder, tree[collection][folder])
			grouped = len(variants) > 1
			print(
				f"\n[{folder_index:2d}] {collection}/{folder} -> {cosmetic_type} "
				f"${price:.2f} ({len(variants)} variant(s))"
			)

			for variant in variants:
				fields = [
					("type", cosmetic_type),
					("name", display),
					("description", f"{display} ({collection})"),
					("collection", str(collection_ids[collection])),
					("base_price", f"{price:.2f}"),
					("variant_order", str(variant["order"])),
				]
				for slot in slots:
					fields.append(("slots", slot))
				if grouped:
					fields.append(("group", display))
				if variant["variant_name"]:
					fields.append(("variant_name", variant["variant_name"]))
				if variant["model_variant"]:
					fields.append(("model_variant", variant["model_variant"]))

				content_type, body = encode_multipart(
					fields,
					(
						"file",
						variant["filename"],
						variant["content_type"],
						variant["data"],
					),
				)
				status, text = request(
					"POST",
					"/cosmetics/manage/create",
					content_type=content_type,
					body=body,
				)
				label = variant["variant_name"] or "(single)"
				if status == 200:
					created += 1
					print(f"     ok   {label:<16} {variant['filename']}")
				else:
					failed += 1
					print(f"     FAIL {label:<16} {status}: {text.strip()[:160]}")

	print(f"\ndone: {created} uploaded, {failed} failed")
	sys.exit(1 if failed else 0)


if __name__ == "__main__":
	main()
