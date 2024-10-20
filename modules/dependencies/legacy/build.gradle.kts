plugins {
    alias(libs.plugins.jetbrains.idea.ext)
}

description = "Dependencies for legacy platforms (<1.12)"

val natives = listOf("windows", "windows-arm64", "linux", "macos", "macos-arm64")

dependencies {
    for (dep in listOf("-nanovg", "-tinyfd", "-stb", "")) {
        val lwjglDep = "org.lwjgl:lwjgl$dep:${libs.versions.lwjgl.get()}"
        api(lwjglDep) {
            isTransitive = false
        }
    }
}