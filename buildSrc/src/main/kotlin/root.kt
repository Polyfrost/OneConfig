import dev.kikugie.stonecutter.data.StonecutterProject
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependencyBundle
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.VersionConstraint
import org.gradle.api.attributes.AttributeDisambiguationRule
import org.gradle.api.attributes.MultipleCandidatesDetails
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugin.use.PluginDependency
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.inject.Inject

data class ForwardingVersionCatalog(
    val catalogs: List<VersionCatalog>
) {
    constructor(vararg catalogs: VersionCatalog) : this(listOf(*catalogs))

    private fun <T> first(name: String, lookup: VersionCatalog.(String) -> Optional<T>): T = try {
        catalogs.firstNotNullOf { it.lookup(name).orElse(null) }
    } catch (e: Exception) {
        throw RuntimeException("Failed to find '$name' in <" +catalogs.joinToString(", ") { it.name } + ">", e)
    }

    val libraries: ForwardingProperty<Provider<MinimalExternalModuleDependency>> = ForwardingProperty(this, VersionCatalog::findLibrary)
    val bundles: ForwardingProperty<Provider<ExternalModuleDependencyBundle>> = ForwardingProperty(this, VersionCatalog::findBundle)
    val plugins: ForwardingProperty<Provider<PluginDependency>> = ForwardingProperty(this, VersionCatalog::findPlugin)
    val versions: ForwardingProperty<VersionConstraint> = ForwardingProperty(this, VersionCatalog::findVersion)

    fun library(name: String): Provider<MinimalExternalModuleDependency> = first(name, VersionCatalog::findLibrary)
    fun bundle(name: String): Provider<ExternalModuleDependencyBundle> = first(name, VersionCatalog::findBundle)
    fun plugin(name: String): Provider<PluginDependency> = first(name, VersionCatalog::findPlugin)
    fun version(name: String): VersionConstraint = first(name, VersionCatalog::findVersion)

    fun has(name: String): Boolean = runCatching { get(name) }.map { true }.getOrDefault(false)
    operator fun get(name: String): Provider<MinimalExternalModuleDependency> = library(name)

    data class ForwardingProperty<T>(
        val parent: ForwardingVersionCatalog,
        val lookup: VersionCatalog.(String) -> Optional<T>
    ) {
        fun has(name: String): Boolean = runCatching { get(name) }.map { true }.getOrDefault(false)
        operator fun get(name: String): T = parent.first(name, lookup)
        fun getOrNull(name: String): T? = runCatching { parent.first(name, lookup) }.getOrNull()
        fun getOrFallback(
            name: String,
            fallbackName: String
        ) = runCatching { this[name] }.getOrElse { this[fallbackName] }
    }
}

fun Project.getForwardingVersionCatalog(project: StonecutterProject): ForwardingVersionCatalog {
    val loader = project.project.substringAfterLast("-")
    val version = project.version.replace(".", "")
    val verisonCatalog = rootProject.extensions.getByType<VersionCatalogsExtension>()
    return ForwardingVersionCatalog(
        verisonCatalog.named("$loader$version"),
        verisonCatalog.named("common$version"),
        verisonCatalog.named(loader),
        verisonCatalog.named("libs")
    )
}


internal val entries: MutableMap<Project, ForwardingVersionCatalog> = mutableMapOf()
val Project.versionedCatalog get() = entries[this] ?: ForwardingVersionCatalog()

/**
 * Copy a Fabric mod jar, dropping the `minecraft` entry from its `depends` block.
 *
 * Used for third-party mods that have a hard dependency on an older game version
 * even though they work fine on a newer version.
 */
fun relaxGameConstraint(source: File, target: File) {
    var relaxed = false
    rewriteZip(source, target) { name, bytes ->
        if (name != "fabric.mod.json") return@rewriteZip bytes
        @Suppress("UNCHECKED_CAST")
        val json = JsonSlurper().parse(bytes) as MutableMap<String, Any?>
        @Suppress("UNCHECKED_CAST")
        relaxed = (json["depends"] as? MutableMap<String, Any?>)?.remove("minecraft") != null
        JsonOutput.toJson(json).toByteArray()
    }
    if (!relaxed) throw GradleException("${source.name} declares no minecraft dependency to relax")
}

/**
 * Rewrite the nested `META-INF/jars/<modId>-*.jar` inside a mod jar with [relaxGameConstraint] applied.
 */
fun relaxNestedJar(jar: File, modId: String) {
    val temp = File(jar.parentFile, "${jar.name}.relaxing")
    val nestedIn = File(temp.parentFile, "$modId-in.jar")
    val nestedOut = File(temp.parentFile, "$modId-out.jar")
    var found = false
    rewriteZip(jar, temp) { name, bytes ->
        if (!name.startsWith("META-INF/jars/$modId-") || !name.endsWith(".jar")) return@rewriteZip bytes
        nestedIn.writeBytes(bytes)
        relaxGameConstraint(nestedIn, nestedOut)
        found = true
        nestedOut.readBytes()
    }
    nestedIn.delete()
    nestedOut.delete()
    if (!found) {
        temp.delete()
        throw GradleException("${jar.name} nests no $modId jar to relax")
    }
    Files.move(temp.toPath(), jar.toPath(), StandardCopyOption.REPLACE_EXISTING)
}

private fun rewriteZip(source: File, target: File, transform: (String, ByteArray) -> ByteArray) {
    target.parentFile.mkdirs()
    ZipFile(source).use { zip ->
        ZipOutputStream(target.outputStream().buffered()).use { out ->
            for (entry in zip.entries()) {
                val bytes = transform(entry.name, zip.getInputStream(entry).readBytes())
                val copy = ZipEntry(entry.name)
                if (entry.method == ZipEntry.STORED) {
                    copy.method = ZipEntry.STORED
                    copy.size = bytes.size.toLong()
                    copy.crc = CRC32().apply { update(bytes) }.value
                }
                out.putNextEntry(copy)
                out.write(bytes)
                out.closeEntry()
            }
        }
    }
}
