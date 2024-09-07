import org.jetbrains.gradle.ext.settings
import org.jetbrains.gradle.ext.taskTriggers

plugins {
    alias(libs.plugins.jetbrains.idea.ext)
}

val lwjglVersion = libs.versions.lwjgl.get() // also duplicated in root.gradle.kts
version = lwjglVersion

description = "Dependencies for legacy platforms (<1.12)"

val natives = listOf("windows", "windows-arm64", "linux", "macos", "macos-arm64")

dependencies {
    for (dep in listOf("-nanovg", "-tinyfd", "-stb", "")) {
        val lwjglDep = "org.lwjgl:lwjgl$dep:$lwjglVersion"
        api(lwjglDep) {
            isTransitive = false
        }
        for (native in natives) {
            implementation("$lwjglDep:natives-$native") {
                isTransitive = false
            }
        }
    }
}

project.tasks.withType(Jar::class.java).configureEach {
    archiveVersion.set(lwjglVersion)
}

val build = project.tasks.getByName("build")
val jar = project.tasks.withType(Jar::class.java).getByName("jar")

rootProject.idea.project.settings {
    taskTriggers {
        if (!jar.archiveFile.get().asFile.exists()) {
            afterSync(build)
            beforeBuild(build)
            logger.warn("Building legacy LWJGL jar after sync for you...")
        }
    }
}