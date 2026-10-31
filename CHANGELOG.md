## Unreleased changes
- Bring back `secure` for `@Text` (also `text(secure = true)` in KtConfig and `secure` in the config DSL) to mask sensitive values like API keys, with an eye button to reveal them
- `secureMask`, `secureShowLast` and `secureShowChars` pick the mask character and which characters stay readable, e.g. `****-****-****-abcd`
