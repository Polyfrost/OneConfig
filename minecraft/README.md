# OneConfig Minecraft interface
`dependsOn(":modules:*")`

This directory contains the version and Minecraft dependant code, including the full loading and internal workings of OneConfig (not public API), Minecraft tests and more. 

The project is designed to be version agnostic, and so the version specific code is kept in this directory. 
It is designed to be as small as possible, with the majority of the code being in the `modules/` directory.
Every supported Minecraft version and loader is a [Stonecutter](https://stonecutter.kikugie.dev) node built from the shared `src/main` directory, with version specific code kept behind Stonecutter comments.
`src/main` is written for the active node set in `stonecutter.gradle.kts`.
