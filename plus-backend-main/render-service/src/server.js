// Headless skinview3d render sidecar.
//
// Exposes `POST /render` which takes a cosmetic (a cape png or a model bundle
// zip that already contains its Bedrock geometry + texture) and returns a PNG
// showing that cosmetic worn on a configurable default player skin.
//
// The skinview3d fork is browser-only (three.js/WebGL), so rendering happens in
// a long-lived headless Chrome instance driven by Puppeteer. The fork is
// pre-bundled into vendor/skinview3d.bundle.js (an IIFE exposing a global
// `skinview3d`, with three inlined) and injected into the page. Rebuild that
// bundle with `node scripts/build-bundle.mjs` after bumping the fork.

import http from "node:http";
import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

import { unzipSync } from "fflate";
import puppeteer from "puppeteer";

const here = path.dirname(fileURLToPath(import.meta.url));

const PORT = Number(process.env.PORT ?? 8090);
const RENDER_WIDTH = Number(process.env.RENDER_WIDTH ?? 400);
const RENDER_HEIGHT = Number(process.env.RENDER_HEIGHT ?? 600);
// Small yaw so covers read as 3D rather than a flat front-on shot.
const PLAYER_YAW = Number(process.env.PLAYER_YAW ?? 0.4);
// Back-worn cosmetics (capes, wings, backpacks) are invisible from the front,
// so show them from a back-3/4 angle instead (roughly 180deg plus the same 3D
// offset).
const BACK_YAW = Number(process.env.BACK_YAW ?? process.env.CAPE_YAW ?? Math.PI + 0.4);
const BACK_WORN_TYPES = new Set(["cape", "wings", "backpack"]);

// Per-type framing. The camera always looks at the origin, so the harness
// translates the player so the `anchor` region's center lands there, then picks
// a zoom that fits the whole `fit` region (union) inside the frame with a
// `margin` of slack — so tall hats and wide wings never get clipped.
// Region names resolve to: "head" | "body" | "legs" | "cosmetic" (the loaded
// cosmetic's rendered pieces).
const FOCUS_BY_TYPE = {
	hat: { anchor: "head", fit: ["head", "cosmetic"], margin: 1.3 },
	glasses: { anchor: "head", fit: ["head", "cosmetic"], margin: 1.3 },
	boots: { anchor: "cosmetic", fit: ["cosmetic"], margin: 1.6 },
	shoulder: { anchor: "cosmetic", fit: ["cosmetic", "head"], margin: 1.35 },
	wings: {
		anchor: "body",
		fit: ["cosmetic", "body"],
		margin: 1.6,
		recenter: false,
		centerXShift: 0.25,
	},
	backpack: { anchor: "cosmetic", fit: ["cosmetic"], margin: 1.25 },
};
const ANIMATE_SECONDS_BY_TYPE = {
	aura: 0.9,
	wings: 1.0,
};
const BUNDLE_PATH = path.join(here, "..", "vendor", "skinview3d.bundle.js");
const DEFAULT_SKIN_PATH =
	process.env.DEFAULT_SKIN_PATH ??
	path.join(here, "..", "assets", "default-skin.png");
// Classic "Steve" texture, fetched at startup when no skin file is present (so
// the Nix package needs no baked-in asset).
const FALLBACK_SKIN_URL =
	"https://textures.minecraft.net/texture/31f477eb1a7beee631c2ca64d06f8f68fa93a3386d04452ab27f43acdf1b60cb";

// skinview3d's CosmeticSlot union. Types the fork has no dedicated slot for are
// approximated to the nearest attachment point (best-effort — the geometry file
// carries the real offsets, the slot only picks the bone).
const SLOT_BY_TYPE = {
	wings: "wings",
	aura: "aura",
	hat: "hat",
	glove: "glove",
	boots: "boots",
	shoulder: "shoulder",
	backpack: "shoulder",
	glasses: "hat",
};

function pngDataUrl(bytes) {
	return `data:image/png;base64,${Buffer.from(bytes).toString("base64")}`;
}

/**
 * Pull the Bedrock geometry, texture and (optional) animation out of a model
 * bundle zip. Entry paths differ between cosmetic bundles (files at root) and
 * emote packs (under models/, textures/), so we match by extension, not path.
 */
function extractBundle(data) {
	const files = unzipSync(data);
	let geometry = null;
	let animation = null;
	let textureUrl = null;

	for (const [name, bytes] of Object.entries(files)) {
		const lower = name.toLowerCase();
		if (lower.endsWith(".geo.json")) {
			geometry = JSON.parse(Buffer.from(bytes).toString("utf8"));
		} else if (lower.endsWith(".animation.json")) {
			animation = JSON.parse(Buffer.from(bytes).toString("utf8"));
		} else if (lower.endsWith(".png")) {
			// First png wins; bundles carry a single cosmetic texture.
			textureUrl ??= pngDataUrl(bytes);
		}
	}

	return { geometry, animation, textureUrl };
}

