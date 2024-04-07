# OneConfig Minecraft interface
`dependsOn(":modules:*")`

This directory contains the version and Minecraft dependant code, including the full loading and internal workings of OneConfig (not public API), Minecraft tests and more. 

The project is designed to be version agnostic, and so the version specific code is kept in this directory. 
It is designed to be as small as possible, with the majority of the code being in the `modules/` directory.
This project was originally made for `1.8.9-forge`, and so this is the `mainProject` and the API used in the `src/main` directory.

Each of the other directories is a separate project, which is automatically generated using the `Polyfrost Gradle Toolkit` and the `ReplayMod preprocessor`.
Some very version specific code is overwritten in these directories as well, with the exact same path as the `src/main` directory.