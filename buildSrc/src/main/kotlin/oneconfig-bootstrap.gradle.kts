import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.bundling.AbstractArchiveTask

plugins {
    java
    id("versioned-catalogues")
    id("me.modmuss50.mod-publish-plugin")
}

repositories {
    maven("https://repo.polyfrost.org/releases")
    maven("https://repo.polyfrost.org/snapshots")
    maven("https://repo.hypixel.net/repository/Hypixel/")
    maven("https://maven.deftu.dev/releases")
    maven("https://nexus.prsm.wtf/repository/maven-public/maven-repo/releases/")
    maven("https://maven.fabricmc.net/")
    maven("https://jitpack.io") {
        content { includeGroupAndSubgroups("com.github") }
    }
    maven("https://maven.bawnorton.com/releases") {
        content { includeGroup("com.github.bawnorton.mixinsquared") }
    }
    maven("https://maven.azureaaron.net/releases") {
        content { includeGroup("net.azureaaron") }
    }
    maven("https://redirector.kotlinlang.org/maven/compose-dev")
    google()
}

group = "${rootProject.group}.bootstrap"
version = rootProject.version

val node = project.name
val platformPath = ":minecraft:$node"

dependencies {
    "minecraft"("com.mojang:minecraft:${versionedCatalog.versions["minecraft"]}")
}

evaluationDependsOn(platformPath)

afterEvaluate {
    val platform = rootProject.project(platformPath)

    fun isExcluded(group: String?, name: String) =
        group == "net.fabricmc" && (name == "fabric-loader" || name == "intermediary")

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

    // Compose/skiko classes are shipped via the shaded :modules:compose-bundle jar.
    // The individual *-desktop artifacts are added non-transitively below, so on their
    // own they are missing the backing androidx.compose.* classes (e.g. SnapshotStateKt).
    fun isShadedInComposeBundle(group: String?): Boolean {
        group ?: return false
        return group.startsWith("org.jetbrains.compose") ||
            group == "org.jetbrains.skiko" ||
            group.startsWith("org.jetbrains.androidx")
    }

    fun includeExternal(dep: ExternalModuleDependency) {
        if (isExcluded(dep.group, dep.name)) return
        if (isShadedInComposeBundle(dep.group)) return
        val coord = "${dep.group}:${dep.name}:${dep.version}"
        if (!seen.add(coord)) return
        if (kotlinLibs.contains(coord.substringBeforeLast(":"))) return
        (dependencies.add("include", coord) as ExternalModuleDependency).isTransitive = false
    }

    // compose-bundle ships as its own Fabric mod on Modrinth, so it must NOT be JiJ'd
    // into the bootstrap. The platform still compiles against it (api dependency).
    val excludedProjects = setOf<String>()
        //setOf(":modules:compose-bundle") TODO re-add

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
        "mod_id" to (project.findProperty("mod.id") ?: "oneconfigbootstrap"),
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

val modrinthId = findProperty("publish.modrinth")?.toString()?.takeIf { it.isNotBlank() }
val modrinthToken = findProperty("modrinth.token")?.toString()?.takeIf { it.isNotBlank() }
val rawMinecraftVersion = versionedCatalog.versions["minecraft"].requiredVersion
val minecraftVersion = modrinthMinecraftVersionOverride[rawMinecraftVersion] ?: listOf(rawMinecraftVersion)
val publishJarTaskName = if ("remapJar" in tasks.names) "remapJar" else "jar"
val changelogs = rootProject.file("CHANGELOG.md").takeIf { it.exists() }?.readText() ?: "No changelog provided."

tasks.publishMods.get().doFirst {
    if (!changelogs.contains(project.version.toString())) {
        throw GradleException("Changelog for version ${project.version} not found.")
    }
}

publishMods {
    file = tasks.named<AbstractArchiveTask>(publishJarTaskName).flatMap { it.archiveFile }

    displayName = project.version.toString()
    version = "v${project.version}"
    changelog = changelogs
    type = BETA

    modLoaders.add("fabric")

    dryRun = modrinthId == null || modrinthToken == null

    if (modrinthId != null) {
        modrinth {
            projectId = modrinthId
            accessToken = modrinthToken.orEmpty()

            minecraftVersions.addAll(minecraftVersion)

            requires("fabric-language-kotlin")
            findProperty("publish.modrinth.compose-bundle")
                ?.toString()
                ?.takeIf { it.isNotBlank() }
                ?.let { requires(it) }
        }
    }
}
