pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.polyfrost.cc/releases")
    }
    val pgtVersion = "0.2.3"
    plugins {
        id("cc.polyfrost.multi-version.root") version pgtVersion
        id("cc.polyfrost.defaults") version pgtVersion
        id("cc.polyfrost.defaults.repo") version pgtVersion
        id("cc.polyfrost.defaults.java") version pgtVersion
        id("cc.polyfrost.multi-version.api-validation") version pgtVersion
    }
    dependencyResolutionManagement {
        versionCatalogs {
            create("pgtLibs") {
                plugin("pgtRoot", "cc.polyfrost.multi-version.root").version(pgtVersion)
                plugin("pgt", "cc.polyfrost.multi-version").version(pgtVersion)
                plugin("pgtDefaults", "cc.polyfrost.defaults").version(pgtVersion)
                plugin("pgtDefaultRepo", "cc.polyfrost.defaults.repo").version(pgtVersion)
                plugin("pgtDefaultJava", "cc.polyfrost.defaults.java").version(pgtVersion)
                plugin("pgtAbi", "cc.polyfrost.multi-version.api-validation").version(pgtVersion)
            }
        }
    }

}

val mod_name: String by settings

rootProject.name = mod_name

include(":platform")
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
