## 1.4.4
- Add a config entrypoint (`ConfigEntrypoint`) so mods can provide their own OneConfig layout instead of having their original screen extracted
- Add a generic tile picker, used by item list options
- Rework header titles and navigation controls
- Rename the "Open original screen" button to "Original menu" and give it an external link icon
- Make popups and dropdowns follow the open/close animation and UI scaling
- Improve config screen extraction and its retry handling for third-party config libraries
- Fix the "All" mod filter chip sometimes being hidden
- Fix mouse buttons staying held after a screen closes or loses focus
- Fix numpad decimal, menu, ISO and JIS keys not being mapped on legacy versions