/**
 * Runs inside the headless Chrome page. `skinview3d` is a global injected at
 * startup. Builds a fresh viewer, applies the cosmetic, renders one frame and
 * returns a PNG data url. The viewer is disposed so WebGL contexts don't leak.
 */
async function renderInPage(params) {
	const viewer = new skinview3d.SkinViewer({
		width: params.width,
		height: params.height,
		renderPaused: true,
	});

	// Axis-aligned world bounding box over a set of Object3D subtrees, computed
	// with only Object3D/Vector3 APIs (the bundle doesn't export THREE.Box3).
	// `vec` is a scratch Vector3 (cloned from an existing one) reused per corner.
	function worldBounds(roots, vec) {
		let minX = Infinity, minY = Infinity, minZ = Infinity;
		let maxX = -Infinity, maxY = -Infinity, maxZ = -Infinity;
		let seen = false;
		for (const root of roots) {
			root.traverse((o) => {
				const g = o.geometry;
				if (!g) return;
				if (!g.boundingBox) g.computeBoundingBox();
				const bb = g.boundingBox;
				if (!bb) return;
				for (const x of [bb.min.x, bb.max.x])
					for (const y of [bb.min.y, bb.max.y])
						for (const z of [bb.min.z, bb.max.z]) {
							vec.set(x, y, z).applyMatrix4(o.matrixWorld);
							if (vec.x < minX) minX = vec.x;
							if (vec.y < minY) minY = vec.y;
							if (vec.z < minZ) minZ = vec.z;
							if (vec.x > maxX) maxX = vec.x;
							if (vec.y > maxY) maxY = vec.y;
							if (vec.z > maxZ) maxZ = vec.z;
							seen = true;
						}
			});
		}
		return seen ? { minX, minY, minZ, maxX, maxY, maxZ } : null;
	}

	try {
		await viewer.loadSkin(params.skinDataUrl, { model: params.model });

		let cosmeticObject = null;
		if (params.kind === "cape") {
			await viewer.loadCape(params.textureDataUrl);
		} else if (params.kind === "cosmetic") {
			cosmeticObject = await viewer.loadCosmetic({
				type: params.slot,
				geometry: params.geometry,
				texture: params.textureDataUrl,
				animation: params.animation ?? undefined,
			});
		}
		// kind === "emote": just render the default player (best-effort cover).

		const player = viewer.playerObject;
		if (player) {
			player.rotation.y = params.yaw;

			// Particle auras emit over time; a frame-0 still shows nothing. Step
			// the cosmetic animation forward (the render loop normally ticks it
			// with per-frame deltas) so the still catches particles mid-emission.
			if (params.animateSeconds > 0) {
				const step = 1 / 60;
				for (let t = 0; t < params.animateSeconds; t += step) {
					player.updateCosmetics(step);
				}
			}

			const focus = params.focus;
			if (focus) {
				// The cosmetic wrapper isn't an Object3D; its rendered pieces live
				// in `mounts[].group`, attached into the player's tree.
				const cosmeticRoots = cosmeticObject
					? (cosmeticObject.mounts ?? []).map((m) => m.group).filter(Boolean)
					: [];
				const resolve = (names) => {
					const out = [];
					for (const n of names) {
						if (n === "head") out.push(player.skin.head);
						else if (n === "body") out.push(player.skin.body);
						else if (n === "legs")
							out.push(player.skin.leftLeg, player.skin.rightLeg);
						else if (n === "cosmetic") out.push(...cosmeticRoots);
					}
					return out.filter(Boolean);
				};

				const anchorRoots = resolve([focus.anchor]);
				const fitRoots = resolve(focus.fit);
				if (anchorRoots.length && fitRoots.length) {
					// Measure after the yaw is applied so boxes are in the same
					// world frame the camera sees.
					player.updateMatrixWorld(true);
					const scratch = player.position.clone();
					const anchorBox = worldBounds(anchorRoots, scratch);
					const fitBox = worldBounds(fitRoots, scratch);
					if (anchorBox && fitBox) {
						// Pick a zoom that fits the whole fit region. The viewer's
						// visible world height is ~4.2 + 33/zoom; visible width is
						// that times the canvas aspect (width/height).
						const aspect = params.width / params.height;
						const h = fitBox.maxY - fitBox.minY;
						const w = fitBox.maxX - fitBox.minX;
						const zoomH = 33 / Math.max(h * focus.margin - 4.2, 0.1);
						const zoomW =
							33 / Math.max((w * focus.margin) / aspect - 4.2, 0.1);
						const zoom = Math.max(0.5, Math.min(2.6, Math.min(zoomH, zoomW)));

						// Center the anchor region at the origin (the camera target).
						if (focus.recenter !== false) {
							player.position.x -= (anchorBox.minX + anchorBox.maxX) / 2;
							player.position.y -= (anchorBox.minY + anchorBox.maxY) / 2;
						} else if (focus.centerXShift) {
							const fitCX = (fitBox.minX + fitBox.maxX) / 2;
							player.position.x -= fitCX * focus.centerXShift;
						}
						viewer.zoom = zoom;
					}
				}
			}
		}
		viewer.render();
		// Read synchronously in the same tick so the drawing buffer is intact.
		return viewer.canvas.toDataURL("image/png");
	} finally {
		if (typeof viewer.dispose === "function") {
			viewer.dispose();
		}
	}
}

