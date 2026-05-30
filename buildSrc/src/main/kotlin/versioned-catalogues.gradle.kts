import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import org.jetbrains.kotlin.gradle.plugin.extraProperties

private val stonecutter = project.extensions.getByName("stonecutter") as StonecutterBuildExtension

entries[project] = project.getForwardingVersionCatalog(stonecutter.current)
