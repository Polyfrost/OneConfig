@file:Suppress("DSL_SCOPE_VIOLATION")

import net.fabricmc.loom.task.RemapSourcesJarTask
import org.polyfrost.gradle.util.RelocationTransform.Companion.registerRelocationAttribute
import org.polyfrost.gradle.util.noServerRunConfigs
import org.polyfrost.gradle.util.prebundle
import java.text.SimpleDateFormat
import java.util.concurrent.atomic.AtomicReference

plugins {
    alias(libs.plugins.kotlin)
    id(pgtLibs.plugins.pgt.get().pluginId)
    id(pgtLibs.plugins.pgtDefaults.get().pluginId)
    id(libs.plugins.blossom.get().pluginId)
    id(libs.plugins.shadow.get().pluginId)
    id("maven-publish")
    id("signing")
    java
}

kotlin.jvmToolchain {
    (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(8))
}

java {
    withSourcesJar()
    withJavadocJar()
}

val availablePlatforms = listOf("windows", "windows-arm64", "linux", "macos", "macos-arm64")
val modName = project.properties["mod_name"]
val modMajor = project.properties["mod_major_version"]
val modMinor = project.properties["mod_minor_version"]
val modId = project.properties["mod_id"] as String

preprocess {
    vars.put("MODERN", if (project.platform.mcMinor >= 16) 1 else 0)
}

version = "$modMajor$modMinor"
group = "org.polyfrost"

blossom {
    replaceToken("@VER@", version)
    replaceToken("@NAME@", modName)
    replaceToken("@ID@", modId)
}

base {
    archivesName.set("$modId-$platform")
}

loom {
    noServerRunConfigs()
    runConfigs {
        "client" {
            if (project.platform.isLegacyForge) {
                programArgs("--tweakClass", "org.polyfrost.oneconfig.internal.plugin.asm.OneConfigTweaker")
            }
            property("mixin.debug.export", "true")
            property("debugBytecode", "true")
            property("forge.logging.console.level", "debug")
            if (org.gradle.internal.os.OperatingSystem.current().isMacOsX) {
                property("fml.earlyprogresswindow", "false")
            }
        }
    }
    if (project.platform.isForge) {
        forge {
            mixinConfig("mixins.${modId}.json")
        }
    }
    @Suppress("UnstableApiUsage")
    mixin.defaultRefmapName.set("mixins.${modId}.refmap.json")
}

repositories {
    maven("https://repo.polyfrost.org/releases")
}

val relocatedCommonProject = registerRelocationAttribute("common-lwjgl") {
    if (platform.isModLauncher || platform.isFabric) {
        relocate("org.lwjgl3.buffer", "org.lwjgl3")
    }
}

val relocated = registerRelocationAttribute("relocate") {
    relocate("com.github.benmanes", "org.polyfrost.oneconfig.libs")
    relocate("dev.xdark", "org.polyfrost.oneconfig.libs")
    remapStringsIn("com.github.benmanes.caffeine.cache.LocalCacheFactory")
    remapStringsIn("com.github.benmanes.caffeine.cache.NodeFactory")
}

val implementationNoPom: Configuration by configurations.creating {
    configurations.named(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME) { extendsFrom(this@creating) }
    configurations.named(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME) { extendsFrom(this@creating) }
}

val modImplementationNoPom: Configuration by configurations.creating {
    configurations.modImplementation.get().extendsFrom(this)
    configurations.modRuntimeOnly.get().extendsFrom(this)
}

val shadeProject: Configuration by configurations.creating {
    attributes { attribute(relocatedCommonProject, false) }
}

val shadeRelocated: Configuration by configurations.creating {
    attributes { attribute(relocated, true) }
}

val shade: Configuration by configurations.creating {
    configurations.api.get().extendsFrom(this)
}

val shadeNoPom: Configuration by configurations.creating

val shadeOnly: Configuration by configurations.creating

val shadeNoJar: Configuration by configurations.creating

