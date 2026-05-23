plugins {
    `oneconfig-setup`
}

/*@file:Suppress("UnstableApiUsage")
// Shared build logic for all versions of OneConfig.

import com.google.devtools.ksp.gradle.KspAATask
import com.replaymod.gradle.preprocess.PreprocessTask
import com.replaymod.gradle.preprocess.ProjectGraphNode
import com.replaymod.gradle.preprocess.RootPreprocessExtension
import dev.deftu.gradle.utils.GameSide
import dev.deftu.gradle.utils.version.MinecraftDropVersion
import dev.deftu.gradle.utils.version.MinecraftReleaseVersion
import dev.deftu.gradle.utils.version.MinecraftVersions
import gg.essential.gradle.util.RelocationTransform.Companion.registerRelocationAttribute
import gg.essential.gradle.util.prebundle
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.internal.builtins.StandardNames.FqNames.target
import org.polyfrost.gradle.provideFabricApiDependency
import org.polyfrost.gradle.provideIncludedDependencies
import java.text.SimpleDateFormat
import java.util.Date
import java.util.function.Predicate
import kotlin.io.path.absolutePathString
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import java.lang.Boolean as JBoolean

val skyhanniRelocated = registerRelocationAttribute("relocate-skyhanni-moulconfig") {
    relocate("io.github.notenoughupdates.moulconfig", "at.hannibal2.skyhanni.deps.moulconfig")
}

val skyhanniRelocatedConfiguration: Configuration by configurations.creating {
    attributes { attribute(skyhanniRelocated, true) }
}

val firmamentRelocated = registerRelocationAttribute("relocate-firmament-moulconfig") {
    relocate("io.github.notenoughupdates.moulconfig", "moe.nea.firmament.deps.moulconfig")
}

val firmamentRelocatedConfiguration: Configuration by configurations.creating {
    attributes { attribute(firmamentRelocated, true) }
}

val dandelionBpRelocated = registerRelocationAttribute("relocate-dandelion-bp-moulconfig") {
    relocate("io.github.notenoughupdates.moulconfig", "net.azureaaron.dandelion_bp.deps.moulconfig")
}

val dandelionBpRelocatedConfiguration: Configuration by configurations.creating {
    attributes { attribute(dandelionBpRelocated, true) }
}
dependencies {
    data class CompatDependency(
        val all: String? = null,
        val forge: String? = all,
        val fabric: String? = all,
        val neoforge: String? = all,
    )

    fun DependencyHandlerScope.compileOnlyCompat(notation: String?) =
        notation?.let { maybeModCompileOnly(it) { isTransitive = false } }

    fun DependencyHandlerScope.compileOnlyCompat(notation: CompatDependency?) {
        when {
            mcData.isNeoForge -> compileOnlyCompat(notation?.neoforge)
            mcData.isForge -> compileOnlyCompat(notation?.forge)
            mcData.isFabric -> compileOnlyCompat(notation?.fabric)
        }
    }

    val mcVersion = mcData.version
    val tripleVersion = when (mcVersion) {
        is MinecraftDropVersion -> Triple(mcVersion.year, mcVersion.drop, mcVersion.patch)
        is MinecraftReleaseVersion -> Triple(mcVersion.major, mcVersion.minor, mcVersion.patch)
        else -> error("no")
    }

    val mcVersionString = mcData.version.toString()

    compileOnlyCompat("gg.essential:vigilance-1.8.9-forge:299")
    compileOnlyCompat("org.notenoughupdates.moulconfig:common:3.11.0")
    skyhanniRelocatedConfiguration("org.notenoughupdates.moulconfig:common:3.11.0")
    compileOnly(prebundle(skyhanniRelocatedConfiguration))
    firmamentRelocatedConfiguration("org.notenoughupdates.moulconfig:common:3.11.0")
    compileOnly(prebundle(firmamentRelocatedConfiguration))
    dandelionBpRelocatedConfiguration("org.notenoughupdates.moulconfig:common:3.11.0")
    compileOnly(prebundle(dandelionBpRelocatedConfiguration))

    fun rconfig(mcVersion: String, modVersion: String, mcVersionOverride: String = mcVersion) =
        mcVersion to CompatDependency("com.teamresourceful.resourcefulconfig:resourcefulconfig-common-$mcVersionOverride:$modVersion")

    val rconfig = mapOf(
        rconfig("1.21.1", "3.0.11", "1.21"),
        rconfig("1.21.2", "3.0.11", "1.21"),
        rconfig("1.21.3", "3.3.4"),
        rconfig("1.21.4", "3.4.3"),
        rconfig("1.21.5", "3.5.9"),
        rconfig("1.21.6", "3.6.2"),
        rconfig("1.21.7", "3.7.2"),
        rconfig("1.21.8", "3.7.2", "1.21.7"),
        rconfig("1.21.9", "3.9.1"),
        rconfig("1.21.10", "3.9.1", "1.21.9"),
        rconfig("1.21.11", "3.9.1", "1.21.9"),
    )

    compileOnlyCompat(rconfig[mcVersionString])

    fun yacl(
        mcVersion: String,
        modVersion: String,
        mcVersionOverride: String = mcVersion,
        withoutLoader: Boolean = false,
        noForge: Boolean = false
    ) = mcVersion to if (withoutLoader)
        CompatDependency("dev.isxander:yet-another-config-lib:$modVersion")
    else CompatDependency(
        fabric = "dev.isxander:yet-another-config-lib:$modVersion+$mcVersionOverride-fabric",
        forge = "dev.isxander:yet-another-config-lib:$modVersion+$mcVersionOverride-forge".takeUnless { noForge },
        neoforge = "dev.isxander:yet-another-config-lib:$modVersion+$mcVersionOverride-neoforge"
    )

    val yacl = mapOf(
        yacl("1.21.1", "3.7.1"),
        yacl("1.21.2", "3.7.1", "1.21.3"),
        yacl("1.21.3", "3.7.1"),
        yacl("1.21.4", "3.7.1"),
        yacl("1.21.5", "3.7.1"),
        yacl("1.21.6", "3.7.1"),
        yacl("1.21.7", "3.7.1", "1.21.6"),
        yacl("1.21.8", "3.7.1", "1.21.6"),
        yacl("1.21.9", "3.8.0", "1.21.6"),
        yacl("1.21.10", "3.8.0", "1.21.9"),
        yacl("1.21.11", "3.8.0", "1.21.9"),
    )
    compileOnlyCompat(yacl[mcVersionString])

    fun modMenu(mcVersion: String, version: String) = mcVersion to CompatDependency(fabric = "com.terraformersmc:modmenu:$version")

    val modMenu = mapOf(
        modMenu("1.21.1", "11.0.3"),
        modMenu("1.21.2", "12.0.0"),
        modMenu("1.21.3", "12.0.0"),
        modMenu("1.21.4", "13.0.3"),
        modMenu("1.21.5", "14.0.0-rc.2"),
        modMenu("1.21.6", "15.0.0-beta.3"),
        modMenu("1.21.7", "15.0.0-beta.3"),
        modMenu("1.21.8", "15.0.0-beta.3"),
        modMenu("1.21.10", "16.0.0-rc.1"),
        modMenu("1.21.11", "17.0.0-alpha.1"),
    )
    compileOnlyCompat(modMenu[mcVersionString])

    provideIncludedDependencies(
        tripleVersion,
        mcData.loader.friendlyString,
        mcVersion.toString()
    ).forEach {
        if (it.dep is String) {
            @Suppress("USELESS_CAST")
            handleApiDep(it.dep as String, it.mod)
        } else {
            handleApiDep(it.dep as ExternalModuleDependency, it.mod)
        }
    }

    if (mcData.isFabric) {
        provideFabricApiDependency(tripleVersion).forEach {
            @Suppress("USELESS_CAST")
            maybeModApi(if (it.dep is String) it.dep as String else "${(it.dep as ExternalModuleDependency).group}:${(it.dep as ExternalModuleDependency).name}:${(it.dep as ExternalModuleDependency).version}") {
                isTransitive = false
            }
        }
    }

    annotationProcessor(libs.mixin.extras)
    annotationProcessor(libs.mixin.squared)

    for (dep in listOf("-nanovg").run {
        if (mcData.version < MinecraftVersions.VERSION_1_13) this else this + listOf(
            "-tinyfd",
            "-stb",
            ""
        )
    }) {
        val lwjglDep = "org.lwjgl:lwjgl$dep:${libs.versions.lwjgl.get()}"
        compileOnlyApi(lwjglDep) {
            isTransitive = false
        }
    }

    ksp(rootProject.project(":modules:relocator"))
    annotationProcessor(rootProject.project(":modules:relocator"))

    if (properties["minecraft.vulkan"] != null) {
        implementation("graphics.cinnabar:cinnabar-fabric:26.1-snapshot-9-0.0.7-beta-85-gd3508cc")
    }

    for (project in rootProject.project(":modules").subprojects) {
        if ("relocator" in project.path) {
            compileOnly(project(project.path))
        } else if ("dependencies" !in project.path) {
            "oneConfigModulesCompileOnlyApi"(localRuntime(compileOnly(project(project.path)) {
                isTransitive = false
                attributes {
                    attribute(includeInLoader, JBoolean.TRUE)
                }
            })!!)
        }
    }
    if (mcData.isLegacyForge) {
        "oneConfigModulesCompileOnlyApi"(project(":modules:dependencies:legacy")) {
            isTransitive = false
            attributes {
                attribute(includeInLoader, JBoolean.TRUE)
                attribute(jijInLoader, JBoolean.TRUE)
            }
        }
    }

    if (mcData.isLegacyForge) {
        compileOnly("cc.polyfrost:oneconfig-$mcData:0.2.2-alpha216") {
            isTransitive = false
        }
    }

    if (mcData.version > MinecraftVersions.VERSION_1_21_4) {
        compileOnly("net.azureaaron:dandelion:1.0.0-alpha.3") { isTransitive = false }
    }
    api("dev.deftu:enhancedeventbus:2.0.0") // TODO
    if (properties["minecraft.vulkan"] != null) {
        // i couldnt find a way to get it to work
        // soooooooooooooooooooooooooooooooo
        val fabricApiPatchSrc = configurations.create("fabricApiPatchSrc") {
            isCanBeConsumed = false
            isCanBeResolved = true
        }
        dependencies.add(fabricApiPatchSrc.name, "net.fabricmc.fabric-api:fabric-screen-api-v1:4.0.0+9f78a5a8ed") { isTransitive = false }
        dependencies.add(fabricApiPatchSrc.name, "net.fabricmc.fabric-api:fabric-rendering-v1:23.0.2+f348b6c3c3") { isTransitive = false }

        val patchedFabricApiDir = layout.buildDirectory.dir("patched-fabric-api")

        val patchFabricApiMods by tasks.registering {
            inputs.files(fabricApiPatchSrc)
            outputs.dir(patchedFabricApiDir)
            doLast {
                val outDir = patchedFabricApiDir.get().asFile
                outDir.mkdirs()
                val jarFiles: Set<File> = fabricApiPatchSrc.resolvedConfiguration.resolvedArtifacts.map { it.file }.toSet()
                for (jar: File in jarFiles) {
                    val mixinJson: String? = when {
                        "screen-api" in jar.name -> "fabric-screen-api-v1.mixins.json"
                        "rendering-v1" in jar.name -> "fabric-rendering-v1.mixins.json"
                        else -> null
                    }
                    val outFile = File(outDir, jar.name)
                    ZipFile(jar).use { zf ->
                        ZipOutputStream(outFile.outputStream().buffered()).use { zos ->
                            zf.entries().asSequence().forEach { entry ->
                                val bytes = zf.getInputStream(entry).readBytes()
                                val content = if (mixinJson != null && entry.name == mixinJson) {
                                    var json = String(bytes)
                                        .replace("\"required\": true", "\"required\": false")
                                        .replace("\"defaultRequire\": 1", "\"defaultRequire\": 0")
                                    if ("rendering-v1" in mixinJson) {
                                        json = json.replace(Regex("\"client\"\\s*:\\s*\\[.*?\\]", RegexOption.DOT_MATCHES_ALL), "\"client\": []")
                                    }
                                    json.toByteArray()
                                } else bytes
                                zos.putNextEntry(ZipEntry(entry.name))
                                zos.write(content)
                                zos.closeEntry()
                            }
                        }
                    }
                }
            }
        }

        val patchedScreenApi = patchedFabricApiDir.map { it.file("fabric-screen-api-v1-4.0.0+9f78a5a8ed.jar") }
        val patchedRenderingV1 = patchedFabricApiDir.map { it.file("fabric-rendering-v1-23.0.2+f348b6c3c3.jar") }
        maybeModApi(files(patchedScreenApi) { builtBy(patchFabricApiMods) })
        maybeModApi(files(patchedRenderingV1) { builtBy(patchFabricApiMods) })
    }
}

tasks {
    val manifestFunc = { manifest: Manifest ->
        val attributesMap = buildMap<String, Any> {
            putAll(
                mapOf(
                    "Specification-Title" to modData.id,
                    "Specification-Vendor" to "Polyfrost",
                    "Specification-Version" to "1", // We are version 1 of ourselves, whatever the hell that means
                    "Implementation-Title" to rootProject.name,
                    "Implementation-Version" to project.version,
                    "Implementation-Vendor" to "Polyfrost",
                    "Implementation-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(Date()),
                    "OneConfig-Main-Class" to "org.polyfrost.oneconfig.internal.bootstrap.Bootstrap",
                    "MixinConfigs" to "mixins.oneconfigv1.init.json,mixins.oneconfigv1.json",)
            )
        }
        manifest.attributes(attributesMap)
        Unit
    }

    withType(Jar::class) {
        exclude("** /**_Test.**")
        exclude("**/**_Test$**.**")
    }
    if (mcData.version.isDrop) {
        jar {
            manifest(manifestFunc)
        }
    } else {
        named<org.gradle.jvm.tasks.Jar>("remapJar") {
            manifest(manifestFunc)
        }
    }
    processResources {
        if (mcData.version >= MinecraftVersions.VERSION_1_13) {
            exclude("patched-lwjgl/**")
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            named<MavenPublication>("mavenJava") {
                groupId = group.toString()
                artifactId = mcData.toString()

                signing {
                    isRequired = project.properties["signing.keyId"] != null
                    sign(this@named)
                }
            }
        }
    }

    tasks.withType<PreprocessTask>().configureEach {
        for (project in rootProject.project(":modules").subprojects) {
            if ("dependencies" !in project.path) {
                project.tasks.findByPath("jar")?.let {
                    this@configureEach.dependsOn(it)
                }
            }
        }

        fun recurseAndAdd(list: MutableList<ProjectGraphNode>, node: ProjectGraphNode) {
            list.add(node)
            for (child in node.links) {
                recurseAndAdd(list, child.first)
            }
        }
        val rootPreprocess = parent!!.extensions.getByType<RootPreprocessExtension>()
        val nodes = mutableListOf<ProjectGraphNode>()
        recurseAndAdd(nodes, rootPreprocess.rootNode!!)
        var previousNode: ProjectGraphNode? = null
        nodes.reversed().forEach {
            if (it.project == project.name) {
                val previousProject = rootProject.project(":minecraft:${previousNode!!.project}")
                this@configureEach.dependsOn(previousProject.tasks.withType<KspAATask>())
                return@forEach
            }
            previousNode = it
        }
    }
}
*/