@file:Suppress("UnstableApiUsage")
// Shared build logic for all versions of OneConfig.

import org.polyfrost.gradle.util.noServerRunConfigs
import java.text.SimpleDateFormat

plugins {
    alias(libs.plugins.kotlin)
    id(libs.plugins.pgt.main.get().pluginId)
    id(libs.plugins.pgt.default.get().pluginId)
//    id(libs.plugins.shadow.get().pluginId)
}

val modId = properties["mod_id"] as String
val tweakClass = "org.polyfrost.oneconfig.internal.legacy.OneConfigTweaker"

base {
    archivesName = platform.toString()
}

java.withSourcesJar()

loom {
    noServerRunConfigs()
    runConfigs {
        "client" {
            if (project.platform.isLegacyForge) {
                programArgs("--tweakClass", tweakClass)
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
    mixin.defaultRefmapName = "mixins.${modId}.refmap.json"
}

dependencies {
    compileOnly("gg.essential:vigilance-1.8.9-forge:295") {
        isTransitive = false
    }

    implementation("org.polyfrost:universalcraft-$platform:${libs.versions.universalcraft.get()}")

    if (platform.isLegacyForge || platform.isLegacyFabric) {
        implementation(project(":modules:dependencies:legacy"))
    } else {
        implementation(project(":modules:dependencies:modern"))
    }
    implementation(project(":modules:dependencies"))
    implementation(project(":modules:dependencies:bundled"))

    if (platform.isLegacyForge) {
        implementation(libs.mixin) {
            isTransitive = false
        }
        compileOnly("cc.polyfrost:oneconfig-${platform}:0.2.2-alpha195") {
            isTransitive = false
        }
    }

    if (!platform.isLegacyFabric) {
        modRuntimeOnly(
            "me.djtheredstoner:DevAuth-" +
                    (if (platform.isForge) {
                        if (platform.isLegacyForge) "forge-legacy" else "forge-latest"
                    } else "fabric")
                    + ":1.1.2"
        )
    }
}

tasks {
    withType(Jar::class) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        exclude("META-INF/com.android.tools/**")
        exclude("META-INF/proguard/**")
        if (platform.isFabric) {
            exclude("mcmod.info", "META-INF/mods.toml")
        } else {
            exclude("fabric.mod.json")
            if (platform.isLegacyForge) {
                exclude("**/mods.toml")
                exclude("META-INF/versions/**")
                exclude("**/module-info.class")
                exclude("**/package-info.class")
            } else {
                exclude("mcmod.info")
            }
        }

        // Removes the TestMod entrypoint from the generated JARs.
        doLast {
            val fabricModJson = layout.buildDirectory.get().asFile.resolve("resources")
                .resolve("main")
                .resolve("fabric.mod.json")
            if (fabricModJson.exists()) {
                val lines = fabricModJson.readLines().toMutableList()
                lines.removeIf { it.contains("TestMod") }
                fabricModJson.writeText(lines.joinToString("\n"))
            }
        }
    }

    processResources {
        inputs.property("id", rootProject.properties["mod_id"].toString())
        inputs.property("name", rootProject.name)
        inputs.property("java", 8)
        inputs.property("version", version)
        inputs.property("mcVersionStr", if (platform.isFabric) platform.mcVersionStr.substringBeforeLast('.') + ".x" else platform.mcVersionStr)
        val id = inputs.properties["id"]
        val name = inputs.properties["name"]
        val version = inputs.properties["version"]
        val mcVersionStr = inputs.properties["mcVersionStr"].toString()
        val java = inputs.properties["java"].toString().toInt()
        val javaLevel = "JAVA-$java"

        filesMatching(listOf("mcmod.info", "mixins.${id}.json", "**/mods.toml", "fabric.mod.json")) {
            expand(
                mapOf(
                    "id" to id,
                    "name" to name,
                    "java" to java,
                    "java_level" to javaLevel,
                    "version" to version,
                    "mcVersionStr" to mcVersionStr
                )
            )
        }
    }

    remapJar {
        manifest {
            val attributesMap = buildMap<String, Any> {
                if (platform.isForge) {
                    if (platform.isLegacyForge) {
                        putAll(mapOf(
                            "ModSide" to "CLIENT",
                            "ForceLoadAsMod" to true,
                            "TweakOrder" to "0",
                            "MixinConfigs" to "mixins.$modId.json",
                            "TweakClass" to tweakClass
                        ))
                    } else {
                        put("MixinConfigs", "mixins.$modId.json")
                    }
                }
                putAll(mapOf(
                    "Specification-Title" to modId,
                    "Specification-Vendor" to "Polyfrost",
                    "Specification-Version" to "1", // We are version 1 of ourselves, whatever the hell that means
                    "Implementation-Title" to rootProject.name,
                    "Implementation-Version" to project.version,
                    "Implementation-Vendor" to "Polyfrost",
                    "Implementation-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(`java.util`.Date())
                ))
            }
            attributes(attributesMap)
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("java") {
            from(components["java"])

            groupId = group.toString()
            artifactId = base.archivesName.get()

            signing {
                isRequired = project.properties["signing.keyId"] != null
                sign(this@register)
            }
        }
    }
}