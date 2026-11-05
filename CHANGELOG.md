## Unreleased changes
- Dropped compatibility with Hypixel SkyBlock mods (Apec, Firmament, Odin, Skyblocker, SkyHanni and other MoulConfig-based mods) on Minecraft versions below 26.1, which can no longer be used to play SkyBlock
- Bring back `secure` for `@Text` (also `text(secure = true)` in KtConfig and `secure` in the config DSL) to mask sensitive values like API keys, with an eye button to reveal them
- `secureMask`, `secureShowLast` and `secureShowChars` pick the mask character and which characters stay readable, e.g. `****-****-****-abcd`
