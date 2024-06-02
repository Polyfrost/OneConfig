pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
		mavenCentral()
        maven("https://repo.polyfrost.org/releases") {
            name = "Polyfrost Releases"
        }
    }
}

dependencyResolutionManagement {
    pluginManagement.repositories.forEach { repositories.add(it) }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.8.0")
}

val name: String by settings

rootProject.name = name


include(":modules")
project(":modules").apply {
    buildFileName = "root.gradle.kts"
}

include(":platform")
project(":platform").apply {
    projectDir = file("versions/")
    buildFileName = "preprocessor.gradle.kts"
}

listOf(
    "config",
    "config-impl",
    "commands",
    "hud",
    "events",
    "ui",
    "internal",
    "utils"
).forEach { module ->
    include(":modules:$module")
}


// FOR ALL NEW VERSIONS MAKE SURE TO INCLUDE THEM IN preprocessor.gradle.kts !
listOf(
    "1.8.9-forge",
    "1.8.9-fabric",
    "1.12.2-fabric",
    "1.12.2-forge",
    "1.16.5-forge",
    "1.16.5-fabric",
    "1.17.1-forge",
    "1.17.1-fabric",
    "1.18.1-forge",
    "1.18.1-fabric",
    "1.19.4-forge",
    "1.19.4-fabric",
    "1.20.4-fabric",
    "1.20.4-forge"
).forEach { version ->
    val proj = ":platform:$version"
    include(proj)
    project(proj).apply {
        projectDir = file("versions/$version")
        buildFileName = "../build.gradle.kts"
    }
}
