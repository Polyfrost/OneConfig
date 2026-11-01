## Unreleased changes
- Fixed legacy HUDs being drawn behind the OneConfig UI outside a world
- Fixed the HUD editor tour picking legacy HUDs
- Fixed rounded corners rendering incorrectly on elements too small for their corner radius
- Fixed the HUD editor tour's highlight breaking for HUDs at the edge of the screen
- Number spinners in the HUD editor now keep their unit next to the value
- Fixed the OneConfig UI closing with a rendering error when a HUD fails to update outside a world, such as Legacy Skyblock's Pet Display on 26.x
- HUDs that keep failing to update now only log the error once instead of every frame

### For mod developers
- Fixed builds depending on OneConfig for 1.8.9 failing to resolve LWJGL STB natives
