import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import org.jetbrains.kotlin.gradle.plugin.extraProperties

private val stonecutter = project.extensions.getByName("stonecutter") as StonecutterBuildExtension

rootProject.versionCatalogs {
    val loader = stonecutter.current.project.substringAfterLast("-")
    val version = stonecutter.current.version.replace(".", "")
    entries[project] = ForwardingVersionCatalog(
        named("$loader$version"),
        named("common$version"),
        named(loader),
        named("libs")
    )
}