async function loadDefaultSkin() {
	try {
		return pngDataUrl(await readFile(DEFAULT_SKIN_PATH));
	} catch {
		console.log(
			`No skin at ${DEFAULT_SKIN_PATH}; fetching the classic skin...`,
		);
		const response = await fetch(FALLBACK_SKIN_URL);
		if (!response.ok) {
			throw new Error(
				`Unable to read ${DEFAULT_SKIN_PATH} and fallback download failed ` +
					`(${response.status}). Set DEFAULT_SKIN_PATH to a 64x64 skin png.`,
			);
		}
		return pngDataUrl(Buffer.from(await response.arrayBuffer()));
	}
}

async function main() {
	const skinDataUrl = await loadDefaultSkin();
	const bundleSource = await readFile(BUNDLE_PATH, "utf8");

	console.log("Launching headless Chrome...");
	const browser = await puppeteer.launch({
		headless: true,
		executablePath: process.env.PUPPETEER_EXECUTABLE_PATH || undefined,
		args: [
			"--no-sandbox",
			"--disable-dev-shm-usage",
			"--use-gl=angle",
			"--use-angle=swiftshader",
			"--enable-unsafe-swiftshader",
			"--ignore-gpu-blocklist",
		],
	});

	const page = await browser.newPage();
	page.on("console", (msg) => {
		if (msg.type() === "error") console.error(`[page] ${msg.text()}`);
	});
	await page.setContent("<!doctype html><html><body></body></html>");
	await page.addScriptTag({ content: bundleSource });

	async function render(body) {
		const isBundle = Boolean(body.is_bundle);
		const type = body.type;
		const model = body.model_variant === "slim" ? "slim" : "default";

		let kind;
		let geometry = null;
		let animation = null;
		let textureDataUrl = null;

		if (type === "cape") {
			kind = "cape";
			textureDataUrl = isBundle
				? extractBundle(Buffer.from(body.asset_b64, "base64")).textureUrl
				: pngDataUrl(Buffer.from(body.asset_b64, "base64"));
		} else if (type === "emote") {
			kind = "emote";
		} else {
			kind = "cosmetic";
			const bundle = extractBundle(Buffer.from(body.asset_b64, "base64"));
			geometry = bundle.geometry;
			animation = bundle.animation;
			textureDataUrl = bundle.textureUrl;
			if (!geometry || !textureDataUrl) {
				throw new Error(
					`bundle for '${type}' is missing geometry or texture`,
				);
			}
		}

		const dataUrl = await page.evaluate(renderInPage, {
			width: RENDER_WIDTH,
			height: RENDER_HEIGHT,
			yaw: BACK_WORN_TYPES.has(type) ? BACK_YAW : PLAYER_YAW,
			focus: FOCUS_BY_TYPE[type] ?? null,
			animateSeconds: ANIMATE_SECONDS_BY_TYPE[type] ?? 0,
			skinDataUrl,
			model,
			kind,
			slot: SLOT_BY_TYPE[type],
			geometry,
			animation,
			textureDataUrl,
		});
		return Buffer.from(dataUrl.split(",")[1], "base64");
	}

	// Serialize renders: one page, one WebGL pipeline.
	let queue = Promise.resolve();

	const server = http.createServer((req, res) => {
		if (req.method === "GET" && req.url === "/") {
			res.writeHead(200).end("ok");
			return;
		}
		if (req.method !== "POST" || req.url !== "/render") {
			res.writeHead(404).end("not found");
			return;
		}

		const chunks = [];
		req.on("data", (chunk) => chunks.push(chunk));
		req.on("end", () => {
			let body;
			try {
				body = JSON.parse(Buffer.concat(chunks).toString("utf8"));
			} catch {
				res.writeHead(400).end("invalid json");
				return;
			}
			queue = queue
				.then(() => render(body))
				.then((png) => {
					res.writeHead(200, { "content-type": "image/png" }).end(png);
				})
				.catch((error) => {
					console.error(`render failed: ${error.stack ?? error}`);
					res.writeHead(500).end(String(error.message ?? error));
				});
		});
	});

	server.listen(PORT, () => {
		console.log(`render-service listening on :${PORT}`);
	});

	const shutdown = async () => {
		await browser.close();
		process.exit(0);
	};
	process.on("SIGINT", shutdown);
	process.on("SIGTERM", shutdown);
}

main().catch((error) => {
	console.error(error);
	process.exit(1);
});
