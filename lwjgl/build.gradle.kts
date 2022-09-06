@file:Suppress("GradlePackageUpdate")

import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream
import java.io.Closeable
import java.util.zip.ZipEntry

plugins {
    kotlin("jvm")
    id("gg.essential.multi-version")
    id("gg.essential.defaults.repo")
    id("gg.essential.defaults.java")
    id("gg.essential.defaults.loom")
    id("com.github.johnrengelman.shadow")
    id("maven-publish")
    id("signing")
    java
}

val mod_name: String by project
val mod_version: String by project
val mod_id: String by project

version = "1.0.0-alpha14"

repositories {
    maven("https://repo.polyfrost.cc/releases")
}
val shadeCompileOnly: Configuration by configurations.creating
val shadeRuntimeOnly: Configuration by configurations.creating

sourceSets {
    main {
        runtimeClasspath += shadeRuntimeOnly
    }
}

dependencies {
    val lwjglVersion = if (platform.mcVersion >= 11600) {
        "3.2.1"
    } else {
        "3.3.1"
    }
    if (platform.isLegacyForge || platform.isLegacyFabric) {
        shadeCompileOnly("org.lwjgl:lwjgl-stb:$lwjglVersion")
        shadeCompileOnly("org.lwjgl:lwjgl-tinyfd:$lwjglVersion")

        shadeRuntimeOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:natives-windows")
        shadeRuntimeOnly("org.lwjgl:lwjgl-tinyfd:$lwjglVersion:natives-windows")
        shadeRuntimeOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:natives-linux")
        shadeRuntimeOnly("org.lwjgl:lwjgl-tinyfd:$lwjglVersion:natives-linux")
        shadeRuntimeOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:natives-macos")
        shadeRuntimeOnly("org.lwjgl:lwjgl-tinyfd:$lwjglVersion:natives-macos")
        shadeRuntimeOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:natives-macos-arm64")
        shadeRuntimeOnly("org.lwjgl:lwjgl-tinyfd:$lwjglVersion:natives-macos-arm64")
    }

    shadeCompileOnly("org.lwjgl:lwjgl:$lwjglVersion")
    shadeRuntimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-windows")
    shadeRuntimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-linux")
    shadeRuntimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-macos")
    shadeRuntimeOnly("org.lwjgl:lwjgl:3.3.1:natives-macos-arm64")

    shadeCompileOnly("org.lwjgl:lwjgl-nanovg:$lwjglVersion") {
        isTransitive = false
    }
    shadeRuntimeOnly("org.lwjgl:lwjgl-nanovg:$lwjglVersion:natives-windows") {
        isTransitive = false
    }
    shadeRuntimeOnly("org.lwjgl:lwjgl-nanovg:$lwjglVersion:natives-linux") {
        isTransitive = false
    }
    shadeRuntimeOnly("org.lwjgl:lwjgl-nanovg:$lwjglVersion:natives-macos") {
        isTransitive = false
    }
    // force 3.3.1 for this, because
    // if the user is actually running M1+, LWJGL must be 3.3.0+
    shadeRuntimeOnly("org.lwjgl:lwjgl-nanovg:3.3.1:natives-macos-arm64") {
        isTransitive = false
    }
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        configurations = listOf(shadeCompileOnly, shadeRuntimeOnly)
        if (platform.isLegacyForge || platform.isLegacyFabric) {
            exclude("META-INF/versions/**")
            exclude("**/module-info.class")
            exclude("**/package-info.class")
        }
        relocate("lwjgl", "lwjgl3") {
            include("org/lwjgl/system/Library.class")
        }
        relocate("org.lwjgl", "org.lwjgl3")
        val lwjglNatives = mapOf(
            "liblwjgl.so" to "liblwjgl3.so",
            "liblwjgl.so.git" to "liblwjgl3.so.git",
            "liblwjgl.so.sha1" to "liblwjgl3.so.sha1",
            "liblwjgl.dylib" to "liblwjgl3.dylib",
            "liblwjgl.dylib.git" to "liblwjgl3.dylib.git",
            "liblwjgl.dylib.sha1" to "liblwjgl3.dylib.sha1",
            "lwjgl.dll" to "lwjgl3.dll",
            "lwjgl.dll.git" to "lwjgl3.dll.git",
            "lwjgl.dll.sha1" to "lwjgl3.dll.sha1"
        )
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(jar)

        doLast {
            val original = shadowJar.get().archiveFile.get().asFile
            val input = File(original.parentFile, "${original.nameWithoutExtension}-donotusethis.jar")
            original.renameTo(input)
            val output = File(input.parentFile, input.name.replace("-donotusethis", ""))
            useInOut((input to output)) { jarIn, jarOut ->
                while (true) {
                    val entry = jarIn.nextJarEntry ?: break
                    val beforeName = entry.name.substringBeforeLast("/")
                    val afterName = entry.name.substringAfterLast("/")
                    jarOut.putNextEntry(ZipEntry(lwjglNatives[afterName]?.let { "$beforeName/$it" } ?: entry.name))
                    jarOut.write(jarIn.readBytes())
                    jarOut.closeEntry()
                }
            }
        }
    }
    jar {
        enabled = false
    }
    remapJar {
        enabled = false
    }
}

/**
 * Taken from Essential Gradle Toolkit under GPL 3.0
 * https://github.com/EssentialGG/essential-gradle-toolkit/blob/master/LICENSE.md
 */
fun <T : Closeable, U : Closeable> T.nestedUse(nest: (T) -> U, block: (U) -> Unit) =
    use { nest(it).use(block) }

/**
 * Adapted from Essential Gradle Toolkit under GPL 3.0
 * https://github.com/EssentialGG/essential-gradle-toolkit/blob/master/LICENSE.md
 */
fun useInOut(inOut: Pair<File, File>, block: (jarIn: JarInputStream, jarOut: JarOutputStream) -> Unit) {
    inOut.first.inputStream().nestedUse(::JarInputStream) { jarIn ->
        inOut.second.outputStream().nestedUse(::JarOutputStream) { jarOut ->
            block.invoke(jarIn, jarOut)
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("lwjgl-$platform") {
            groupId = "cc.polyfrost"
            artifactId = "lwjgl-$platform"
            artifact(tasks["shadowJar"])
        }
    }

    repositories {
        maven {
            name = "releases"
            url = uri("https://repo.polyfrost.cc/releases")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
        maven {
            name = "snapshots"
            url = uri("https://repo.polyfrost.cc/snapshots")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
        maven {
            name = "private"
            url = uri("https://repo.polyfrost.cc/private")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}