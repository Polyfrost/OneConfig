import argparse
import hashlib
import io
import json
import re
import shutil
import sys
import time
import urllib.request
import zipfile
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

USER_AGENT = "Polyfrost/OneConfig (crowdin-sync; +https://github.com/Polyfrost/OneConfig)"
LANG_ENTRY = re.compile(r"^assets/([a-z0-9_.-]+)/lang/([A-Za-z]{2,3}_[A-Za-z]{2,3})\.(json|lang)$")
NESTED_JAR = re.compile(r"^META-INF/(jars|jarjar)/[^/]+\.jar$")
SOURCE_LANGUAGE = "en_us"
EXCLUDED_NAMESPACES = {"minecraft", "realms"}
MAX_NESTING = 3
ATTEMPTS = 3


def fetch(url):
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=60) as response:
        return response.read()


def fetch_verified(urls, sha1):
    for attempt in range(ATTEMPTS):
        if attempt:
            time.sleep(5 * attempt)
        for url in urls:
            try:
                data = fetch(url)
            except Exception as error:
                print(f"warning: failed to download {url}: {error}", file=sys.stderr)
                continue
            if sha1 and hashlib.sha1(data).hexdigest() != sha1.lower():
                print(f"warning: sha1 mismatch for {url}", file=sys.stderr)
                continue
            return data
    return None


def version_key(version):
    return tuple(int(part) if part.isdigit() else 0 for part in re.split(r"[.\-]", version))


def parse_lang(data, kind):
    text = data.decode("utf-8-sig", errors="replace")
    if kind == "json":
        parsed = json.loads(text)
        if not isinstance(parsed, dict):
            return {}
        return {key: value for key, value in parsed.items() if isinstance(key, str) and isinstance(value, str)}
    entries = {}
    for line in text.splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        entries[key] = value
    return entries


def read_lang_entry(entry, read, name, strict=False):
    match = LANG_ENTRY.match(entry)
    if not match:
        return None
    namespace, language, kind = match.groups()
    if namespace in EXCLUDED_NAMESPACES:
        return None
    try:
        return namespace, language.lower(), parse_lang(read(), kind)
    except Exception as error:
        if strict:
            raise
        print(f"warning: skipping {name}!{entry}: {error}", file=sys.stderr)
        return None


def scan_jar(data, name, depth=0):
    found = []
    try:
        archive = zipfile.ZipFile(io.BytesIO(data))
    except Exception as error:
        print(f"warning: {name} is not a valid jar: {error}", file=sys.stderr)
        return found
    with archive:
        for entry in archive.namelist():
            lang = read_lang_entry(entry, lambda: archive.read(entry), name)
            if lang:
                found.append(lang)
            elif depth < MAX_NESTING and NESTED_JAR.match(entry):
                try:
                    nested = archive.read(entry)
                except Exception as error:
                    print(f"warning: skipping {name}!{entry}: {error}", file=sys.stderr)
                    continue
                found.extend(scan_jar(nested, f"{name}!{entry}", depth + 1))
    return found


def scan_resources(root):
    found = []
    for file in sorted(root.glob("assets/*/lang/*")):
        lang = read_lang_entry(file.relative_to(root).as_posix(), file.read_bytes, str(root), strict=True)
        if lang:
            found.append(lang)
    return found


def collect_jars(base_url):
    metadata = json.loads(fetch(f"{base_url}/oneclient/bundles/metadata.json"))
    jars = {}
    for version in sorted(metadata.get("versions", {}), key=version_key):
        for loader, bundles in sorted(metadata["versions"][version].items()):
            for bundle in bundles:
                pack = fetch_verified([base_url + bundle["path"]], bundle.get("sha1"))
                if pack is None:
                    raise RuntimeError(f"failed to download bundle {bundle['path']}")
                with zipfile.ZipFile(io.BytesIO(pack)) as archive:
                    index = json.loads(archive.read("modrinth.index.json"))
                print(f"{version} {loader}: {index.get('name', bundle['path'])} ({len(index.get('files', []))} files)")
                for file in index.get("files", []):
                    path = file.get("path", "")
                    sha1 = file.get("hashes", {}).get("sha1")
                    if not path.startswith("mods/") or not path.endswith(".jar") or not sha1:
                        continue
                    jars.pop(sha1, None)
                    jars[sha1] = (path, file.get("downloads", []))
    return jars


def locale_directory(language):
    code, region = language.split("_", 1)
    return f"{code}_{region.upper()}"


def write_json(path, entries):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as output:
        json.dump(dict(sorted(entries.items())), output, ensure_ascii=False, indent=2)
        output.write("\n")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=Path("build/crowdin"))
    parser.add_argument("--meta-url", default="https://data-v2.polyfrost.org")
    parser.add_argument("--threads", type=int, default=8)
    parser.add_argument("--local-resources", type=Path, action="append", default=[])
    args = parser.parse_args()

    jars = collect_jars(args.meta_url.rstrip("/"))
    print(f"scanning {len(jars)} unique mod jars")

    def scan(item):
        sha1, (path, urls) = item
        data = fetch_verified(urls, sha1)
        if data is None:
            raise RuntimeError(f"failed to download {path}")
        return scan_jar(data, path)

    sources = {}
    translations = {}

    def merge(found):
        for namespace, language, entries in found:
            if language == SOURCE_LANGUAGE:
                sources.setdefault(namespace, {}).update(entries)
            else:
                translations.setdefault(namespace, {}).setdefault(language, {}).update(entries)

    with ThreadPoolExecutor(max_workers=args.threads) as pool:
        for found in pool.map(scan, jars.items()):
            merge(found)
    for root in args.local_resources:
        merge(scan_resources(root))

    sources = {namespace: entries for namespace, entries in sources.items() if entries}
    if not sources:
        raise RuntimeError("no source strings found")

    shutil.rmtree(args.output / "sources", ignore_errors=True)
    shutil.rmtree(args.output / "translations", ignore_errors=True)
    translated_files = 0
    for namespace, entries in sources.items():
        write_json(args.output / "sources" / f"{namespace}.json", entries)
        for language, values in translations.get(namespace, {}).items():
            values = {key: value for key, value in values.items() if key in entries and value != entries[key]}
            if values:
                write_json(args.output / "translations" / locale_directory(language) / f"{namespace}.json", values)
                translated_files += 1

    strings = sum(len(entries) for entries in sources.values())
    print(f"wrote {len(sources)} namespaces, {strings} source strings, {translated_files} existing translation files")


if __name__ == "__main__":
    main()
