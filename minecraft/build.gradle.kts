@file:Suppress("UnstableApiUsage")
// Shared build logic for all versions of OneConfig.

import dev.deftu.gradle.utils.GameSide
import dev.deftu.gradle.utils.propertyBoolOr
import dev.deftu.gradle.utils.version.MinecraftReleaseVersion
import dev.deftu.gradle.utils.version.MinecraftVersions
import org.polyfrost.gradle.provideIncludedDependencies
import java.text.SimpleDateFormat

plugins {
    java
    alias(libs.plugins.kotlin)
    id(libs.plugins.dgt.multiversion.platform.get().pluginId)
    id(libs.plugins.dgt.base.get().pluginId)
    id(libs.plugins.dgt.resources.get().pluginId)
    id(libs.plugins.dgt.loom.get().pluginId)
    id(libs.plugins.dgt.publishing.maven.get().pluginId)
}

if (mcData.isForge) {
    loom.forge.mixinConfig("mixins.oneconfigv1.init.json")
}

toolkitLoomHelper {
    disableRunConfigs(GameSide.SERVER)

    useDevAuth("+")

    useProperty("mixin.debug.export", "true", GameSide.CLIENT)
    useProperty("debugBytecode", "true", GameSide.CLIENT)
    useProperty("forge.logging.console.level", "debug", GameSide.CLIENT)
    if (org.gradle.internal.os.OperatingSystem.current().isMacOsX) {
        useProperty("fml.earlyprogresswindow", "false", GameSide.CLIENT)
    }

    if (mcData.isForge) {
        useForgeMixin("oneconfigv1")
    }

    if (mcData.isLegacyForge) {
        useTweaker("org.polyfrost.oneconfig.internal.legacy.OneConfigTweaker")
    }
}

java {
    withSourcesJar()
}

repositories {
    maven("https://repo.polyfrost.org/releases")
    maven("https://repo.polyfrost.org/snapshots")
    maven("https://repo.hypixel.net/repository/Hypixel/")
    maven("https://maven.deftu.dev/releases")
    maven("https://maven.notenoughupdates.org/releases")
}

if (mcData.isLegacyForge) { // Quick substitution for relaunch in dev env, so that mixinextras works properly (yay!)
    configurations.all {
        resolutionStrategy {
            dependencySubstitution {
                all {
                    if (requested is ModuleComponentSelector) {
                        val module = (requested as ModuleComponentSelector)
                        if (module.group == "org.ow2.asm" && module.version == "5.0.3") {
                            logger.warn("Substituting ${module.group}:${module.module}:${module.version} with ${libs.asm.get()}")
                            useTarget(module.group + ":" + module.module + ":" + libs.asm.get().version)
                        }
                    }
                }
            }
        }
    }
}

