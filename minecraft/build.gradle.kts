@file:Suppress("UnstableApiUsage")
// Shared build logic for all versions of OneConfig.

import com.replaymod.gradle.preprocess.PreprocessTask
import dev.deftu.gradle.utils.GameSide
import dev.deftu.gradle.utils.version.MinecraftReleaseVersion
import dev.deftu.gradle.utils.version.MinecraftVersions
import org.polyfrost.gradle.provideIncludedDependencies
import org.polyfrost.gradle.provideFabricApiDependency
import java.text.SimpleDateFormat
import java.util.function.Predicate
import kotlin.io.path.absolutePathString
import java.lang.Boolean as JBoolean

plugins {
    java
    alias(libs.plugins.kotlin)
    alias(libs.plugins.google.ksp)
    id(libs.plugins.dgt.multiversion.platform.get().pluginId)
    id(libs.plugins.dgt.base.get().pluginId)
    id(libs.plugins.dgt.resources.get().pluginId)
    id(libs.plugins.dgt.loom.get().pluginId)
    id(libs.plugins.dgt.publishing.maven.get().pluginId)
}

evaluationDependsOn(":modules")

if (mcData.isForge) {
    loom.forge.mixinConfig("mixins.oneconfigv1.init.json")
}

toolkitLoomHelper {
    disableRunConfigs(GameSide.SERVER)

    useDevAuth("+")

    useProperty("mixin.debug.export", "true", GameSide.CLIENT)
    useProperty("debugBytecode", "true", GameSide.CLIENT)
    useProperty("forge.logging.console.level", "debug", GameSide.CLIENT)
    if (org.gradle.internal.os.OperatingSystem.current().isMacOsX) {
        useProperty("fml.earlyprogresswindow", "false", GameSide.CLIENT)
    }

    if (mcData.isForge) {
        useForgeMixin("oneconfigv1")
    }

    if (mcData.isLegacyForge) {
        useTweaker("org.polyfrost.oneconfig.internal.legacy.OneConfigTweaker")
    }
}

java {
    withSourcesJar()
    registerFeature("oneConfigModules") {
        usingSourceSet(sourceSets.create("oneConfigModules"))
    }
}

