// Downloads the classic (wide) Minecraft skin to assets/default-skin.png so the
// render-service has a base player to wear cosmetics on. Run once after install:
//   node scripts/fetch-default-skin.mjs
import { writeFile, mkdir } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const dest = path.join(here, "..", "assets", "default-skin.png");

// Classic "Steve" texture (stable Mojang texture-server hash).
const URL =
	"https://textures.minecraft.net/texture/31f477eb1a7beee631c2ca64d06f8f68fa93a3386d04452ab27f43acdf1b60cb";

const response = await fetch(URL);
if (!response.ok) {
	throw new Error(`Failed to download default skin: ${response.status}`);
}
await mkdir(path.dirname(dest), { recursive: true });
await writeFile(dest, Buffer.from(await response.arrayBuffer()));
console.log(`Wrote ${dest}`);