dependencies {
    compileOnly("gg.essential:vigilance-1.8.9-forge:295") { isTransitive = false }
    compileOnly("org.notenoughupdates.moulconfig:common:3.11.0") { isTransitive = false }

    val mcVersion = mcData.version as MinecraftReleaseVersion
    provideIncludedDependencies(
        Triple(mcVersion.major, mcVersion.minor, mcVersion.patch),
        mcData.loader.friendlyString
    ).forEach {
        if (it.dep is String) {
            handleApiDep(it.dep as String, it.mod)
        } else {
            handleApiDep(it.dep as ExternalModuleDependency, it.mod)
        }
    }

    annotationProcessor(libs.mixin.extras)

    for (dep in listOf("-nanovg").run {
        if (mcData.version < MinecraftVersions.VERSION_1_13) this else this + listOf(
            "-tinyfd",
            "-stb",
            ""
        )
    }) {
        val lwjglDep = "org.lwjgl:lwjgl$dep:${libs.versions.lwjgl.get()}"
        compileOnlyApi(lwjglDep) {
            isTransitive = false
        }
    }

    implementation(project(":modules:dependencies:bundled"))
    implementation(project(":modules:internal")) {
        isTransitive = false
    }

    if (mcData.isLegacyForge) {
        compileOnly("cc.polyfrost:oneconfig-$mcData:0.2.2-alpha216") {
            isTransitive = false
        }
    }

    if (mcData.isLegacyForge || mcData.isLegacyFabric) {
        handleApiDep("com.mojang:brigadier:1.0.18")
    }

    api("dev.deftu:enhancedeventbus:2.0.0") // TODO

    if (mcData.isFabric) {
        modImplementation("net.fabricmc:fabric-language-kotlin:${mcData.dependencies.fabric.fabricLanguageKotlinVersion}")

        if (mcData.isLegacyFabric) {
            // 1.8.9 - 1.13
            modImplementation("net.legacyfabric.legacy-fabric-api:legacy-fabric-api:${mcData.dependencies.legacyFabric.legacyFabricApiVersion}")
        } else {
            // 1.16.5+
            if (mcVersion.minor == 21 && mcVersion.patch == 5) {
                modImplementation("net.fabricmc.fabric-api:fabric-api:0.126.0+1.21.5")
            } else
                modImplementation("net.fabricmc.fabric-api:fabric-api:${mcData.dependencies.fabric.fabricApiVersion}")
        }
    }

    if (propertyBoolOr("loom.appleSiliconFix", true) && mcData.version < MinecraftVersions.VERSION_1_13) {
        if (
            System.getProperty("os.arch") == "aarch64" &&
            System.getProperty("os.name") == "Mac OS X"
        ) {
            logger.error("Setting up fix with Apple Silicon for Minecraft ${mcData.version}")

            repositories {
                maven("https://maven.legacyfabric.net/") {
                    content {
                        includeGroup("org.lwjgl.lwjgl")
                    }
                }
            }

            val lwjglVersion = "2.9.4+legacyfabric.8"

            configurations.all {
                resolutionStrategy {
                    dependencySubstitution {
                        all {
                            if (requested is ModuleComponentSelector) {
                                val module = (requested as ModuleComponentSelector)
                                if (module.group == "org.lwjgl.lwjgl") {
                                    logger.warn("Substituting ${module.group}:${module.module}:${module.version} with ${module.group}:${module.module}:$lwjglVersion")
                                    useTarget(module.group + ":" + module.module + ":" + lwjglVersion)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun DependencyHandlerScope.handleApiDep(dependency: String, isMod: Boolean = false) {
    val dep = project.dependencies.create(dependency) as ExternalModuleDependency
    handleApiDep(dep, isMod)
}

fun DependencyHandlerScope.handleApiDep(dependency: Provider<MinimalExternalModuleDependency>, isMod: Boolean = false) {
    handleApiDep(dependency.get(), isMod)
}

fun DependencyHandlerScope.handleApiDep(dependency: ExternalModuleDependency, isMod: Boolean = false) {
    val dep = "${dependency.group}:${dependency.name}:${dependency.version}"
    if (isMod) modApi(dep) {
        isTransitive = false
    } else api(dep) {
        isTransitive = false
    }
}

tasks {
    withType(Jar::class) {
        exclude("**/**_Test.**")
        exclude("**/**_Test$**.**")
    }
    remapJar {
        manifest {
            val attributesMap = buildMap<String, Any> {
                putAll(
                    mapOf(
                        "Specification-Title" to modData.id,
                        "Specification-Vendor" to "Polyfrost",
                        "Specification-Version" to "1", // We are version 1 of ourselves, whatever the hell that means
                        "Implementation-Title" to rootProject.name,
                        "Implementation-Version" to project.version,
                        "Implementation-Vendor" to "Polyfrost",
                        "Implementation-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(`java.util`.Date()),
                        "OneConfig-Main-Class" to "org.polyfrost.oneconfig.internal.bootstrap.Bootstrap",
                        "MixinConfigs" to "mixins.oneconfigv1.init.json,mixins.oneconfigv1.json",
                    )
                )
            }
            attributes(attributesMap)
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            named<MavenPublication>("mavenJava") {
                groupId = group.toString()
                artifactId = mcData.toString()

                signing {
                    isRequired = project.properties["signing.keyId"] != null
                    sign(this@named)
                }
            }
        }
    }
}