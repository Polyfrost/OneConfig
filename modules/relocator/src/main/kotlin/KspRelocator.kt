package org.polyfrost.oneconfig.relocator

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSDeclaration
import me.owdding.kotlinpoet.*
import me.owdding.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import me.owdding.kotlinpoet.ksp.writeTo
import org.polyfrost.oneconfig.relocator.SourceFileHelper.replacePatterns
import org.polyfrost.oneconfig.relocator.annotations.RelocatedMixin
import java.io.OutputStream
import javax.annotation.Generated
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.readText

internal const val mainMixinPath = "org.polyfrost.oneconfig.internal.mixin"


/**
 * The relocator for kotlin source files.
 */
internal class KspRelocator : SymbolProcessorProvider, SymbolProcessor {

    var hasRun = false
    lateinit var logger: KSPLogger
    lateinit var environment: SymbolProcessorEnvironment
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        this.environment = environment
        logger = environment.logger
        return this
    }

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (hasRun) return emptyList()
        hasRun = true
        val mixinPaths = mutableListOf<String>()
        Locations.relocations.forEach { (annotation, relocations) ->
            val symbols = resolver.getSymbolsWithAnnotation(annotation.qualifiedName!!)
            val files = mutableMapOf<String, MutableList<Relocation>>()
            symbols.forEach { symbol ->
                val relocateMixin = symbol.isAnnotationPresent(RelocatedMixin::class)

                val symbol = symbol as? KSDeclaration ?: return@forEach
                val location = Path((symbol.location as FileLocation).filePath)
                val content = location.readText()
                val sourcePackage = relocations.sourcePackage

                val originName = symbol.simpleName.asString()
                relocations.targets.forEach { (target, targetPackage) ->
                    val newName = originName + "_" + target
                    val packageName = symbol.packageName.asString()
                    if (relocateMixin) {
                        mixinPaths.add(packageName.removePrefix(mainMixinPath).removePrefix(".").let { "$it.$newName" })
                    }
                    val meow = environment.codeGenerator.createNewFile(
                        Dependencies(false),
                        packageName.replace(".", "/"),
                        newName,
                        location.extension
                    )
                    val list = files.getOrDefault(target, mutableListOf())
                    list.add(Relocation(meow, content.replace(sourcePackage, targetPackage), newName, originName))
                    files.put(target, list)
                }

            }
            files.entries.forEach { (target, files) ->
                files.forEach { (stream, content) ->
                    var content =
                        content.replace("@${annotation.simpleName}", "@Generated(\"@${annotation.simpleName}\")")
                            .replace(annotation.qualifiedName!!, "javax.annotation.processing.Generated")
                    files.forEach { (_, _, name, origin) ->
                        content = content.replace(origin, name)
                    }
                    stream.use { it.write(content.replacePatterns(target).toByteArray()) }
                }
            }
        }

        FileSpec.builder("org.polyfrost.oneconfig.internal.generated", "RelocatedMixins")
            .indent("    ")
            .addType(
                TypeSpec.objectBuilder("RelocatedMixins")
                    .addModifiers(KModifier.INTERNAL)
                    .addProperty(
                        PropertySpec.builder(
                            "mixins",
                            List::class.asTypeName().parameterizedBy(String::class.asTypeName()),
                            KModifier.PRIVATE
                        ).apply {
                            addAnnotation(AnnotationSpec.builder(ClassName("javax.annotation.processing", "Generated"))
                                .addMember("\"@RelocatedMixins\"").build())
                            if (mixinPaths.isEmpty()) {
                                initializer("emptyList()")
                            } else {
                                initializer("listOf(\n${mixinPaths.joinToString(",\n") { "    \"$it\"" }}\n)")
                            }
                        }.build()
                    ).addFunction(
                        FunSpec.builder("register")
                            .addParameter(
                                ParameterSpec.builder(
                                    "init",
                                    LambdaTypeName.get(
                                        receiver = String::class.asTypeName(),
                                        returnType = Unit::class.asTypeName()
                                    )
                                ).build()
                            ).addCode("mixins.forEach { mixin -> mixin.init() }")
                            .build()
                    )
                    .build()
            ).build().writeTo(environment.codeGenerator, Dependencies(false))


        return emptyList()
    }

    data class Relocation(val output: OutputStream, val content: String, val name: String, val originalName: String)

}