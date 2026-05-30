import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import java.lang.Boolean.TRUE

plugins {
    java
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("versioned-catalogues")
}

repositories {
    maven("https://repo.polyfrost.org/releases")
    maven("https://repo.polyfrost.org/snapshots")
    maven("https://repo.hypixel.net/repository/Hypixel/")
    maven("https://maven.deftu.dev/releases")
    maven("https://nexus.prsm.wtf/repository/maven-public/maven-repo/releases/")
    maven("https://maven.notenoughupdates.org/releases/") {
        content { includeGroup("org.notenoughupdates.moulconfig") }
    }
    maven("https://maven.teamresourceful.com/repository/maven-releases") {
        content { includeGroup("com.teamresourceful.resourcefulconfig") }
    }
    maven("https://maven.isxander.dev/releases") {
        content { includeGroup("dev.isxander") }
    }
    maven("https://api.modrinth.com/maven") {
        content { includeGroup("maven.modrinth") } // for some reason yacl versions exist that aren't on the official repo???
    }
    maven("https://maven.terraformersmc.com/") {
        content { includeGroup("com.terraformersmc") }
    }
    maven("https://jitpack.io") {
        content { includeGroupAndSubgroups("com.github") }
    }
    maven("https://maven.teamresourceful.com/repository/maven-public/") {
        content { includeGroupAndSubgroups("me.owdding") }
    }
    maven("https://maven.azureaaron.net/releases") {
        content { includeGroup("net.azureaaron") }
    }
    maven("https://maven.bawnorton.com/releases") {
        content { includeGroup("com.github.bawnorton.mixinsquared") }
    }
    maven("https://maven.parchmentmc.org") {
        content { includeGroupAndSubgroups("org.parchmentmc") }
    }
    maven("https://redirector.kotlinlang.org/maven/compose-dev")
    google()
}

evaluationDependsOn(":modules")

java {
    withSourcesJar()
    registerFeature("oneConfigModules") {
        usingSourceSet(sourceSets.create("oneConfigModules"))
    }
}

private val stonecutter = project.extensions.getByName("stonecutter") as StonecutterBuildExtension
val loader = stonecutter.current.project.substringAfterLast("-")


val includeInLoader = Attribute.of("org.polyfrost.oneconfig.loader.include", Boolean::class.javaObjectType)
val jijInLoader = Attribute.of("org.polyfrost.oneconfig.loader.jij", Boolean::class.javaObjectType)


fun DependencyHandlerScope.handleApiDep(dependency: String, isMod: Boolean = false, transitive: Boolean = false) {
    val dep = project.dependencies.create(dependency) as ExternalModuleDependency
    this.handleApiDep(dep, isMod)
}

fun DependencyHandlerScope.handleApiDep(
    dependency: ExternalModuleDependency,
    isMod: Boolean = false,
    transitive: Boolean = false,
) {
    this.handleApiDep(project.provider { dependency }, isMod)
}

if (loader != "fabric") {
    configurations {
        val localRuntime = create("localRuntime")
        named("runtimeClasspath") { this.extendsFrom(localRuntime) }
    }
}

@JvmName("handleApiDepBundle")
fun DependencyHandlerScope.handleApiDep(
    dependency: Provider<out ExternalModuleDependencyBundle>,
    isMod: Boolean = false,
    transitive: Boolean = false,
) {
    dependency.get().forEach { handleApiDep(it, isMod) }
}

