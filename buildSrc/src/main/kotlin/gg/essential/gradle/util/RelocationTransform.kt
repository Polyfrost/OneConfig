/*
 * This file is part of essential-gradle-toolkit, licensed under the GPL-3.0.
 *
 * Copyright (C) 2025 EssentialGG and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package gg.essential.gradle.util

import gg.essential.gradle.util.relocate.KotlinMetadataRemappingClassVisitor
import org.gradle.api.Project
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import java.io.Closeable
import java.io.File
import java.io.Serializable
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 * Relocates packages and single files in an artifact
 *
 * If a package is relocated the folder containing it will be relocated as a whole
 * File renames take priority over package relocations
 *
 * The packages do not have to be part of the artifact
 * For example a completely valid use case would be relocating guava packages in an artifact using guava
 * For actual use you would of course also have to relocate the guava artifact itself
 * otherwise the classes referred to after the relocation will not exist
 * This can be used together with [prebundle] to create fat jars which apply at dev time
 * for example to use two different versions of the same library
 *
 * To simplify setup use [registerRelocationAttribute]
 */
abstract class RelocationTransform : TransformAction<RelocationTransform.Parameters> {
    interface Parameters : TransformParameters {
        @get:Input
        val relocations: SetProperty<Relocation>

        @get:Input
        val remapStringsIn: SetProperty<String>

        @get:Input
        val renames: SetProperty<Rename>

        fun relocate(sourcePackage: String, targetPackage: String) =
            relocations.add(Relocation(sourcePackage, targetPackage))

        fun remapStringsIn(cls: String) =
            remapStringsIn.add(cls)

        fun rename(sourceFile: String, targetFile: String) =
            renames.add(Rename(sourceFile, targetFile))
    }

    data class Relocation(val sourcePackage: String, val targetPackage: String) : Serializable
    data class Rename(val sourceFile: String, val targetFile: String) : Serializable

    @get:InputArtifact
    abstract val input: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val fileMap = parameters.renames.get().associate { it.sourceFile to it.targetFile }

        val jvmPackageMap = parameters.relocations.get().associate {
            it.sourcePackage.replace('.', '/') + '/' to it.targetPackage.replace('.', '/') + '/'
        }
        val javaPackageMap = jvmPackageMap.map { (source, target) ->
            source.replace('/', '.') to target.replace('/', '.')
        }.toMap()
        val absoluteFolderMap = jvmPackageMap.map { (source, target) ->
            "/$source" to "/$target"
        }.toMap()

        val remapStringsInFiles = parameters.remapStringsIn.get().map {
            it.replace('.', '/') + ".class"
        }

        open class ClassPrefixRemapper : Remapper() {
            override fun map(typeName: String): String = map(jvmPackageMap, typeName)

            protected fun map(mappings: Map<String, String>, typeName: String): String {
                for ((sourcePackage, targetPackage) in mappings) {
                    if (typeName.startsWith(sourcePackage)) {
                        return targetPackage + typeName.substring(sourcePackage.length)
                    }
                }
                return typeName
            }
        }
        val baseRemapper = ClassPrefixRemapper()

        class ClassPrefixAndStringsRemapper : ClassPrefixRemapper() {
            override fun mapValue(value: Any?): Any {
                if (value is String) {
                    return map(absoluteFolderMap, map(jvmPackageMap, map(javaPackageMap, value)))
                }
                return super.mapValue(value)
            }
        }
        val stringRemapper = ClassPrefixAndStringsRemapper()

        val input = input.get().asFile
        val output = outputs.file(input.nameWithoutExtension + "-relocated.jar")
        (input to output).useInOut { jarIn, jarOut ->
            while (true) {
                val entry = jarIn.nextJarEntry ?: break
                val originalBytes = jarIn.readBytes()
                val remapper = if (entry.name in remapStringsInFiles) stringRemapper else baseRemapper

                val modifiedBytes = if (entry.name.endsWith(".class")) {
                    val reader = ClassReader(originalBytes)
                    // Not copying the constant pool cause that leaves references to the old classes
                    // Any lazy tool will never end up resolving them but proguard does resolve them
                    val writer = ClassWriter(0)
                    reader.accept(ClassRemapper(KotlinMetadataRemappingClassVisitor(remapper, writer), remapper), 0)
                    writer.toByteArray()
                } else if (entry.name.startsWith("META-INF/services/")) {
                    String(originalBytes).replace('.', '/').lines().map(remapper::map).joinToString("\n").replace('/','.').encodeToByteArray()
                } else {
                    originalBytes
                }

                jarOut.putNextEntry(ZipEntry(fileMap[entry.name] ?: remapper.map(entry.name)))
                jarOut.write(modifiedBytes)
                jarOut.closeEntry()
            }
        }
    }

    private inline fun Pair<File, File>.useInOut(block: (jarIn: JarInputStream, jarOut: JarOutputStream) -> Unit) =
        first.inputStream().nestedUse(::JarInputStream) { jarIn ->
            second.outputStream().nestedUse(::JarOutputStream) { jarOut ->
                block(jarIn, jarOut)
            }
        }

    private inline fun <T: Closeable, U: Closeable> T.nestedUse(nest: (T) -> U, block: (U) -> Unit) =
        use { nest(it).use(block) }

    companion object {
        fun Project.registerRelocationAttribute(name: String, configure: Parameters.() -> Unit): Attribute<Boolean> {
            val attribute = Attribute.of(name, Boolean::class.javaObjectType)

            dependencies.registerTransform(RelocationTransform::class.java) {
                from.attribute(attribute, false)
                to.attribute(attribute, true)
                parameters(configure)
            }

            dependencies.artifactTypes.all {
                attributes.attribute(attribute, false)
            }

            return attribute
        }
    }
}