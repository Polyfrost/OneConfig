1.0.0-beta.6 changelogs:

- Add VulkanMod support
- Add BlazeSDL support
- New translation API
- Make profiles support keybinds and all mods, even compat mods
- Add all vanilla Minecraft keybinds to OneConfig keybinds
- Add missing options to KtConfig, including transformed and observable entries
- Stop including Compose Multiplatform in the JAR
- Add ModMenu entries

- Fix keybinds not unbinding
- Fix keybind infinite recursion StackOverflowError between OneConfig and kryptconfig (#688)
- Fix LegacyHud editor issues (#686)
- Fix Minecraft theme having rounded corners (#712)
- Fix Fabric API dependency issues (#721)
- Fix non-daemon threads delaying Minecraft shutdown on 26.2 (#722)
- Fix FrameBufferEvent and ChatSendEvent mixins (#719)
- Dispatch ServerJoinEvent once the play connection is established (#694)
- Attempt to fix Sodium Extra resolution scaling
- Fix various profiles issues
- Reset config on corrupt data and improve tree overwrite resilience
- Fix theme consistency in many places
- Slow down chroma speed
- Properly dispose Compose in certain situations