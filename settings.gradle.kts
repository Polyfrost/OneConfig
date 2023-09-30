pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.polyfrost.org/releases")
    }
    val pgtVersion = "0.2.6"
    plugins {
        id("org.polyfrost.multi-version.root") version pgtVersion
        id("org.polyfrost.defaults") version pgtVersion
        id("org.polyfrost.defaults.repo") version pgtVersion
        id("org.polyfrost.defaults.java") version pgtVersion
        id("org.polyfrost.multi-version.api-validation") version pgtVersion
    }
    dependencyResolutionManagement {
        versionCatalogs {
            create("pgtLibs") {
                plugin("pgtRoot", "org.polyfrost.multi-version.root").version(pgtVersion)
                plugin("pgt", "org.polyfrost.multi-version").version(pgtVersion)
                plugin("pgtDefaults", "org.polyfrost.defaults").version(pgtVersion)
                plugin("pgtDefaultRepo", "org.polyfrost.defaults.repo").version(pgtVersion)
                plugin("pgtDefaultJava", "org.polyfrost.defaults.java").version(pgtVersion)
                plugin("pgtAbi", "org.polyfrost.multi-version.api-validation").version(pgtVersion)
            }
        }
    }

}

val mod_name: String by settings

rootProject.name = mod_name

include(":platform", ":config", ":config-impl", ":commands", ":hud", "events", ":ui")
project(":platform").apply {
    projectDir = file("versions/")
    buildFileName = "root.gradle.kts"
}

listOf(
    "1.8.9-forge",
    "1.8.9-fabric",
    "1.12.2-fabric",
    "1.12.2-forge",
    "1.16.5-forge",
    "1.16.5-fabric"
).forEach { version ->
    include(":platform:$version")
    project(":platform:$version").apply {
        projectDir = file("versions/$version")
        buildFileName = "../build.gradle.kts"
    }
}
