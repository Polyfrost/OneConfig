pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        maven("https://repo.polyfrost.org/releases")
    }
}

val mod_name: String by settings

rootProject.name = mod_name


include(":modules")
project(":modules").apply {
    buildFileName = "root.gradle.kts"
}

include(":platform")
project(":platform").apply {
    projectDir = file("versions/")
    buildFileName = "root.gradle.kts"
}

listOf(
    "config",
    "config-impl",
    "commands",
    "hud",
    "events",
    "ui",
    "utils"
).forEach { module ->
    include(":modules:$module")
}

listOf(
    "1.8.9-forge",
    "1.8.9-fabric",
    "1.12.2-fabric",
    "1.12.2-forge",
    "1.16.5-forge",
    "1.16.5-fabric"
).forEach { version ->
    val proj = ":platform:$version"
    include(proj)
    project(proj).apply {
        projectDir = file("versions/$version")
        buildFileName = "../build.gradle.kts"
    }
}
