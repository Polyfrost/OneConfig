
allprojects {
    repositories {
        // First, so a `publishToMavenLocal` run satisfies OneConfig's own
        // artifacts without reaching repo.polyfrost.org. See startlocal/mod.ps1.
        mavenLocal()

        mavenCentral()
        maven("https://maven.fabricmc.net/releases")
    }
}

// each bootstrap node JiJ's its platform jar plus all modules and deps
// compose-bundle ships standalone and is deliberately kept out of that JiJ
val bootstrapNodes = subprojects.filter { it.parent?.path == ":bootstrap" }
val composeBundle = project(":modules:compose-bundle")

// compose-bundle carries its own version so evaluate it to grab the jar task
// rather than guessing the file name
evaluationDependsOn(composeBundle.path)

// Sync rather than Copy because node build/libs keeps jars of previously built versions
tasks.register<Sync>("buildAndCollect") {
    group = "build"
    description = "Builds every OneConfig bootstrap node plus compose-bundle and collects the production jars into build/libs."

    dependsOn(":bootstrap:assembleAllNodes")

    // loom keeps the unmapped jar in build/devlibs so build/libs holds only the shippable one
    // version filter skips stale jars from earlier project versions
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
