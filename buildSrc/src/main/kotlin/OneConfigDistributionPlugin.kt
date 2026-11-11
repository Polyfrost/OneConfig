import me.modmuss50.mpp.ModPublishExtension
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

class OneConfigDistributionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("me.modmuss50.mod-publish-plugin")

        val isOrnithe = project.name.endsWith("-ornithe")
        val modJarTask = if ("remapJar" in tasks.names) "remapJar" else "jar"
        val modJarContents = zipTree(tasks.named<AbstractArchiveTask>(modJarTask).flatMap { it.archiveFile })

        val modJarManifestDir = layout.buildDirectory.dir("modJarManifest")
        val extractModJarManifest = tasks.register<Sync>("extractModJarManifest") {
            from(modJarContents) { include("META-INF/MANIFEST.MF") }
            into(modJarManifestDir)
        }

        // Loom's own include configuration nests into the jar published to Maven, where the
        // nested copies would clash with the same dependencies resolved by mods developing against it
        val bundleJar = tasks.register<Jar>("bundleJar") {
            group = "build"
            description = "Assembles the mod jar with the bundled modules and libraries nested inside."

            from(modJarContents)
            // Jar writes its own manifest in place of the copied one, so Loom's attributes are merged in
            dependsOn(extractModJarManifest)
            manifest.from(modJarManifestDir.get().file("META-INF/MANIFEST.MF").asFile)

            archiveBaseName = "OneConfig-${project.name}"
            destinationDirectory = layout.buildDirectory.dir("dist")
        }
        extensions.getByType<LoomGradleExtensionAPI>().nestJars(bundleJar, configurations.named("bundle"))
        tasks.named("assemble") { dependsOn(bundleJar) }

        val modrinthMinecraftVersionOverride = mapOf(
            "26.1" to listOf("26.1", "26.1.1", "26.1.2")
        )

        val modrinthId = listOf("oneconfig.publish.modrinth", "publish.modrinth").firstNotNullOfOrNull { findProperty(it) }?.toString()?.takeIf { it.isNotBlank() }
        val modrinthToken = listOf("oneconfig.publish.modrinth.token", "publish.modrinth.token", "modrinth.token").firstNotNullOfOrNull { findProperty(it) }?.toString()?.takeIf { it.isNotBlank() }
        val curseforgeId = listOf("oneconfig.publish.curseforge", "publish.curseforge").firstNotNullOfOrNull { findProperty(it) }?.toString()?.takeIf { it.isNotBlank() }
        val curseforgeToken = listOf("oneconfig.publish.curseforge.token", "publish.curseforge.token", "curseforge.token").firstNotNullOfOrNull { findProperty(it) }?.toString()?.takeIf { it.isNotBlank() }
        val rawMinecraftVersion = versionedCatalog.versions["minecraft"].requiredVersion
        val minecraftVersion = modrinthMinecraftVersionOverride[rawMinecraftVersion] ?: listOf(rawMinecraftVersion)
        val modVersion = project.version.toString()
        val releaseNotes = rootProject.file("CHANGELOG.md").takeIf { it.exists() }?.readText()?.let { releaseNotes(it, modVersion) }
        val changelogs = releaseNotes?.let { "## $modVersion\n\n$it" } ?: "No changelog provided."

        val validateChangelog = tasks.register("validateChangelog") {
            description = "Validates that the changelog is written for the current version."
            val hasNotes = releaseNotes != null
            doLast {
                if (!hasNotes) {
                    throw GradleException("Changelog for version $modVersion not found.")
                }
            }
        }

        tasks.named("publishMods") {
            dependsOn(validateChangelog)
        }
        tasks.matching { it.name == "publishModrinth" || it.name == "publishCurseforge" }.configureEach {
            dependsOn(validateChangelog)
        }

        configure<ModPublishExtension> {
            file = bundleJar.flatMap { it.archiveFile }

            displayName = project.version.toString()
            version = "v${project.version}"
            changelog = changelogs
            type = STABLE

            modLoaders.add(if (isOrnithe) "ornithe" else "fabric")

            dryRun = (modrinthId == null || modrinthToken == null) && (curseforgeId == null || curseforgeToken == null)

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

            if (curseforgeId != null && !isOrnithe) {
                curseforge {
                    projectId = curseforgeId
                    accessToken = curseforgeToken.orEmpty()

                    minecraftVersions.addAll(minecraftVersion)
                    clientRequired = true

                    requires("fabric-api")
                    requires("fabric-language-kotlin")
                    findProperty("publish.curseforge.compose-bundle.slug")
                        ?.toString()
                        ?.takeIf { it.isNotBlank() }
                        ?.let { requires(it) }
                }
            }
        }

        if (curseforgeId != null && isOrnithe) tasks.register("publishCurseforge")
    }
}

private fun releaseNotes(changelog: String, version: String): String? {
    val heading = Regex("^## \\[${Regex.escape(version)}].*$",RegexOption.MULTILINE).find(changelog) ?: return null
    val rest = changelog.substring(heading.range.last + 1)
    val end = Regex("^## ", RegexOption.MULTILINE).find(rest)?.range?.first ?: rest.length
    return rest.substring(0, end).trim().takeIf { it.isNotEmpty() }
}
