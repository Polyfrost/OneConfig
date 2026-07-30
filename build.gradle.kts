
allprojects {
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/releases")
        maven("https://maven.parchmentmc.org")
    }
}

// Every artifact OneConfig actually ships: one bootstrap jar per Minecraft/loader node
// (each already JiJ's the matching platform jar plus all modules and deps), and the
// standalone compose-bundle mod, which is deliberately kept out of the bootstrap JiJ.
val bootstrapNodes = subprojects.filter { it.parent?.path == ":bootstrap" }
val composeBundle = project(":modules:compose-bundle")

// compose-bundle carries its own version, so evaluate it to grab the jar task directly
// instead of guessing its file name.
evaluationDependsOn(composeBundle.path)

// Sync, not Copy: node build/libs directories keep the jars of previously built versions,
// so the collected directory must only ever hold what this run produced.
tasks.register<Sync>("buildAndCollect") {
    group = "build"
    description = "Builds every OneConfig bootstrap node plus compose-bundle and collects the production jars into build/libs."

    dependsOn(":bootstrap:assembleAllNodes")

    // Loom keeps the unmapped jar in build/devlibs, so build/libs holds only the shippable
    // artifact — filtered by version to skip stale jars from earlier project versions.
    from(bootstrapNodes.map { it.layout.buildDirectory.dir("libs") }) {
        include("*-${rootProject.version}.jar")
    }
    from(composeBundle.tasks.named<Jar>("jar").flatMap { it.archiveFile })
    into(layout.buildDirectory.dir("libs"))

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    doFirst {
        if (bootstrapNodes.isEmpty()) {
            throw GradleException("No bootstrap nodes were registered — check the stonecutter tree in settings.gradle.kts.")
        }
    }

    doLast {
        val target = destinationDir
        logger.lifecycle("Collected ${target.listFiles { f -> f.extension == "jar" }?.size ?: 0} jars into $target")
    }
}

/*
plugins {
    alias(libs.plugins.kotlin) apply(false)
    alias(libs.plugins.licenser) apply(false)
    alias(libs.plugins.jetbrains.idea.ext)

    alias(libs.plugins.dgt.base) apply(false)
}

// Note for future devs: DON'T apply the java-library plugin to subprojects here.
// This will cause loom to completely break apart and throw itself into oblivion.
// I have no idea how to fix it, and honestly, I don't want to know.



subprojects {
    pluginManager.withPlugin("java") {
        apply(plugin = rootProject.libs.plugins.licenser.get().pluginId)
        apply(plugin = rootProject.libs.plugins.dgt.base.get().pluginId)
        apply(plugin = rootProject.libs.plugins.dgt.publishing.maven.get().pluginId)
        apply(plugin = "signing")

        configure<PublishingExtension> {
            repositories {
                arrayOf("releases", "snapshots", "private").forEach { type ->
                    maven {
                        name = type
                        url = uri("https://repo.polyfrost.org/$type")
                        credentials(PasswordCredentials::class)
                        authentication { create<BasicAuthentication>("basic") }
                    }
                }
            }
        }
    }
}

 */
