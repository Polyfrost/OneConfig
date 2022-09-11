@file:Suppress("GradlePackageUpdate")

import gg.essential.gradle.util.RelocationTransform.Companion.registerRelocationAttribute
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.io.Closeable
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream
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

version = "1.0.0-alpha19"

repositories {
    maven("https://repo.polyfrost.cc/releases")
}

val relocate = registerRelocationAttribute("relocate") {
    relocate("org.lwjgl.nanovg", "org.lwjgl3.nanovg")
}

val shadeCompileOnly: Configuration by configurations.creating {
    if (platform.mcVersion >= 11600) {
        attributes { attribute(relocate, true) }
    }
}
val shadeSeparate: Configuration by configurations.creating {
    if (platform.mcVersion >= 11600) {
        attributes { attribute(relocate, true) }
    }
}

dependencies {
    val lwjglVersion = if (platform.mcVersion >= 11600) {
        "3.2.1"
    } else {
        "3.3.1"
    }
    if (platform.isLegacyForge || platform.isLegacyFabric) {
        shadeCompileOnly("org.lwjgl:lwjgl:$lwjglVersion")
        shadeCompileOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-windows")
        shadeCompileOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-linux")
        shadeCompileOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-macos")
        shadeCompileOnly("org.lwjgl:lwjgl:3.3.1:natives-macos-arm64")

        shadeCompileOnly("org.lwjgl:lwjgl-stb:$lwjglVersion")
        shadeCompileOnly("org.lwjgl:lwjgl-tinyfd:$lwjglVersion")

        shadeCompileOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:natives-windows")
        shadeCompileOnly("org.lwjgl:lwjgl-tinyfd:$lwjglVersion:natives-windows")
        shadeCompileOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:natives-linux")
        shadeCompileOnly("org.lwjgl:lwjgl-tinyfd:$lwjglVersion:natives-linux")
        shadeCompileOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:natives-macos")
        shadeCompileOnly("org.lwjgl:lwjgl-tinyfd:$lwjglVersion:natives-macos")
        shadeCompileOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:natives-macos-arm64")
        shadeCompileOnly("org.lwjgl:lwjgl-tinyfd:$lwjglVersion:natives-macos-arm64")
    }

    shadeCompileOnly("org.lwjgl:lwjgl-nanovg:$lwjglVersion") {
        isTransitive = false
    }
    shadeCompileOnly("org.lwjgl:lwjgl-nanovg:$lwjglVersion:natives-windows") {
        isTransitive = false
    }
    shadeCompileOnly("org.lwjgl:lwjgl-nanovg:$lwjglVersion:natives-linux") {
        isTransitive = false
    }
    shadeCompileOnly("org.lwjgl:lwjgl-nanovg:$lwjglVersion:natives-macos") {
        isTransitive = false
    }
    // force 3.3.1 for this, because
    // if the user is actually running M1+, LWJGL must be 3.3.0+
    shadeSeparate("org.lwjgl:lwjgl-nanovg:3.3.1:natives-macos-arm64") {
        isTransitive = false
    }
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        configurations = listOf(shadeCompileOnly, shadeSeparate)
        if (platform.isLegacyForge || platform.isLegacyFabric) {
            exclude("META-INF/versions/**")
            exclude("**/module-info.class")
            exclude("**/package-info.class")
        }
        if (platform.mcVersion < 11600) {
            relocate("org.lwjgl", "org.lwjgl3")
        }
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(jar)

        if (platform.mcVersion < 11600) {
            doLast {
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
                val original = shadowJar.get().archiveFile.get().asFile
                val input = File(original.parentFile, "${original.nameWithoutExtension}-donotusethis.jar")
                original.renameTo(input)
                val output = File(input.parentFile, input.name.replace("-donotusethis", ""))
                useInOut((input to output)) { jarIn, jarOut ->
                    while (true) {
                        val entry = jarIn.nextJarEntry ?: break
                        var modifiedBytes = jarIn.readBytes()
                        if (entry.name.endsWith(".class")) {
                            val reader = ClassReader(modifiedBytes)
                            val node = ClassNode()
                            reader.accept(node, ClassReader.EXPAND_FRAMES)

                            node.methods.forEach {
                                if (it.name == "<clinit>") {
                                    it.instructions.forEach { insn ->
                                        if (insn is org.objectweb.asm.tree.LdcInsnNode) {
                                            if (insn.cst is String) {
                                                val str = insn.cst as String
                                                if (str == "lwjgl") {
                                                    insn.cst = "lwjgl3"
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)
                            try {
                                node.accept(cw)
                            } catch (t: Throwable) {
                                logger.error("Exception when transforming " + entry.name + " : " + t.javaClass.simpleName)
                                t.printStackTrace()
                            }
                            modifiedBytes = cw.toByteArray()
                        }
                        val beforeName = entry.name.substringBeforeLast("/")
                        val afterName = entry.name.substringAfterLast("/")
                        jarOut.putNextEntry(ZipEntry(lwjglNatives[afterName]?.let { "$beforeName/$it" } ?: entry.name))
                        jarOut.write(modifiedBytes)
                        jarOut.closeEntry()
                    }
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
            block(jarIn, jarOut)
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