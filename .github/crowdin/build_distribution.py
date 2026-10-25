import argparse
import hashlib
import io
import json
import os
import shutil
import sys
import time
import urllib.request
import zipfile
from pathlib import Path

USER_AGENT = "Polyfrost/OneConfig (crowdin-publish; +https://github.com/Polyfrost/OneConfig)"
API_URL = "https://api.crowdin.com/api/v2"
BUILD_TIMEOUT = 900
POLL_INTERVAL = 5


def request(url, token=None, body=None):
    headers = {"User-Agent": USER_AGENT}
    data = None
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if body is not None:
        headers["Content-Type"] = "application/json"
        data = json.dumps(body).encode("utf-8")
    with urllib.request.urlopen(urllib.request.Request(url, data=data, headers=headers), timeout=120) as response:
        return response.read()


def build_archive(project_id, token):
    builds = f"{API_URL}/projects/{project_id}/translations/builds"
    build = json.loads(request(builds, token, {"skipUntranslatedStrings": True}))["data"]
    deadline = time.monotonic() + BUILD_TIMEOUT
    while build["status"] in ("created", "inProgress"):
        if time.monotonic() > deadline:
            raise RuntimeError(f"build {build['id']} did not finish in {BUILD_TIMEOUT}s")
        time.sleep(POLL_INTERVAL)
        build = json.loads(request(f"{builds}/{build['id']}", token))["data"]
    if build["status"] != "finished":
        raise RuntimeError(f"build {build['id']} ended with status {build['status']}")
    url = json.loads(request(f"{builds}/{build['id']}/download", token))["data"]["url"]
    return request(url)


def collect(archive):
    files = {}
    with zipfile.ZipFile(io.BytesIO(archive)) as zipped:
        for entry in zipped.infolist():
            segments = entry.filename.strip("/").split("/")
            if entry.is_dir() or len(segments) < 2 or not segments[-1].endswith(".json"):
                continue
            parsed = json.loads(zipped.read(entry).decode("utf-8-sig"))
            if not isinstance(parsed, dict):
                continue
            values = {key: value for key, value in parsed.items() if isinstance(key, str) and isinstance(value, str)}
            if values:
                path = f"/translations/{segments[-2]}/{segments[-1]}"
                files[path] = json.dumps(dict(sorted(values.items())), ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    return files


def digest(files):
    hasher = hashlib.sha256()
    for path in sorted(files):
        hasher.update(path.encode("utf-8"))
        hasher.update(b"\0")
        hasher.update(hashlib.sha256(files[path]).digest())
    return hasher.hexdigest()


def read_previous(path):
    if path is None or not path.is_file():
        return None
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except ValueError:
        print(f"warning: ignoring unreadable manifest {path}", file=sys.stderr)
        return None


def write_output(name, value):
    output = os.environ.get("GITHUB_OUTPUT")
    if output:
        with open(output, "a", encoding="utf-8") as file:
            file.write(f"{name}={value}\n")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=Path("build/distribution"))
    parser.add_argument("--previous", type=Path)
    args = parser.parse_args()

    files = collect(build_archive(os.environ["CROWDIN_PROJECT_ID"], os.environ["CROWDIN_PERSONAL_TOKEN"]))
    if not files:
        raise RuntimeError("no translated strings found")

    content_hash = digest(files)
    previous = read_previous(args.previous)
    changed = previous is None or previous.get("hash") != content_hash
    timestamp = int(time.time()) if changed else previous["timestamp"]

    content = {}
    for path in sorted(files):
        content.setdefault(path.split("/")[-2], []).append(path)

    shutil.rmtree(args.output, ignore_errors=True)
    for path, data in files.items():
        target = args.output / path.lstrip("/")
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
    manifest = {"timestamp": timestamp, "hash": content_hash, "content": content}
    (args.output / "manifest.json").write_text(json.dumps(manifest, separators=(",", ":")), encoding="utf-8")

    write_output("changed", str(changed).lower())
    print(f"wrote {len(files)} files in {len(content)} languages, {'new release' if changed else 'unchanged'} at {timestamp}")


if __name__ == "__main__":
    main()
