// compose-bundle ships as a standalone Fabric mod (see src/main/resources/fabric.mod.json)
// so the shaded Compose/skiko runtime can be published to Modrinth separately. It is
// therefore excluded from the bootstrap JiJ (see oneconfig-bootstrap.gradle.kts).
group = "${rootProject.group}.compose"
version = "1.0.1+compose.${libs.versions.compose.asProvider().get()}-skiko.${libs.versions.skiko.get()}"

repositories {
    maven("https://redirector.kotlinlang.org/maven/compose-dev")
}

val shade: Configuration by configurations.creating {
    exclude(group = "org.jetbrains.kotlin")
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

tasks.jar {
    from(shade.map { file ->
        when {
            isExcludedFromBundle(file) -> files()
            file.isDirectory -> fileTree(file)
            else -> zipTree(file)
        }
    })
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
