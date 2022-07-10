import gg.essential.gradle.util.RelocationTransform.Companion.registerRelocationAttribute
import gg.essential.gradle.util.noServerRunConfigs
import gg.essential.gradle.util.prebundle
import net.fabricmc.loom.task.RemapSourcesJarTask
import java.text.SimpleDateFormat


plugins {
    kotlin("jvm")
    id("gg.essential.multi-version")
    id("gg.essential.defaults.repo")
    id("gg.essential.defaults.java")
    id("gg.essential.defaults.loom")
    id("com.github.johnrengelman.shadow")
    id("net.kyori.blossom") version "1.3.0"
    id("org.jetbrains.dokka") version "1.6.21"
    id("maven-publish")
    id("signing")
    java
}

kotlin.jvmToolchain {
    (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(8))
}

java {
    withSourcesJar()
}

val mod_name: String by project
val mod_version: String by project
val mod_id: String by project

preprocess {
    vars.put("MODERN", if (project.platform.mcMinor >= 16) 1 else 0)
}

blossom {
    replaceToken("@VER@", mod_version)
    replaceToken("@NAME@", mod_name)
    replaceToken("@ID@", mod_id)
}

version = mod_version
group = "cc.polyfrost"
base {
    archivesName.set("$mod_id-$platform")
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
            mixinConfig("mixins.${mod_id}.json")
        }
    }
    mixin.defaultRefmapName.set("mixins.${mod_id}.refmap.json")
}

repositories {
    maven("https://repo.polyfrost.cc/releases")
}

val relocatedCommonProject = registerRelocationAttribute("common-lwjgl") {
    if (platform.isModLauncher || platform.isFabric) {
        relocate("org.lwjgl3", "org.lwjgl")
    }
}