repositories {
    maven("https://repo.polyfrost.org/releases")
    maven("https://repo.polyfrost.org/snapshots")
    maven("https://repo.hypixel.net/repository/Hypixel/")
    maven("https://maven.deftu.dev/releases")
    maven("https://maven.notenoughupdates.org/releases") {
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
        content { includeGroup("com.terraformersmc" ) }
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
}

if (mcData.isLegacyForge) { // Quick substitution for relaunch in dev env, so that mixinextras works properly (yay!)
    configurations.all {
        resolutionStrategy {
            dependencySubstitution {
                all {
                    if (requested is ModuleComponentSelector) {
                        val module = (requested as ModuleComponentSelector)
                        if (module.group == "org.ow2.asm" && module.version == "5.0.3") {
                            logger.warn("Substituting ${module.group}:${module.module}:${module.version} with ${libs.asm.get()}")
                            useTarget(module.group + ":" + module.module + ":" + libs.asm.get().version)
                        }
                    }
                }
            }
        }
    }
}

val includeInLoader = Attribute.of("org.polyfrost.oneconfig.loader.include", Boolean::class.javaObjectType)
val jijInLoader = Attribute.of("org.polyfrost.oneconfig.loader.jij", Boolean::class.javaObjectType)


fun DependencyHandlerScope.handleApiDep(dependency: String, isMod: Boolean = false) {
    val dep = project.dependencies.create(dependency) as ExternalModuleDependency
    this.handleApiDep(dep, isMod)
}

fun DependencyHandlerScope.handleApiDep(dependency: Provider<MinimalExternalModuleDependency>, isMod: Boolean = false) {
    handleApiDep(dependency.get(), isMod)
}

fun DependencyHandlerScope.handleApiDep(dependency: ExternalModuleDependency, isMod: Boolean = false) {
    val dep = "${dependency.group}:${dependency.name}:${dependency.version}"
    if (isMod) "oneConfigModulesCompileOnlyApi"(modApi(dep) {
        isTransitive = false
        attributes {
            attribute(includeInLoader, JBoolean.TRUE)
        }
    }) else api(dep) {
        isTransitive = false
    }
}

preprocess {
    val filter: Predicate<File> = Predicate { !it.toPath().absolutePathString().contains("build/generated/ksp") }
    javaFilter = filter
    kotlinFilter = filter
}

dependencies {
    data class CompatDependency(
        val all: String? = null,
        val forge: String? = all,
        val fabric: String? = all,
        val neoforge: String? = all,
    )

    fun DependencyHandlerScope.compileOnlyCompat(notation: String?) =
        notation?.let { modCompileOnly(it) { isTransitive = false } }

    fun DependencyHandlerScope.compileOnlyCompat(notation: CompatDependency?) {
        when {
            mcData.isNeoForge -> compileOnlyCompat(notation?.neoforge)
            mcData.isForge -> compileOnlyCompat(notation?.forge)
            mcData.isFabric -> compileOnlyCompat(notation?.fabric)
        }
    }

    val mcVersion = mcData.version as MinecraftReleaseVersion
    val tripleVersion = Triple(mcVersion.major, mcVersion.minor, mcVersion.patch)
    val mcVersionString = listOf(mcVersion.major, mcVersion.minor, mcVersion.patch).joinToString(".")

    compileOnlyCompat("gg.essential:vigilance-1.8.9-forge:295")
    compileOnlyCompat("org.notenoughupdates.moulconfig:common:3.11.0")

    fun rconfig(mcVersion: String, modVersion: String, mcVersionOverride: String = mcVersion) =
        mcVersion to CompatDependency("com.teamresourceful.resourcefulconfig:resourcefulconfig-common-$mcVersionOverride:$modVersion")

    val rconfig = mapOf(
        rconfig("1.19.2", "1.0.20"),
        rconfig("1.19.3", "1.1.4"),
        rconfig("1.19.4", "1.2.0"),
        rconfig("1.20.0", "2.1.0", "1.20"),
        rconfig("1.20.1", "2.1.3"),
        rconfig("1.20.2", "2.2.3"),
        rconfig("1.20.4", "2.4.8"),
        rconfig("1.20.5", "2.5.2"),
        rconfig("1.20.6", "2.5.2", "1.20.5"),
        rconfig("1.21.0", "3.0.11", "1.21"),
        rconfig("1.21.1", "3.0.11", "1.21"),
        rconfig("1.21.3", "3.3.4"),
        rconfig("1.21.4", "3.4.3"),
        rconfig("1.21.5", "3.5.9"),
        rconfig("1.21.6", "3.6.2"),
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
        yacl("1.19.0", "1.7.1", withoutLoader = true),
        yacl("1.19.1", "1.7.1", withoutLoader = true),
       "1.19.2" to CompatDependency("maven.modrinth:1eAoo2KR:gJ6ZmZ4Z", "maven.modrinth:1eAoo2KR:Jf2pciI1"),
        yacl("1.19.3", "2.2.0", withoutLoader = true),
        "1.19.4" to CompatDependency("maven.modrinth:1eAoo2KR:gJ6ZmZ4Z", "maven.modrinth:1eAoo2KR:Jf2pciI1"),
        yacl("1.20.0", "3.6.6", "1.20.1"),
        yacl("1.20.1", "3.6.6", "1.20.1"),
        yacl("1.20.2", "3.2.1"),
        yacl("1.20.3", "3.3.2"),
        yacl("1.20.4", "3.6.6", "1.20.4", noForge = true),
        yacl("1.20.5", "3.6.6", "1.20.6"),
        yacl("1.20.6", "3.6.6", "1.20.6"),
        yacl("1.21.0", "3.7.1", "1.21.1"),
        yacl("1.21.1", "3.7.1"),
        yacl("1.21.2", "3.7.1", "1.21.3"),
        yacl("1.21.3", "3.7.1"),
        yacl("1.21.4", "3.7.1"),
        yacl("1.21.5", "3.7.1"),
        yacl("1.21.6", "3.7.1"),
    )
    compileOnlyCompat(yacl[mcVersionString])

    fun modMenu(mcVersion: String, version: String) = mcVersion to CompatDependency(fabric = "com.terraformersmc:modmenu:$version")

    val modMenu = mapOf(
        modMenu("1.16.5", "1.16.23"),
        modMenu("1.17.1", "2.0.16"),
        modMenu("1.18.2", "3.2.5"),
        modMenu("1.18.2", "3.2.5"),
        modMenu("1.19.2", "4.1.2"),
        modMenu("1.19.4", "6.3.1"),
        modMenu("1.20.1", "7.2.2"),
        modMenu("1.20.4", "9.2.0"),
        modMenu("1.20.6", "10.0.0"),
        modMenu("1.21.1", "11.0.3"),
        modMenu("1.21.2", "12.0.0"),
        modMenu("1.21.3", "12.0.0"),
        modMenu("1.21.4", "13.0.3"),
        modMenu("1.21.5", "14.0.0-rc.2"),
    )
    compileOnlyCompat(modMenu[mcVersionString])


    provideIncludedDependencies(
        Triple(mcVersion.major, mcVersion.minor, mcVersion.patch),
        mcData.loader.friendlyString
    ).forEach {
        if (it.dep is String) {
            handleApiDep(it.dep as String, it.mod)
        } else {
            handleApiDep(it.dep as ExternalModuleDependency, it.mod)
        }
    }

    if (mcData.isFabric) {
        provideFabricApiDependency(tripleVersion).forEach {
            include(modApi(if (it.dep is String) it.dep as String else "${(it.dep as ExternalModuleDependency).group}:${(it.dep as ExternalModuleDependency).name}:${(it.dep as ExternalModuleDependency).version}") {
                isTransitive = false
            })
        }
    }

    annotationProcessor(libs.mixin.extras)

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

    compileOnly("com.github.hannibal002:SkyHanni:3.8.0") { isTransitive = false }
    if ((mcData.version as MinecraftReleaseVersion).isNewerThan(MinecraftVersions.VERSION_1_21_4)) {
        compileOnly("net.azureaaron:dandelion:1.0.0-alpha.3") { isTransitive = false }
    }
    api("dev.deftu:enhancedeventbus:2.0.0") // TODO
}

tasks {
    withType(Jar::class) {
        exclude("**/**_Test.**")
        exclude("**/**_Test$**.**")
    }
    remapJar {
        manifest {
            val attributesMap = buildMap<String, Any> {
                putAll(
                    mapOf(
                        "Specification-Title" to modData.id,
                        "Specification-Vendor" to "Polyfrost",
                        "Specification-Version" to "1", // We are version 1 of ourselves, whatever the hell that means
                        "Implementation-Title" to rootProject.name,
                        "Implementation-Version" to project.version,
                        "Implementation-Vendor" to "Polyfrost",
                        "Implementation-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(`java.util`.Date()),
                        "OneConfig-Main-Class" to "org.polyfrost.oneconfig.internal.bootstrap.Bootstrap",
                        "MixinConfigs" to "mixins.oneconfigv1.init.json,mixins.oneconfigv1.json",
                    )
                )
            }
            attributes(attributesMap)
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
                    println("$this > $it")
                    this@configureEach.dependsOn(it)
                }
            }
        }
    }
}

ksp {
    arg("relocator.mcVersion", mcData.version.toString())
}

if (mcData.version < MinecraftVersions.VERSION_1_13) {
    if (
        System.getProperty("os.arch") == "aarch64" &&
        System.getProperty("os.name") == "Mac OS X"
    ) {
        logger.error("Setting up fix with Apple Silicon for Minecraft ${mcData.version}")

        repositories {
            maven("https://maven.legacyfabric.net/") {
                content {
                    includeGroup("org.lwjgl.lwjgl")
                }
            }
        }

        val lwjglVersion = "2.9.4+legacyfabric.8"

        configurations.all {
            resolutionStrategy {
                dependencySubstitution {
                    all {
                        if (requested is ModuleComponentSelector) {
                            val module = (requested as ModuleComponentSelector)
                            if (module.group == "org.lwjgl.lwjgl") {
                                logger.warn("Substituting ${module.group}:${module.module}:${module.version} with ${module.group}:${module.module}:$lwjglVersion")
                                useTarget(module.group + ":" + module.module + ":" + lwjglVersion)
                            }
                        }
                    }
                }
            }
        }
    }
}