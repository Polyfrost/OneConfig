import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import kotlin.collections.listOf

plugins {
    java
    id("versioned-catalogues")
    id("me.modmuss50.mod-publish-plugin")
}

repositories {
    maven("https://repo.polyfrost.org/releases")
    maven("https://repo.polyfrost.org/snapshots")
    maven("https://repo.hypixel.net/repository/Hypixel/") {
        content { includeGroupAndSubgroups("net.hypixel") }
    }
    maven("https://maven.deftu.dev/releases") {
        content { includeGroupAndSubgroups("dev.deftu") }
    }
    maven("https://nexus.prsm.wtf/repository/maven-public/maven-repo/releases/")
    maven("https://api.modrinth.com/maven") {
        content { includeGroup("maven.modrinth") }
    }
    maven("https://maven.fabricmc.net/") {
        content { includeGroupAndSubgroups("net.fabricmc") }
    }
    maven("https://jitpack.io") {
        content { includeGroupAndSubgroups("com.github") }
    }
    maven("https://maven.bawnorton.com/releases") {
        content { includeGroup("com.github.bawnorton.mixinsquared") }
    }
    maven("https://maven.azureaaron.net/releases") {
        content { includeGroup("net.azureaaron") }
    }
    maven("https://redirector.kotlinlang.org/maven/compose-dev") {
        content { includeGroupAndSubgroups("org.jetbrains") }
    }
    maven("https://central.sonatype.com/repository/maven-snapshots/")
    google()
}

group = "${rootProject.group}.bootstrap"
version = rootProject.version

val node = project.name
val isOrnithe = node.endsWith("-ornithe")
val platformPath = ":minecraft:$node"

dependencies {
    "minecraft"("com.mojang:minecraft:${versionedCatalog.versions["minecraft"]}")
}

gradle.projectsEvaluated {
    val platform = rootProject.project(platformPath)

    fun isExcluded(group: String?, name: String) =
        (group == "net.fabricmc" && (name == "fabric-loader")) || group == "net.fabricmc.fabric-api" ||
            (isOrnithe && ((group == "pl.tomgirl" && name == "lenis") || group == "net.ornithemc.osl-gen2"))

    val seen = HashSet<String>()

    val kotlinLibs = listOf(
        "org.jetbrains.kotlin:kotlin-stdlib",
        "org.jetbrains.kotlin:kotlin-stdlib-jdk8",
        "org.jetbrains.kotlin:kotlin-stdlib-jdk7",
        "org.jetbrains.kotlin:kotlin-reflect",

        "org.jetbrains.kotlinx:kotlinx-coroutines-core",
        "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm",
        "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8",
        "org.jetbrains.kotlinx:kotlinx-serialization-core-jvm",
        "org.jetbrains.kotlinx:kotlinx-serialization-json-jvm",
        "org.jetbrains.kotlinx:kotlinx-serialization-cbor-jvm",
        "org.jetbrains.kotlinx:atomicfu-jvm",
        "org.jetbrains.kotlinx:kotlinx-datetime-jvm",
        "org.jetbrains.kotlinx:kotlinx-io-core-jvm",
        "org.jetbrains.kotlinx:kotlinx-io-bytestring-jvm",

        "net.fabricmc:fabric-language-kotlin"
    )

    // compose/skiko classes ship inside the shaded :modules:compose-bundle jar
    // the *-desktop artifacts are added non-transitively so alone they lack androidx.compose.* classes
    fun isShadedInComposeBundle(group: String?): Boolean {
        group ?: return false
        return group.startsWith("org.jetbrains.compose") ||
            group == "org.jetbrains.skiko" ||
            group.startsWith("org.jetbrains.androidx")
    }

    fun isHypixelProvidedElsewhere(group: String?, name: String) =
        (group == "net.hypixel" && name == "mod-api") ||
            (group == "maven.modrinth" && name == "hypixel-mod-api")

    fun includeExternal(dep: ExternalModuleDependency) {
        if (isExcluded(dep.group, dep.name)) return
        if (isHypixelProvidedElsewhere(dep.group, dep.name)) return
        if (isShadedInComposeBundle(dep.group)) return
        val coord = "${dep.group}:${dep.name}:${dep.version}"
        if (!seen.add(coord)) return
        if (kotlinLibs.contains(coord.substringBeforeLast(":"))) return
        (dependencies.add("include", coord) as ExternalModuleDependency).isTransitive = false
    }

    // compose-bundle ships as its own Fabric mod on Modrinth so it must not be JiJ'd
    // into the bootstrap while the platform still compiles against it
    val excludedProjects = setOf(":modules:compose-bundle")

    fun includeProject(path: String) {
        if (path in excludedProjects) return
        if (!seen.add(path)) return
        (dependencies.add("include", dependencies.project(path)) as ModuleDependency).isTransitive = false
    }

    listOf("api", "modApi", "modImplementation").forEach { name ->
        platform.configurations.findByName(name)?.dependencies?.forEach { dep ->
            when (dep) {
                is ProjectDependency -> includeProject(dep.path)
                is ExternalModuleDependency -> includeExternal(dep)
            }
        }
    }

    (dependencies.add("include", dependencies.project(mapOf("path" to platformPath))) as ModuleDependency)
        .isTransitive = false

    val hypixelFabricMod = if (versionedCatalog.versions["minecraft"].requiredVersion.startsWith("26")) {
        "maven.modrinth:hypixel-mod-api:1.0.2+build.1+mc26.1"
    } else if (versionedCatalog.versions["minecraft"].requiredVersion.startsWith("1.21")) {
        "maven.modrinth:hypixel-mod-api:1.0.1+build.1+mc1.21"
    } else {
        "org.polyfrost:mod-api-fabric:1.0.2+build.1+mc1.8.9"
    }
    (dependencies.add("include", hypixelFabricMod) as ExternalModuleDependency).isTransitive = false
}

