#!/usr/bin/env python3
"""Dev seeder: generates bundles by chunking the existing cosmetics.

Fetches every cosmetic from a running API (GET /cosmetics), flattens their
variant ids, splits them into fixed-size chunks, and creates one bundle per
chunk via POST /bundles/manage/create.

  * Chunk size defaults to 5; override with the first CLI arg or CHUNK_SIZE.
  * Prices start at $10.00 and rise $0.50 per bundle.
  * Bundles are created without a cover image (the field is optional).
  * All listed cosmetics are chunked, including any legacy seed capes.

Only the Python standard library is used.
"""

import io
import os
import sys
import json
import secrets
import urllib.request
import urllib.error

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
API_BASE = os.environ.get("API_BASE", "http://127.0.0.1:8080")
START_PRICE = 10.00
PRICE_STEP = 0.50


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


def chunk_size():
	raw = sys.argv[1] if len(sys.argv) > 1 else os.environ.get("CHUNK_SIZE", "5")
	try:
		size = int(raw)
	except ValueError:
		sys.exit(f"error: invalid chunk size {raw!r}")
	if size < 1:
		sys.exit("error: chunk size must be at least 1")
	return size


def encode_multipart(fields):
	"""Build a multipart/form-data body from a list of (name, value) pairs."""
	boundary = "----polybundle" + secrets.token_hex(16)
	body = io.BytesIO()

	def write(text):
		body.write(text.encode("utf-8"))

	for name, value in fields:
		write(f"--{boundary}\r\n")
		write(f'Content-Disposition: form-data; name="{name}"\r\n\r\n')
		write(f"{value}\r\n")
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


def cosmetic_ids():
	"""Return every cosmetic variant id, sorted, from GET /cosmetics."""
	status, text = request("GET", "/cosmetics")
	if status != 200:
		sys.exit(f"error: GET /cosmetics failed: {status} {text}")
	ids = set()
	for cosmetic in json.loads(text).get("cosmetics", []):
		for variant in cosmetic.get("variants", []):
			ids.add(variant["id"])
	return sorted(ids)


def main():
	size = chunk_size()
	ids = cosmetic_ids()
	if not ids:
		sys.exit("error: no cosmetics found to bundle; seed cosmetics first")

	chunks = [ids[i : i + size] for i in range(0, len(ids), size)]
	print(f"bundling {len(ids)} cosmetics into {len(chunks)} bundle(s) of up to {size}")

	created, failed = 0, 0
	for index, chunk in enumerate(chunks):
		name = f"Bundle {index + 1}"
		price = round(START_PRICE + index * PRICE_STEP, 2)
		fields = [
			("name", name),
			("description", f"{name}: {len(chunk)} cosmetics"),
			("base_price", f"{price:.2f}"),
		]
		for cosmetic_id in chunk:
			fields.append(("cosmetic_id", str(cosmetic_id)))

		content_type, body = encode_multipart(fields)
		status, text = request(
			"POST", "/bundles/manage/create", content_type=content_type, body=body
		)
		if status == 200:
			created += 1
			print(f"  ok   {name:<12} ${price:.2f}  {chunk}")
		else:
			failed += 1
			print(f"  FAIL {name:<12} {status}: {text.strip()[:160]}")

	print(f"\ndone: {created} created, {failed} failed")
	sys.exit(1 if failed else 0)


if __name__ == "__main__":
	main()