fun DependencyHandlerScope.handleApiDep(
    dependency: Provider<out ExternalModuleDependency>,
    isMod: Boolean = false,
    transitive: Boolean = false,
) {
    if (isMod) "modImplementation"(dependency) {
        isTransitive = transitive
        attributes {
            attribute(includeInLoader, TRUE)
        }
    } else {
        "implementation"(dependency) {
            isTransitive = transitive
        }
    }
}
dependencies {
    data class CompatDependency(
        val all: String? = null,
        val forge: String? = all,
        val fabric: String? = all,
        val neoforge: String? = all,
    )

    fun DependencyHandlerScope.compileOnlyCompat(notation: String?) =
        notation?.let { "modCompileOnly"(it) { isTransitive = false } }

    fun DependencyHandlerScope.compileOnlyCompat(notation: CompatDependency?) {
        when {
            //mcData.isNeoForge -> compileOnlyCompat(notation?.neoforge)
            //mcData.isForge -> compileOnlyCompat(notation?.forge)
            //mcData.isFabric -> compileOnlyCompat(notation?.fabric)
        }
    }

    val mcVersionString = stonecutter.current

    compileOnlyCompat("org.notenoughupdates.moulconfig:common:3.11.0")
    //skyhanniRelocatedConfiguration("org.notenoughupdates.moulconfig:common:3.11.0")
    //compileOnly(prebundle(skyhanniRelocatedConfiguration))
    //firmamentRelocatedConfiguration("org.notenoughupdates.moulconfig:common:3.11.0")
    //compileOnly(prebundle(firmamentRelocatedConfiguration))
    //dandelionBpRelocatedConfiguration("org.notenoughupdates.moulconfig:common:3.11.0")
    //compileOnly(prebundle(dandelionBpRelocatedConfiguration))

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

    //compileOnlyCompat(rconfig[mcVersionString])

    fun yacl(
        mcVersion: String,
        modVersion: String,
        mcVersionOverride: String = mcVersion,
        withoutLoader: Boolean = false,
        noForge: Boolean = false,
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
    //compileOnlyCompat(yacl[mcVersionString])

    fun modMenu(mcVersion: String, version: String) =
        mcVersion to CompatDependency(fabric = "com.terraformersmc:modmenu:$version")

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
    //compileOnlyCompat(modMenu[mcVersionString])

    "implementation"(versionedCatalog["jetbrains.compose.foundation"])
    "implementation"(versionedCatalog["jetbrains.compose.material"])
    "implementation"(versionedCatalog["jetbrains.compose.runtime"])
    "implementation"(versionedCatalog["jetbrains.compose.ui"])
    "implementation"(versionedCatalog["jetbrains.compose.ui.tooling.preview"])
    "implementation"(versionedCatalog["jetbrains.compose.ui.util"])
    "implementation"(versionedCatalog["jetbrains.skiko.awt"])
    "implementation"(versionedCatalog["jetbrains.skiko.awt.runtime.windows.x64"])
    "implementation"(versionedCatalog["jetbrains.skiko.awt.runtime.linux.x64"])
    "implementation"(versionedCatalog["jetbrains.skiko.awt.runtime.macos.x64"])
    "implementation"(versionedCatalog["jetbrains.skiko.awt.runtime.macos.arm64"])
    "implementation"(versionedCatalog["jetbrains.compose.navigation"])
    "implementation"(versionedCatalog["jetbrains.lifecycle"])
    "implementation"(versionedCatalog["jetbrains.viewmodel"])
    "implementation"(versionedCatalog["commonmark"])
    "implementation"(versionedCatalog["adventure"])

    if (loader == "fabric") {
        "modImplementation"("net.kyori:adventure-platform-mod-shared-fabric-repack:6.8.0")
    }

    handleApiDep(versionedCatalog.bundles["kotlin"])
    handleApiDep(versionedCatalog.bundles["kotlinx"])
    handleApiDep(versionedCatalog.bundles["nightconfig"])
    handleApiDep(versionedCatalog["snakeyaml"])
    handleApiDep(versionedCatalog["isolated-lwjgl3-loader"]) //todo check if needed
    handleApiDep(versionedCatalog["polyio"]) //todo check if needed
    val copycat = versionedCatalog["copycat"].get()
    handleApiDep(copycat)
    setOf(
        "windows" to setOf("x64", "x86"),
        "linux" to setOf("x64", "x86", "arm", "arm64"),
        "osx" to setOf("x64", "arm64")
    ).forEach { (os, arches) ->
        arches.forEach { arch ->
            handleApiDep("${copycat.group}:${copycat.name}-natives-$os-$arch:${copycat.version}")
        }
    }

    handleApiDep(versionedCatalog["copycat-image-awt"])
    handleApiDep(versionedCatalog["hypixel-modapi"])
    handleApiDep(versionedCatalog["hypixel-data"])

    handleApiDep(versionedCatalog["mixin-squared"])
    handleApiDep(versionedCatalog["commonmark"])

    if (loader == "fabric") {
        handleApiDep(versionedCatalog["fabric-language-kotlin"], transitive = true)
        handleApiDep(versionedCatalog["fabric-loader"], isMod = true, transitive = true)
        handleApiDep(versionedCatalog.bundles["fabric-api"], true, transitive = true)
    }

    if (versionedCatalog.has("cinnabar")) {
        handleApiDep(versionedCatalog["cinnabar"])
    }

    //if (mcData.isFabric) {
    //    provideFabricApiDependency(tripleVersion).forEach {
    //        @Suppress("USELESS_CAST")
    //        maybeModApi(if (it.dep is String) it.dep as String else "${(it.dep as ExternalModuleDependency).group}:${(it.dep as ExternalModuleDependency).name}:${(it.dep as ExternalModuleDependency).version}") {
    //            isTransitive = false
    //        }
    //    }
    //}

    "annotationProcessor"(versionedCatalog["mixin.extras"])
    "annotationProcessor"(versionedCatalog["mixin.squared"])

    //"ksp"(rootProject.project(":modules:relocator"))
    //"annotationProcessor"(rootProject.project(":modules:relocator"))

    for (project in rootProject.project(":modules").subprojects) {
        if ("relocator" in project.path) {
            "compileOnly"(project(project.path))
        } else if ("dependencies" !in project.path) {
            "api"(project(project.path)) {
                isTransitive = false
                attributes {
                    attribute(includeInLoader, TRUE)
                }
            }
        }
    }

    //api("dev.deftu:enhancedeventbus:2.0.0") // TODO
    if (properties["minecraft.vulkan"] != null) {
        // i couldnt find a way to get it to work
        // soooooooooooooooooooooooooooooooo
        val fabricApiPatchSrc = configurations.create("fabricApiPatchSrc") {
            isCanBeConsumed = false
            isCanBeResolved = true
        }
        /*
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

         */
        //val patchedScreenApi = patchedFabricApiDir.map { it.file("fabric-screen-api-v1-4.0.0+9f78a5a8ed.jar") }
        //val patchedRenderingV1 = patchedFabricApiDir.map { it.file("fabric-rendering-v1-23.0.2+f348b6c3c3.jar") }
        //"maybeModApi"(files(patchedScreenApi) { builtBy(patchFabricApiMods) })
        //"maybeModApi"(files(patchedRenderingV1) { builtBy(patchFabricApiMods) })
    }
}

version = project.parent!!.version

tasks.withType<ProcessResources>() {
    val range = if (versionedCatalog.versions.has("minecraft.range")) {
        versionedCatalog.versions.get("minecraft.range").toString()
    } else {
        val start = versionedCatalog.versions.getOrFallback("minecraft.start", "minecraft")
        val end = versionedCatalog.versions.getOrFallback("minecraft.end", "minecraft")
        ">=$start <=$end"
    }
    val version = project.version

    val fabricProperties = buildMap {
        put("mod_version", version)
        put("fabric_mc_version", range)
    }

    this.inputs.properties(fabricProperties)

    this.filesMatching("fabric.mod.json") {
        expand(fabricProperties)
    }
}