base.archivesName.set("OneConfig-${project.name}")

tasks.withType<ProcessResources>().configureEach {
    val range = if (versionedCatalog.versions.has("minecraft.range")) {
        versionedCatalog.versions.get("minecraft.range").toString()
    } else {
        val start = versionedCatalog.versions.getOrFallback("minecraft.start", "minecraft")
        val end = versionedCatalog.versions.getOrFallback("minecraft.end", "minecraft")
        ">=$start <=$end"
    }

    val props = mapOf(
        "mod_id" to (project.findProperty("mod.id") ?: "oneconfig"),
        "mod_name" to (project.findProperty("mod.name") ?: "OneConfig"),
        "mod_version" to project.version,
        "mod_description" to (project.findProperty("mod.description") ?: "OneConfig bootstrap loader."),
        "mc_version" to range,
    )

    inputs.properties(props)
    filesMatching("fabric.mod.json") { expand(props) }
}

val modrinthMinecraftVersionOverride = mapOf(
    "26.1" to listOf("26.1", "26.1.1", "26.1.2")
)

val modrinthId = listOf("oneconfig.publish.modrinth", "publish.modrinth").firstNotNullOfOrNull { findProperty(it) }?.toString()?.takeIf { it.isNotBlank() }
val modrinthToken = listOf("oneconfig.publish.modrinth.token", "publish.modrinth.token", "modrinth.token").firstNotNullOfOrNull { findProperty(it) }?.toString()?.takeIf { it.isNotBlank() }
val rawMinecraftVersion = versionedCatalog.versions["minecraft"].requiredVersion
val minecraftVersion = modrinthMinecraftVersionOverride[rawMinecraftVersion] ?: listOf(rawMinecraftVersion)
val publishJarTaskName = if ("remapJar" in tasks.names) "remapJar" else "jar"
val changelogs = rootProject.file("CHANGELOG.md").takeIf { it.exists() }?.readText() ?: "No changelog provided."

val validateChangelog by tasks.registering {
    description = "Validates that the changelog is written for the current version."
    if (!changelogs.contains(project.version.toString())) {
        throw GradleException("Changelog for version ${project.version} not found.")
    }
}

tasks.publishMods.configure {
    dependsOn(validateChangelog)
}
tasks.matching { it.name == "publishModrinth" }.configureEach {
    dependsOn(validateChangelog)
}

if (!isOrnithe) {
    tasks.publishMods.configure {
        enabled = false
    }
    tasks.matching { it.name == "publishModrinth" }.configureEach {
        enabled = false
    }
}

publishMods {
    file = tasks.named<AbstractArchiveTask>(publishJarTaskName).flatMap { it.archiveFile }

    displayName = project.version.toString()
    version = "v${project.version}"
    changelog = changelogs
    type = STABLE

    modLoaders.add(if (isOrnithe) "ornithe" else "fabric")

    dryRun = modrinthId == null || modrinthToken == null

    if (modrinthId != null) {
        modrinth {
            projectId = modrinthId
            accessToken = modrinthToken.orEmpty()

            minecraftVersions.addAll(minecraftVersion)

            if (isOrnithe) {
                requires("osl")
            } else {
                requires("fabric-api")
            }
            requires("fabric-language-kotlin")
            findProperty("publish.modrinth.compose-bundle")
                ?.toString()
                ?.takeIf { it.isNotBlank() }
                ?.let { requires(it) }
        }
    }
}
