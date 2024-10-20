# OneConfig dependencies

This directory contains the dependencies for the OneConfig project.

## Complications
The dependencies we use are also used by various other projects. Hence,
we want to ensure that we always load the newest version of whatever
dependency we use. However, each mod loader we use has a different way
of handling dependencies, with varying levels of complexity:

- **On Legacy Forge:** There is no built-in dependency system. If we have a
  newer version of a dependency, we depend on a hack in the OneConfig
  loader (which is required for Legacy Forge) known as "Relaunch."
  Essential also uses the same system, but we have systems in place to
  ensure both work together and always load the correct version of our
  dependencies.
- **On modern Forge without JiJ:** In later versions, Forge has
  added a similar system to Fabric's JiJ. However, this system is not
  in all of the versions we support, so we simply have to cope with
  this stupidity and hope that the versions we use will work itself
  out...
- **On Fabric:** Fabric has a built-in dependency system (via JiJ, aka
  "Jar-in-Jar"). Assuming all dependencies follow SemVer correctly
  (which all of ours do), we can simply use the `include` configuration
  to include the dependencies in the final JAR file, and Fabric will
  automatically handle any version conflicts.
- **On NeoForge / modern Forge with JiJ:** We can use the same system as
  Fabric.

In addition, the `UniversalCraft` library is per-MC version, adding a new
level of complexity, LWJGL is split between LWJGL2 (1.12 and below) and
LWJGL3 (1.13 and above), and we require the `PolyMixin` library for
1.12.2 and below.

## So... what do we do?

- **On Legacy Forge:** We put all of our dependencies packed into separate
  JAR into one singular JAR file. Once the OneConfig loader downloads the
  dependency JAR, it will extract the JAR and load / handle each individual
  JAR / dependency from there.
- **On modern Forge without JiJ:** We shade all of our dependencies into
  the platform JAR and pray that it works.
- **On Fabric:** We use the `include` configuration to include the
  dependencies in the final JAR file.
- **On NeoForge / modern Forge with JiJ:** We use the same system as
  Fabric.