val relocated = registerRelocationAttribute("relocate") {
    relocate("gg.essential", "cc.polyfrost.oneconfig.libs")
    relocate("me.kbrewster", "cc.polyfrost.oneconfig.libs")
    relocate("com.github.benmanes", "cc.polyfrost.oneconfig.libs")
    relocate("com.google", "cc.polyfrost.oneconfig.libs")
    relocate("org.checkerframework", "cc.polyfrost.oneconfig.libs")
    remapStringsIn("com.github.benmanes.caffeine.cache.LocalCacheFactory")
    remapStringsIn("com.github.benmanes.caffeine.cache.NodeFactory")
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

val shadeNoPom2: Configuration by configurations.creating

sourceSets {
    main {
        if (project.platform.isForge) {
            output.setResourcesDir(java.classesDirectory)
        }
    }
}

dependencies {
    compileOnly("gg.essential:vigilance-$platform:222") {
        isTransitive = false
    }

    shadeRelocated("gg.essential:universalcraft-$platform:211") {
        isTransitive = false
    }

    shadeRelocated("com.github.KevinPriv:keventbus:c52e0a2ea0") {
        isTransitive = false
    }

    @Suppress("GradlePackageUpdate") shadeRelocated("com.github.ben-manes.caffeine:caffeine:2.9.3")

    // for other mods and universalcraft
    val kotlinVersion: String by project
    val coroutinesVersion: String by project
    val serializationVersion: String by project
    val atomicfuVersion: String by project
    include("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    include("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    include("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")
    include("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    include("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    include("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutinesVersion")
    include("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
    include("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:$serializationVersion")
    include("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:$serializationVersion")
    include("org.jetbrains.kotlinx:kotlinx-serialization-cbor-jvm:$serializationVersion")
    include("org.jetbrains.kotlinx:atomicfu-jvm:$atomicfuVersion")

    if (platform.isLegacyForge) {
        shade("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
            isTransitive = false
        }
    }
    shadeProject(project(":")) {
        isTransitive = false
    }

    include("cc.polyfrost:lwjgl-$platform:1.0.0-alpha6")
    val prebundled = prebundle(shadeRelocated)
    include(prebundled, false, true)

    dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.6.21")

    configurations.named(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME) { extendsFrom(shadeProject) }
    configurations.named(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME) { extendsFrom(shadeProject) }
}

fun DependencyHandlerScope.include(dependency: Any, pom: Boolean = true, mod: Boolean = false) {
    if (platform.isForge) {
        if (pom) {
            shade(dependency)
        } else {
            if (mod) {
                modCompileOnly(dependency)
                modRuntimeOnly(dependency)
            } else {
                compileOnly(dependency)
                runtimeOnly(dependency)
            }
            shadeNoPom2(dependency)
        }
    } else {
        if (pom) {
            if (mod) modApi(dependency) else api(dependency)
        } else {
            if (mod) modRuntimeOnly(dependency) else runtimeOnly(dependency)
            if (mod) modCompileOnly(dependency) else compileOnly(dependency)
        }
        "include"(dependency)
    }
}

tasks {
    processResources {
        inputs.property("id", mod_id)
        inputs.property("name", mod_name)
        val java = if (project.platform.mcMinor >= 18) {
            17
        } else {
            if (project.platform.mcMinor == 17) 16 else 8
        }
        val compatLevel = "JAVA_${java}"
        inputs.property("java", java)
        inputs.property("java_level", compatLevel)
        inputs.property("version", mod_version)
        inputs.property("mcVersionStr", project.platform.mcVersionStr)
        filesMatching(listOf("mcmod.info", "mixins.${mod_id}.json", "**/mods.toml")) {
            expand(
                mapOf(
                    "id" to mod_id,
                    "name" to mod_name,
                    "java" to java,
                    "java_level" to compatLevel,
                    "version" to mod_version,
                    "mcVersionStr" to project.platform.mcVersionStr
                )
            )
        }
        filesMatching("fabric.mod.json") {
            expand(
                mapOf(
                    "id" to mod_id,
                    "name" to mod_name,
                    "java" to java,
                    "java_level" to compatLevel,
                    "version" to mod_version,
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
        if (!name.contains("sourcesjar", ignoreCase = true) || !name.contains("dokka", ignoreCase = true)) {
            exclude("**/**_Test.**")
            exclude("**/**_Test$**.**")
        }
    }

    val shadowJar = named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveClassifier.set("full-dev")
        configurations = listOf(shade, shadeNoPom2, shadeProject)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(jar)
    }

    remapJar {
        input.set(shadowJar.get().archiveFile)
        archiveClassifier.set("full")
    }

    fun Jar.excludeInternal() {
        exclude("**/internal/**")
    }
    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(shadeNoPom2, shadeProject)
        from(ArrayList<File>().run { addAll(shadeNoPom2); addAll(shadeProject); this }
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
                            "Specification-Title" to mod_id,
                            "Specification-Vendor" to mod_id,
                            "Specification-Version" to "1", // We are version 1 of ourselves, whatever the hell that means
                            "Implementation-Title" to mod_name,
                            "Implementation-Version" to mod_version,
                            "Implementation-Vendor" to mod_id,
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
    dokkaHtml.configure {
        outputDirectory.set(buildDir.resolve("dokka"))
        moduleName.set("OneConfig $platform")
        moduleVersion.set(mod_version)
        dokkaSourceSets {
            configureEach {
                jdkVersion.set(8)
                //reportUndocumented.set(true)
            }
        }
        doLast {
            val outputFile = outputDirectory.get().resolve("images/logo-icon.svg")
            if (outputFile.exists()) {
                outputFile.delete()
            }
            val inputFile = project.rootDir.resolve("src/main/resources/assets/oneconfig/icons/OneConfig.svg")
            inputFile.copyTo(outputFile)
        }
    }
    val dokkaJar = create("dokkaJar", Jar::class.java) {
        archiveClassifier.set("dokka")
        group = "build"
        dependsOn(dokkaHtml)
        from(layout.buildDirectory.dir("dokka"))
    }
    named<Jar>("sourcesJar") {
        from(project(":").sourceSets.main.map { it.allSource })
        dependsOn(dokkaJar)
        excludeInternal()
        archiveClassifier.set("sources")
        doFirst {
            archiveClassifier.set("sources")
        }
        doLast {
            archiveClassifier.set("sources")
        }
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
            artifact(tasks["dokkaJar"])
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