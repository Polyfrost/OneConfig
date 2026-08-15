// Bundles the skinview3d fork (+ three) into ../vendor/skinview3d.bundle.js as an
// IIFE exposing a global `skinview3d`. Run after bumping the fork:
//   cd tools && npm install && node build-bundle.mjs
import esbuild from "esbuild";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const outfile = path.join(here, "..", "vendor", "skinview3d.bundle.js");

await esbuild.build({
	stdin: {
		contents: `export * from "skinview3d";`,
		resolveDir: here,
		loader: "js",
	},
	bundle: true,
	format: "iife",
	globalName: "skinview3d",
	platform: "browser",
	minify: true,
	outfile,
	logLevel: "info",
});
console.log(`Wrote ${outfile}`);
