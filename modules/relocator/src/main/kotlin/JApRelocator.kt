package org.polyfrost.oneconfig.relocator

import org.polyfrost.oneconfig.relocator.SourceFileHelper.replacePatterns
import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import javax.tools.JavaFileObject
import kotlin.io.path.deleteIfExists
import kotlin.io.path.toPath

/**
 * The relocator for java source files.
 *
 */
@SupportedAnnotationTypes("org.polyfrost.oneconfig.relocator.annotations.*")
internal class JApRelocator : AbstractProcessor() {

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val test = this.processingEnv.filer.createSourceFile("test" + UUID.randomUUID().toString().replace("-", ""))
        SourceFileHelper.dir = test.toUri().toPath()
            .also { it.deleteIfExists() }.parent.parent.parent.parent.parent.parent.parent.also { println(it) }
        annotations.forEach {
            val (_, relocation) = Locations.relocations.entries
                .find { (key) ->
                    println(key.qualifiedName!!)
                    println(it.qualifiedName.toString())
                    key.qualifiedName!! == it.qualifiedName.toString()
                }!!
            val elementsAnnotatedWith = roundEnv.getElementsAnnotatedWith(it)
            val (sourcePackage, targets) = relocation
            val create = mutableMapOf<String, MutableList<Relocation>>()
            elementsAnnotatedWith.forEach { element ->
                this.processingEnv.elementUtils

                val sourceFile = SourceFileHelper.readSourceFile(SourceKind.JAVA, element.toString())
                if (sourceFile == null) {
                    processingEnv.messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "Unable to locate source file for $element"
                    )
                    return false
                }

                val className = element.simpleName.toString().substringAfterLast(".")
                targets.forEach { (target, targetPackage) ->
                    val output = processingEnv.filer.createSourceFile("${element}_$target", element)
                    val relocated = sourceFile.replace(sourcePackage, targetPackage)


                    val list = create.getOrDefault(target, mutableListOf())
                    list.add(Relocation(output, relocated, "${className}_$target", className))
                    create[target] = list
                }
            }
            create.entries.forEach { (target, list) ->
                list.forEach { (writer, content, name, origin) ->
                    var content = content
                    list.forEach { (_, _, otherName, otherOrigin) ->
                        content = content.replace(otherOrigin, otherName)
                    }
                    writer.openWriter().use { it.write(content.replacePatterns(target)) }
                }
            }
        }
        return true
    }

    data class Relocation(val output: JavaFileObject, val content: String, val name: String, val originalName: String)

    override fun getSupportedSourceVersion(): SourceVersion? {
        return this.processingEnv.sourceVersion
    }
}