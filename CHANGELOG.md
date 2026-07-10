1.0.0-beta.7 changelogs:

- Various performance improvements
- Drag HUD elements directly in the OneConfig UI
- Open the HUD editor from the OneConfig UI with Right Shift
- Always draw boxes around HUD elements in the HUD editor (#758)
- Add a "hidden" toggle for HUD elements

- Fix ModMenu compat on 26.2
- Fix various font renderer bugs (#753)
  - Should work much more reliably and for all MC-supported characters
- Fix the GUI Scale setting being unreadable in the HUD designer (#756)
- Fix the "OneConfig (Platform Module)" mod card occasionally appearing (#754)
- Fix single-instance HUDs showing up in the HUD drawer (#755, #739)
- Fix HUDs always resetting to 0,0 (#746)
- Fix OneConfig keybinds not being changeable through the vanilla Controls menu on 1.21.1–1.21.8 (#737)
- Fix original compat mod screens not saving config values
- Fix various Skyblocker issues
- Load profiles asynchronously and stop copying the whole config folder