![OneConfig_banner_wide](https://user-images.githubusercontent.com/62163840/226811571-ffe7a524-c96a-4b6e-9ca9-d29439f5174f.png)

# OneConfig
[![Build status](https://img.shields.io/github/actions/workflow/status/Polyfrost/OneConfig/build.yml?style=flat&label=build&branch=v1)](https://github.com/Polyfrost/OneConfig/actions/workflows/build.yml)
[![Modrinth downloads](https://img.shields.io/modrinth/dt/AibBIVmj?style=flat&logo=modrinth&label=Modrinth&color=1bd96a)](https://modrinth.com/mod/oneconfig)
[![CurseForge downloads](https://img.shields.io/curseforge/dt/1148006?style=flat&logo=curseforge&label=CurseForge&color=f16436)](https://www.curseforge.com/minecraft/mc-mods/oneconfig)

OneConfig is a free, open source configuration library by [Polyfrost](https://polyfrost.org). It gives developers a config API, and it gives players one screen where they change the settings of every mod that uses it.

## ❔ General information

### Is it a client?
OneConfig may look like a client, but it is a standalone mod. You can [see for yourself](https://github.com/Polyfrost/OneConfig-Bootstrap). It ships no mods of its own, so you decide which mods you use with it.

### What do players get?
- A settings screen with one card per mod, search, and a keybinds page. Right Shift opens it.
- A HUD editor for moving, scaling and restyling the HUD elements of your mods.
- Profiles, which each keep a separate copy of your mod settings.

### Which versions does it support?
- Minecraft 1.8.9 on Ornithe.
- Minecraft 1.21.1-26.3 on Fabric.

OneConfig needs [Compose Multiplatform](https://modrinth.com/mod/compose-multiplatform) and Fabric Language Kotlin. It also needs Fabric API on Fabric and OSL on Ornithe.

## ⚡️ Getting started
Almost everything you need to start using OneConfig is in [our documentation](https://docsv1.polyfrost.org). For further questions, make a developer ticket in [our Discord](https://polyfrost.org/discord).

To build OneConfig yourself, run the command below with JDK 25 installed. Replace `26.3-fabric` with any supported version, such as `1.8.9-ornithe`.

```sh
./gradlew ":bootstrap:26.3-fabric:build"
```

## ℹ️ Example mod
Please check out our [example mod](https://github.com/Polyfrost/OneConfigExampleMod/).

## 📄 Licenses and Supporting Projects
<details>
  <summary>License</summary>
  OneConfig is free and open-source. Except where noted otherwise (below and/or in individual files), all code is licensed under BOTH 
- version 3 of the GNU Lesser General Public License, **AND**
- the Additional Terms Applicable to OneConfig, version 1.0 of the Additional Terms or (at your option) any later version.

Copies of version 3 of the GNU Lesser General Public License, version 1.0 of the Additional Terms Applicable to OneConfig, and version 3 of the GNU General Public License (which the GNU LGPLv3 supplements) can be found in the [**LICENSE**](LICENSE) file.

Polyfrost acts as the specified proxy when deciding whether future versions of the GNU Lesser General Public License shall apply. If Polyfrost publicly states the acceptance of a later version of the GNU Lesser General Public License, this is permanent authorization for you to choose that version for OneConfig. However, you **must still** be compliant with the Additional Terms Applicable to OneConfig.
</details>

<details>
  <summary>Supporting Projects</summary>

![YourKitLogo|50](https://www.yourkit.com/images/yklogo.png)

YourKit supports open source projects with innovative and intelligent tools
for monitoring and profiling Java and .NET applications. <br>
YourKit is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/),
[YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/),
and [YourKit YouMonitor](https://www.yourkit.com/youmonitor/).
</details>
