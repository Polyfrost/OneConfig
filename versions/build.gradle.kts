@file:Suppress("DSL_SCOPE_VIOLATION")

import cc.polyfrost.gradle.util.RelocationTransform.Companion.registerRelocationAttribute
import cc.polyfrost.gradle.util.noServerRunConfigs
import cc.polyfrost.gradle.util.prebundle
import net.fabricmc.loom.task.RemapSourcesJarTask
import java.text.SimpleDateFormat

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
    languageVersion.set(JavaLanguageVersion.of(8))
}

java {
    withSourcesJar()
    withJavadocJar()
}

val modName = project.properties["mod_name"]
val modMajor = project.properties["mod_major_version"]
val modMinor = project.properties["mod_minor_version"]
val modId = project.properties["mod_id"] as String

preprocess {
    vars.put("MODERN", if (project.platform.mcMinor >= 16) 1 else 0)
}

version = "$modMajor$modMinor"
group = "cc.polyfrost"

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
    launchConfigs.named("client") {
        if (project.platform.isLegacyForge) {
            arg("--tweakClass", "cc.polyfrost.oneconfig.internal.plugin.asm.OneConfigTweaker")
        }
        property("mixin.debug.export", "true")
        property("debugBytecode", "true")
        property("forge.logging.console.level", "debug")
        if (org.gradle.internal.os.OperatingSystem.current().isMacOsX) {
            property("fml.earlyprogresswindow", "false")
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
    maven("https://repo.polyfrost.cc/releases")
}

val relocatedCommonProject = registerRelocationAttribute("common-lwjgl") {
    if (platform.isModLauncher || platform.isFabric) {
        relocate("org.lwjgl3.buffer", "org.lwjgl3")
    }
}

val relocated = registerRelocationAttribute("relocate") {
    relocate("gg.essential", "cc.polyfrost.oneconfig.libs")
    relocate("me.kbrewster", "cc.polyfrost.oneconfig.libs")
    relocate("com.github.benmanes", "cc.polyfrost.oneconfig.libs")
    relocate("com.google", "cc.polyfrost.oneconfig.libs")
    relocate("org.checkerframework", "cc.polyfrost.oneconfig.libs")
    relocate("dev.xdark", "cc.polyfrost.oneconfig.libs")

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

val shadeNoJar: Configuration by configurations.creating

sourceSets {
    main {
        if (project.platform.isForge) {
            output.setResourcesDir(java.classesDirectory)
        }
    }
}

private enum class RepackedVersion(val string: String) {
    LEGACY("legacy"), PRE119NOARM("pre-1.19-noarm"), PRE119ARM("pre-1.19-arm"), POST119("post-1.19");

    override fun toString(): String {
        return string
    }
}

dependencies {
    compileOnly(libs.vigilance) {
        isTransitive = false
    }

    include("cc.polyfrost:universalcraft-$platform:${libs.versions.universalcraft.get()}", transitive = false, mod = true)

    include(libs.deencapsulation, relocate = true, transitive = false, mod = false)

    include(libs.keventbus, relocate = true, transitive = false)

    include(libs.caffeine, relocate = true)

    // for other mods and universalcraft
    include(libs.kotlinStdlib)
    include(libs.kotlinStdlibJdk8)
    include(libs.kotlinStdlibJdk7)
    include(libs.kotlinReflect)

    include(libs.kotlinxCoroutinesCore)
    include(libs.kotlinxCoroutinesCoreJvm)
    include(libs.kotlinxCoroutinesJdk8)
    include(libs.kotlinxSerializationCore)
    include(libs.kotlinxSerializationJson)
    include(libs.kotlinxSerializationCbor)
    include(libs.kotlinxAtomicfu)

    if (platform.isLegacyForge) {
        implementationNoPom(shadeNoJar(libs.mixin.get().run { "$group:$name:$version" }) {
            isTransitive = false
        })
    }
    shadeProject(project(":")) {
        isTransitive = false
    }

    if (platform.isFabric) {
        include(libs.fabricAsm)
    }

    val repackedVersions = when (platform.mcVersion) {
        in 10809..11202 -> listOf(RepackedVersion.LEGACY)
        in 11203..11802 -> listOf(RepackedVersion.PRE119NOARM, RepackedVersion.PRE119ARM)
        else -> listOf(RepackedVersion.POST119)
    }

    repackedVersions.forEachIndexed { index, version ->
        val configuration = configurations.create("tempLwjglConfiguration$index")

        compileOnly(configuration("cc.polyfrost:lwjgl-$version:${libs.versions.lwjgl.get()}"){
            isTransitive = false
        })
        shadeNoPom(implementationNoPom(prebundle(configuration, "lwjgl-$version.jar"))!!)
    }

    configurations.named(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME) { extendsFrom(shadeProject) }
    configurations.named(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME) { extendsFrom(shadeProject) }
}

tasks {
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
        }
    }

    shadowJar {
        archiveClassifier.set("full-dev")
        configurations = listOf(shade, shadeNoPom, shadeNoJar, shadeProject, shadeRelocated)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(jar)
    }

    remapJar {
        input.set(shadowJar.get().archiveFile)
        archiveClassifier.set("full")
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
                            "TweakClass" to "cc.polyfrost.oneconfig.internal.plugin.asm.OneConfigTweaker"
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
        /*/

         */
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
                it.copyTo(File(it.parentFile, it.nameWithoutExtension + "-dev" + it.extension.let { if (it.isBlank()) "" else ".$it" }), overwrite = true)
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
            groupId = "cc.polyfrost"
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

fun DependencyHandlerScope.include(dependency: ModuleDependency, pom: Boolean = true, mod: Boolean = false, relocate: Boolean = false, transitive: Boolean = true) {
    if (platform.isForge) {
        if (relocate) {
            shadeRelocated(dependency) { isTransitive = transitive }
            implementationNoPom(dependency) { isTransitive = transitive; attributes { attribute(relocated, true) } }
        } else {
            if (pom) {
                shade(dependency) { isTransitive = transitive }
            } else {
                shadeNoPom(dependency) { isTransitive = transitive }
                implementationNoPom(dependency) { isTransitive = transitive }
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
                modImplementationNoPom(dependency) { isTransitive = transitive; if (relocate) attributes { attribute(relocated, true) } }
            } else {
                implementationNoPom(dependency) { isTransitive = transitive; if (relocate) attributes { attribute(relocated, true) } }
            }
        }
        "include"(dependency) { isTransitive = transitive; if (relocate) attributes { attribute(relocated, true) } }
    }
}

fun DependencyHandlerScope.include(dependency: Provider<MinimalExternalModuleDependency>, pom: Boolean = true, mod: Boolean = false, relocate: Boolean = false, transitive: Boolean = true) {
    include(dependency.get().run { "$group:$name:$version" }, pom, mod, relocate, transitive)
}

fun DependencyHandlerScope.include(dependency: String, pom: Boolean = true, mod: Boolean = false, relocate: Boolean = false, transitive: Boolean = true) {
    if (platform.isForge) {
        if (relocate) {
            shadeRelocated(dependency) { isTransitive = transitive }
            implementationNoPom(dependency) { isTransitive = transitive; attributes { attribute(relocated, true) } }
        } else {
            if (pom) {
                shade(dependency) { isTransitive = transitive }
            } else {
                shadeNoPom(dependency) { isTransitive = transitive }
                implementationNoPom(dependency) { isTransitive = transitive; if (relocate) attributes { attribute(relocated, true) } }
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
                modImplementationNoPom(dependency) { isTransitive = transitive; if (relocate) attributes { attribute(relocated, true) } }
            } else {
                implementationNoPom(dependency) { isTransitive = transitive; if (relocate) attributes { attribute(relocated, true) } }
            }
        }
        "include"(dependency) { isTransitive = transitive; if (relocate) attributes { attribute(relocated, true) } }
    }
}
