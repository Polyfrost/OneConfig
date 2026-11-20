import org.gradle.api.Project
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.registerTransform
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.SimpleRemapper
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

val skikoRemapped: Attribute<Boolean> = Attribute.of("org.polyfrost.compose.skiko-remapped", Boolean::class.javaObjectType)

val skikoRemapping = mapOf(
    "androidx/compose/ui/graphics/SkiaGraphicsContext" to "org/polyfrost/compose/skiko/SkiaGraphicsContext",
    "androidx/compose/ui/text/platform/FontLoader" to "org/polyfrost/compose/skiko/FontLoader",
)

fun Project.useSkikoRemappedRuntime() = pluginManager.withPlugin("java") {
    dependencies {
        attributesSchema { attribute(skikoRemapped) }
        artifactTypes.maybeCreate(ArtifactTypeDefinition.JAR_TYPE).attributes.attribute(skikoRemapped, false)
        registerTransform(RemapSkikoReferences::class) {
            from.attribute(skikoRemapped, false).attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE)
            to.attribute(skikoRemapped, true).attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE)
            parameters { mapping.set(skikoRemapping) }
        }
    }

    configurations.matching { it.name == "runtimeClasspath" || it.name.endsWith("RuntimeClasspath") }.configureEach {
        attributes.attribute(skikoRemapped, true)
    }
}

abstract class RemapSkikoReferences : TransformAction<RemapSkikoReferences.Parameters> {
    interface Parameters : TransformParameters {
        @get:Input
        val mapping: MapProperty<String, String>
    }

    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val input: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val file = input.get().asFile
        val mapping = parameters.mapping.get()
        val needles = mapping.keys.flatMap { listOf(it, it.replace('/', '.')) }.map { it.toByteArray() }
        val rewritten = HashMap<String, ByteArray>()
        ZipFile(file).use { zip ->
            for (entry in zip.entries()) {
                if (!entry.name.endsWith(".class")) continue
                val bytes = zip.getInputStream(entry).use { it.readBytes() }
                if (needles.none { bytes.contains(it) }) continue
                val remapped = remap(bytes, mapping)
                val left = needles.firstOrNull { remapped.contains(it) }
                check(left == null) {
                    "${file.name}!/${entry.name} still references ${String(left!!)} after the Skiko remap. " +
                        "It is probably a string constant or reflection the remapper cannot rewrite."
                }
                rewritten[entry.name] = remapped
            }
        }
        if (rewritten.isEmpty()) {
            outputs.file(input)
            return
        }
        val output = outputs.file(file.name)
        ZipFile(file).use { zip ->
            ZipOutputStream(output.outputStream().buffered()).use { out ->
                for (entry in zip.entries()) {
                    val copy = ZipEntry(entry.name)
                    copy.time = entry.time
                    out.putNextEntry(copy)
                    val bytes = rewritten[entry.name]
                    if (bytes != null) out.write(bytes) else zip.getInputStream(entry).use { it.copyTo(out) }
                    out.closeEntry()
                }
            }
        }
    }

    private fun remap(bytes: ByteArray, mapping: Map<String, String>): ByteArray {
        val reader = ClassReader(bytes)
        val writer = ClassWriter(0)
        reader.accept(KotlinMetadataRemapper(ClassRemapper(writer, SimpleRemapper(Opcodes.ASM9, mapping)), mapping), 0)
        return writer.toByteArray()
    }

    private fun ByteArray.contains(needle: ByteArray): Boolean {
        val last = size - needle.size
        outer@ for (i in 0..last) {
            for (j in needle.indices) if (this[i + j] != needle[j]) continue@outer
            return true
        }
        return false
    }

    private class KotlinMetadataRemapper(next: ClassVisitor, private val mapping: Map<String, String>) : ClassVisitor(Opcodes.ASM9, next) {
        override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
            val visitor = super.visitAnnotation(descriptor, visible)
            if (descriptor != "Lkotlin/Metadata;" || visitor == null) return visitor
            return object : AnnotationVisitor(Opcodes.ASM9, visitor) {
                override fun visitArray(name: String?): AnnotationVisitor? {
                    val array = super.visitArray(name)
                    if (name != "d2" || array == null) return array
                    return object : AnnotationVisitor(Opcodes.ASM9, array) {
                        override fun visit(name: String?, value: Any?) {
                            super.visit(name, if (value is String) remapString(value) else value)
                        }
                    }
                }
            }
        }

        private fun remapString(value: String): String {
            mapping[value]?.let { return it }
            var result = value
            for ((from, to) in mapping) result = result.replace("L$from;", "L$to;")
            return result
        }
    }
}