dependencies {
    compileOnly("gg.essential:vigilance-1.8.9-forge:${libs.versions.vigilance.get()}") {
        isTransitive = false
    }

    include("org.polyfrost:universalcraft-$platform:${libs.versions.universalcraft.get()}", transitive = false, mod = true)

    if (platform.isLegacyFabric) include(libs.deencapsulation, relocate = true, transitive = false, mod = false)

    include(libs.caffeine, relocate = true)

    include(libs.polyui)

    include(libs.slf4jApi)
    include(libs.slf4jSimple)

    // for other mods and universalcraft
    include(libs.bundles.kotlin)

    if (platform.isLegacyForge) {
        implementationNoPom(shadeNoJar(libs.mixin.get().run { "$group:$name:$version" }) {
            isTransitive = false
        })
    }
    shadeProject(project(":")) {
        isTransitive = false
    }
    shadeProject(project(":config"))
    shadeProject(project(":commands"))
    shadeProject(project(":hud"))
    shadeProject(project(":events"))
    shadeProject(project(":config-impl"))
    shadeProject(project(":ui")) {
        isTransitive = false
    }

    if (platform.isFabric) {
        include(libs.fabricAsm)
    }

    val isLegacy = platform.isLegacyForge || platform.isLegacyFabric
    val lwjglVersion = libs.versions.lwjgl.get()
    if(isLegacy) {
        val cfg = configurations.create("bundledLwjgl")
        for(dep in listOf("nanovg", "tinyfd", "stb", null)) {
            val lwjglDep = if(dep == null) "org.lwjgl:lwjgl:$lwjglVersion" else "org.lwjgl:lwjgl-$dep:$lwjglVersion"
            compileOnly(cfg(lwjglDep) {
                isTransitive = false
            })
            for(native in availablePlatforms) {
                runtimeOnly(cfg("$lwjglDep:natives-$native") {
                    isTransitive = false
                })
            }
        }
        shadeNoPom(implementationNoPom(prebundle(cfg, "lwjgl-legacy.jar"))!!)
    } else {
        for(dep in listOf("nanovg", "tinyfd")) {
            val lwjglDep = "org.lwjgl:lwjgl-$dep:$lwjglVersion"
            shade(lwjglDep) {
                isTransitive = false
            }
            for(native in availablePlatforms) {
                shade("$lwjglDep:natives-$native") {
                    isTransitive = false
                }
            }
        }
    }

    if(!platform.isLegacyFabric) {
        modRuntimeOnly(
            "me.djtheredstoner:DevAuth-" +
                    (if (platform.isForge) {
                        if (platform.isLegacyForge) "forge-legacy" else "forge-latest"
                    } else "fabric")
                    + ":1.1.2"
        )
    }

    configurations.named(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME) { extendsFrom(shadeProject) }
    configurations.named(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME) { extendsFrom(shadeProject) }
}

tasks {
    withType(Jar::class) {
        val atomicLines = AtomicReference(listOf<String>())

        // This removes the 24th line in fabric.mod.json,
        // aka the TestMod entrypoint, for production builds.
        doFirst {
            val fabricModJson = buildDir.resolve("resources")
                .resolve("main")
                .resolve("fabric.mod.json")
            if (fabricModJson.exists()) {
                val lines = fabricModJson.readLines()
                if (lines[23].contains("TestMod")) {
                    atomicLines.set(lines)

                    fabricModJson.delete()
                    fabricModJson.writeText(
                        lines.subList(0, 23).joinToString("\n") + "\n" + lines.subList(
                            24,
                            lines.size
                        ).joinToString("\n")
                    )
                }
            }
        }

        doLast {
            val lines = atomicLines.get()
            if (lines.isEmpty()) return@doLast

            val fabricModJson = buildDir.resolve("resources")
                .resolve("main")
                .resolve("fabric.mod.json")

            fabricModJson.delete()
            fabricModJson.writeText(lines.joinToString("\n"))
        }
    }
    processResources {
        inputs.property("id", modId)
        inputs.property("name", modName)
        val java = if (project.platform.mcMinor >= 18) {
            17
        } else {
            if (project.platform.mcMinor == 17) 16 else 8
        }
        val compatLevel = "JAVA_${java}"
        inputs.property("java", java)
        inputs.property("java_level", compatLevel)
        inputs.property("version", project.version)
        inputs.property("mcVersionStr", project.platform.mcVersionStr)

        filesMatching(listOf("mcmod.info", "mixins.${modId}.json", "**/mods.toml")) {
            expand(
                mapOf(
                    "id" to modId,
                    "name" to modName,
                    "java" to java,
                    "java_level" to compatLevel,
                    "version" to project.version,
                    "mcVersionStr" to project.platform.mcVersionStr
                )
            )
        }
        filesMatching("fabric.mod.json") {
            expand(
                mapOf(
                    "id" to modId,
                    "name" to modName,
                    "java" to java,
                    "java_level" to compatLevel,
                    "version" to project.version,
                    "mcVersionStr" to project.platform.mcVersionStr.substringBeforeLast(".") + ".x"
                )
            )
        }
    }

    withType(Jar::class.java) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        if (project.platform.isFabric) {
            exclude("mcmod.info", "META-INF/mods.toml")
        } else {
            exclude("fabric.mod.json")
            if (project.platform.isLegacyForge) {
                exclude("**/mods.toml")
                exclude("META-INF/versions/**")
                exclude("**/module-info.class")
                exclude("**/package-info.class")
            } else {
                exclude("mcmod.info")
            }
        }
        if (!name.contains("sourcesjar", ignoreCase = true) || !name.contains("javadoc", ignoreCase = true)) {
            exclude("**/**_Test.**")
            exclude("**/**_Test$**.**")
            exclude("testmod_dark.svg")
        }
    }

    shadowJar {
        archiveClassifier.set("full-dev")
        configurations = listOf(shade, shadeNoPom, shadeNoJar, shadeProject, shadeRelocated, shadeOnly)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(jar)
    }

    remapJar {
        inputFile.set(shadowJar.get().archiveFile)
        archiveClassifier = "full"
    }

    fun Jar.excludeInternal() {
        exclude("**/internal/**")
        exclude("**/commands/ClientCommandHandler.**")
    }
    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(shadeNoPom, shadeProject, shadeRelocated)
        from(ArrayList<File>().run { addAll(shadeNoPom); addAll(shadeProject); addAll(shadeRelocated); this }
            .map { if (it.isDirectory) it else zipTree(it) })
        manifest {
            attributes(
                if (platform.isForge) {
                    if (platform.isLegacyForge) {
                        mapOf(
                            "ModSide" to "CLIENT",
                            "ForceLoadAsMod" to true,
                            "TweakOrder" to "0",
                            "MixinConfigs" to "mixins.oneconfig.json",
                            "TweakClass" to "org.polyfrost.oneconfig.internal.plugin.asm.OneConfigTweaker"
                        )
                    } else {
                        mapOf(
                            "MixinConfigs" to "mixins.oneconfig.json",
                            "Specification-Title" to modId,
                            "Specification-Vendor" to modId,
                            "Specification-Version" to "1", // We are version 1 of ourselves, whatever the hell that means
                            "Implementation-Title" to modName,
                            "Implementation-Version" to project.version,
                            "Implementation-Vendor" to modId,
                            "Implementation-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(`java.util`.Date())
                        )
                    }
                } else {
                    mapOf()
                }
            )
        }
        excludeInternal()
        archiveClassifier.set("")
    }
    named<Jar>("sourcesJar") {
        from(project(":").sourceSets.main.map { it.allSource })
        excludeInternal()
        archiveClassifier.set("sources")
        doFirst {
            archiveClassifier.set("sources")
        }
        doLast {
            archiveFile.orNull?.asFile?.let {
                it.copyTo(
                    File(
                        it.parentFile,
                        it.nameWithoutExtension + "-dev" + it.extension.let { if (it.isBlank()) "" else ".$it" }),
                    overwrite = true
                )
            }
            archiveClassifier.set("sources")
        }
    }
    named<Jar>("javadocJar") {
        archiveClassifier.set("javadoc")
    }
    withType<RemapSourcesJarTask> {
        enabled = false
    }
}

