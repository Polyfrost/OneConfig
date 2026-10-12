# OneConfig

[![Modrinth downloads](https://img.shields.io/modrinth/dt/AibBIVmj?style=flat&logo=modrinth&label=Modrinth&color=1bd96a)](https://modrinth.com/mod/oneconfig)
[![CurseForge downloads](https://img.shields.io/curseforge/dt/1148006?style=flat&logo=curseforge&label=CurseForge&color=f16436)](https://www.curseforge.com/minecraft/mc-mods/oneconfig)

OneConfig puts the settings of your mods on one screen and gives you an editor for moving HUD elements. Most Polyfrost mods need it to run. It adds no gameplay features of its own.

## Features

### Settings screen

Press Right Shift while you are in a world to open OneConfig. The `/oneconfig` and `/ocfg` commands open it too. You can rebind the key in Preferences.

- The Mods page shows one card per mod. Click a card to open that mod's settings.
- Filter the cards by category: Hypixel, Performance, Visuals, HUD, Utility, Quality of Life or Other.
- Star a card to keep it at the front, hide a card you do not use, and drag cards to reorder them.
- Switch a mod on or off from its card when the mod supports it.
- Search finds mods and single options by name. It tolerates typos, and you set how many in Preferences.

### Settings for other mods

On Fabric, a mod that registers a config screen with Mod Menu also gets a card, and you do not need Mod Menu installed for that. For some config libraries, such as YetAnotherConfigLib, OneConfig shows the options on its own page. For the rest, the card opens the mod's own screen.

### HUD editor

Click Edit HUD in the sidebar or run `/oneconfig hud`. OneConfig ships no HUD elements itself. The editor lists the elements that your installed mods provide.

- Drag an element to move it. It snaps to the screen edges, the screen center, and the edges and centers of other elements.
- Drag a box around several elements to select them together.
- Undo with Ctrl+Z and redo with Ctrl+Shift+Z or Ctrl+Y. Copy, cut, paste, duplicate and delete work with Ctrl+C, Ctrl+X, Ctrl+V, Ctrl+D and Delete. macOS uses Cmd in place of Ctrl. You can rebind every one of these keys.
- Lock an element's position, or reset it to its default position.
- Scale an element with the Minecraft GUI scale, or set a custom scale from 0.25x to 4x.
- Change the background color and corner radius, the text color and the text shadow.
- Choose whether an element shows in the F3 screen, in the tab list, in GUIs and in chat.

### Profiles

A profile is a separate copy of your mod settings. You can create, clone, rename and delete profiles, mark favorites, and export a profile as a zip file. Each profile keeps its own Minecraft controls unless you turn off "Separate controls".

### Keybinds

The Keybinds page lists Minecraft's own keybinds together with the keybinds of your mods, grouped by mod. It marks keybinds that conflict and names the keybind each one conflicts with.

### Themes and preferences

- Pick one of four themes: PolyGlass Dark, PolyGlass Light, Minecraft Dark or Minecraft Light. PolyGlass Dark is the default.
- Set the accent color.
- Set the opacity of the page and the sidebar, and turn the background blur off.
- Turn the opening, closing and page animations off, or change how long they take.
- Turn the interface sounds and the ambient loop off, or change their volume.
- An option pauses singleplayer worlds while the screen is open. It is off by default.

## Compatibility

OneConfig does not load alongside PolyPlus versions older than 1.2.49.

## Supported versions

- Minecraft 1.8.9 on Ornithe.
- Minecraft 1.21.1, 1.21.4, 1.21.5, 1.21.8, 1.21.10, 1.21.11, 26.1, 26.2 and 26.3 on Fabric.

OneConfig needs [Compose Multiplatform](https://modrinth.com/mod/compose-multiplatform) and Fabric Language Kotlin. It also needs Fabric API on Fabric and OSL on Ornithe.

## For developers

OneConfig is a library for client mods. It gives a mod a config with a generated settings page, HUD elements that players place in the HUD editor, keybinds, client commands, events and notifications. The [documentation](https://docsv1.polyfrost.org) explains how to add it to a mod.

## Support

- Report bugs and ask questions on the [Polyfrost Discord](https://polyfrost.org/discord).
- Check [Polyfrost Status](https://status.polyfrost.org/) if a Polyfrost service is down.

## Project layout

- `modules/` holds the library modules, such as `config`, `hud`, `commands`, `events`, `notifications` and `ui`. They do not depend on a Minecraft version.
- `minecraft/` holds the code for each Minecraft version and loader.
- `bootstrap/` builds the mod jars that players install. Each jar bundles the modules and the matching `minecraft` build.
- `modules/compose-bundle` builds Compose Multiplatform, which ships as its own mod and is not bundled in the OneConfig jar.
- `buildSrc/` holds the Gradle convention plugins.

## License

OneConfig is licensed under version 3 of the GNU Lesser General Public License and the Additional Terms Applicable to OneConfig. The [LICENSE](LICENSE) file has the full text.

## Building

```sh
./gradlew ":bootstrap:26.3-fabric:build"
```

Replace `26.3-fabric` with any supported version, for example `1.21.11-fabric` or `1.8.9-ornithe`. The jar lands in `bootstrap/versions/<version>/build/libs`. The build needs JDK 25 installed.
