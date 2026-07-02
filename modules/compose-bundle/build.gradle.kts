import net.fabricmc.loom.build.nesting.NestableJarGenerationTask
import net.fabricmc.loom.task.NestJarsAction

// compose-bundle ships as a standalone Fabric mod (see src/main/resources/fabric.mod.json)
// so the shaded Compose/skiko runtime can be published to Modrinth separately. It is
// therefore excluded from the bootstrap JiJ (see oneconfig-bootstrap.gradle.kts).
group = "${rootProject.group}.compose"
version = "1.0.2+compose.${libs.versions.compose.asProvider().get()}"

repositories {
    maven("https://redirector.kotlinlang.org/maven/compose-dev")
}

val shade: Configuration by configurations.creating {
    isTransitive = true
    exclude(group = "org.jetbrains.kotlin")
    exclude(group = "org.jetbrains.androidx.lifecycle")
    exclude(group = "org.jetbrains.kotlinx")
    exclude(group = "org.jetbrains", module = "annotations")
}

fun isExcludedFromBundle(file: File): Boolean {
    val artifact = shade.resolvedConfiguration.resolvedArtifacts.find { it.file == file }
    return artifact?.moduleVersion?.id?.let { id ->
        id.group == "org.jetbrains.kotlin" ||
            id.group == "org.jetbrains.kotlinx" ||
            (id.group == "org.jetbrains" && id.name == "annotations")
    } ?: false
}

dependencies {
    shade(libs.jetbrains.compose.foundation)
    shade(libs.jetbrains.compose.material)
    shade(libs.jetbrains.compose.runtime)
    shade(libs.jetbrains.compose.ui)
    shade(libs.jetbrains.compose.ui.tooling.preview)
    shade(libs.jetbrains.compose.ui.util)
    shade(libs.jetbrains.skiko.awt)
    shade(libs.jetbrains.skiko.awt.runtime.windows.x64)
    shade(libs.jetbrains.skiko.awt.runtime.linux.x64)
    shade(libs.jetbrains.skiko.awt.runtime.macos.x64)
    shade(libs.jetbrains.skiko.awt.runtime.macos.arm64)
    shade(libs.jetbrains.compose.navigation)
    shade(libs.jetbrains.lifecycle)
    shade(libs.jetbrains.viewmodel)
}

private fun createProcessTask(): TaskProvider<NestableJarGenerationTask> {

    return project.tasks.register("processShadeJars", NestableJarGenerationTask::class.java) {
        description = ""
        from(shade)
        outputDirectory.set(project.layout.buildDirectory.dir(name))
        uncompressNestedJars.set(false)
    }
}

val processTask = createProcessTask()

private fun getOutputJars(): FileCollection {
    return project.fileTree(processTask.flatMap(Transformer { it.outputDirectory })).matching { include("*.jar") }
}

tasks.jar {
    dependsOn(processTask)

    NestJarsAction.addToTask(this, getOutputJars())
}

tasks.jar {
    exclude(
        "kotlin/**",
        "kotlinx/**",
        "META-INF/kotlin-stdlib*.kotlin_module",
        "META-INF/maven/org.jetbrains.kotlin/**",
        "META-INF/maven/org.jetbrains/annotations/**",
        "org/jetbrains/annotations/**",
    )
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("fabric.mod.json") { expand(props) }
}

apply(plugin = "oneconfig-compose-bundle-publish")