publishing {
    publications {
        register<MavenPublication>("oneconfig-$platform") {
            groupId = "org.polyfrost"
            artifactId = base.archivesName.get()

            artifact(tasks["jar"])
            artifact(tasks["remapJar"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
        }
    }

    repositories {
        maven {
            name = "releases"
            url = uri("https://repo.polyfrost.org/releases")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
        maven {
            name = "snapshots"
            url = uri("https://repo.polyfrost.org/snapshots")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
        maven {
            name = "private"
            url = uri("https://repo.polyfrost.org/private")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

fun DependencyHandlerScope.include(dependency: Any, pom: Boolean = true, mod: Boolean = false) {
    if (platform.isForge) {
        if (pom) {
            shade(dependency)
        } else {
            shadeNoPom(dependency)
            implementationNoPom(dependency)
        }
    } else {
        if (pom) {
            if (mod) {
                modApi(dependency)
            } else {
                api(dependency)
            }
        } else {
            if (mod) {
                modImplementationNoPom(dependency)
            } else {
                implementationNoPom(dependency)
            }
        }
        "include"(dependency)
    }
}

fun DependencyHandlerScope.include(dependency: Provider<MinimalExternalModuleDependency>, pom: Boolean = true, mod: Boolean = false, relocate: Boolean = false, transitive: Boolean = true) {
    include(dependency.get().run { "$group:$name:$version" }, pom, mod, relocate, transitive)
}

fun DependencyHandlerScope.include(
    dependency: String,
    pom: Boolean = true,
    mod: Boolean = false,
    relocate: Boolean = false,
    transitive: Boolean = true,
) {
    if (platform.isForge) {
        if (relocate) {
            shadeRelocated(dependency) { isTransitive = transitive }
            implementationNoPom(dependency) { isTransitive = transitive; attributes { attribute(relocated, true) } }
        } else {
            if (pom) {
                shade(dependency) { isTransitive = transitive }
            } else {
                shadeNoPom(dependency) { isTransitive = transitive }
                implementationNoPom(dependency) {
                    isTransitive = transitive
                }
            }
        }
    } else {
        if (pom && !relocate) {
            if (mod) {
                modApi(dependency) { isTransitive = transitive }
            } else {
                api(dependency) { isTransitive = transitive }
            }
        } else {
            if (mod) {
                modImplementationNoPom(dependency) {
                    isTransitive = transitive; if (relocate) attributes {
                    attribute(
                        relocated,
                        true
                    )
                }
                }
            } else {
                implementationNoPom(dependency) {
                    isTransitive = transitive; if (relocate) attributes {
                    attribute(
                        relocated,
                        true
                    )
                }
                }
            }
        }
        "include"(dependency) { isTransitive = transitive; if (relocate) attributes { attribute(relocated, true) } }
    }
}
