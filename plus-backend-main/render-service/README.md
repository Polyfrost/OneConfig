# plus-render-service

Headless [skinview3d](https://github.com/Polyfrost/skinview3d) sidecar that
renders cosmetic **cover images** for `plus-backend`. When an admin uploads a
cosmetic, the backend POSTs it here and stores the returned PNG as the
cosmetic's cover.

skinview3d is browser-only (three.js/WebGL), so rendering runs in a long-lived
headless Chrome instance driven by Puppeteer. The fork is **pre-bundled** into
`vendor/skinview3d.bundle.js` (committed) and injected into the page as a global
`skinview3d`. Runtime deps are therefore pure-JS (`puppeteer` + `fflate`) — no
build step, no git dependency — which keeps the Nix package simple.

## Run with Nix (production)

```sh
nix run .#render-service       # or: nix build .#render-service
```

The Nix package installs deps from `package-lock.json` via `importNpmLock`,
skips Puppeteer's Chromium download, and wires the nixpkgs `chromium` in at
runtime (`PUPPETEER_EXECUTABLE_PATH`). If no skin is present it downloads the
classic skin on first start.

## Run with npm (local dev)

```sh
npm install
npm start                      # skin is auto-fetched if missing
```

## Rebuilding the vendored bundle

After bumping the skinview3d fork, regenerate the bundle (uses the isolated
`tools/` package so the git dep never enters the runtime lockfile):

```sh
cd tools && npm install && node build-bundle.mjs
```

## HTTP API

- `GET /` → `ok` (health check).
- `POST /render` — JSON body:
  ```json
  {
    "type": "cape|wings|aura|hat|glove|boots|shoulder|backpack|glasses|emote",
    "slots": ["cape"],
    "model_variant": "slim|wide|null",
    "is_bundle": true,
    "asset_b64": "<base64 of the uploaded file>"
  }
  ```
  `asset_b64` is the uploaded file itself: a raw `.png` for capes, or a zip
  bundle (containing `*.geo.json` + `*.png` [+ `*.animation.json`]) for model
  cosmetics and emotes. Responds `200 image/png` with the rendered cover, or a
  `5xx` on failure (the backend treats failures as "no cover").

## Config (env)

| Var | Default | Meaning |
| --- | --- | --- |
| `PORT` | `8090` | Listen port |
| `RENDER_WIDTH` / `RENDER_HEIGHT` | `400` / `600` | Canvas size |
| `PLAYER_YAW` | `0.4` | Player rotation (radians) for a 3/4 view |
| `DEFAULT_SKIN_PATH` | `assets/default-skin.png` | Base player skin |
| `PUPPETEER_EXECUTABLE_PATH` | _(bundled Chromium)_ | Chromium binary (set in Nix) |

## Notes

- `backpack` and `glasses` have no dedicated skinview3d slot; they're attached at
  the nearest bone (`shoulder` / `hat`). The geometry file carries the real
  offsets.
- Emote covers currently render the default player only (best-effort).
