
allprojects {
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/releases")
        maven("https://maven.ornithemc.net/releases")
    }
}

val minecraftNodes = subprojects.filter { it.parent?.path == ":minecraft" }

subprojects { useSkikoRemappedRuntime() }

// Sync rather than Copy because node build/dist keeps jars of previously built versions
tasks.register<Sync>("buildAndCollect") {
    group = "build"
    description = "Builds every OneConfig Minecraft node and collects the production jars into build/libs."

    // task paths rather than task providers so the nodes aren't all configured with the root project
    dependsOn(minecraftNodes.map { "${it.path}:bundleJar" })

    // version filter skips stale jars from earlier project versions
    from(minecraftNodes.map { it.layout.buildDirectory.dir("dist") }) {
        include("*-${rootProject.version}.jar")
    }
    into(layout.buildDirectory.dir("libs"))

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    val noMinecraftNodes = minecraftNodes.isEmpty()
    doFirst {
        if (noMinecraftNodes) {
            throw GradleException("No Minecraft nodes were registered — check the stonecutter tree in settings.gradle.kts.")
        }
    }

    doLast {
        val target = destinationDir
        logger.lifecycle("Collected ${target.listFiles { f -> f.extension == "jar" }?.size ?: 0} jars into $target")
    }
}
