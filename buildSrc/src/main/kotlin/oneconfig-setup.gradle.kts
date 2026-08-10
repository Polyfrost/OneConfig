import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.PublishingExtension
import org.gradle.authentication.http.BasicAuthentication
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import gg.essential.gradle.util.RelocationTransform.Companion.registerRelocationAttribute
import gg.essential.gradle.util.prebundle
import java.lang.Boolean.TRUE

plugins {
    `java-library`
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("versioned-catalogues")
    `maven-publish`
    signing
}

repositories {
    fun scopedMaven(url: String, vararg groups: String, includeSubgroups: Boolean = false) = maven(url) {
        content { for (group in groups) if (!includeSubgroups) includeGroup(group) else includeGroupAndSubgroups(group) }
    }

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
    maven("https://jitpack.io") {
        content { includeGroupAndSubgroups("com.github") }
    }
    maven("https://maven.terraformersmc.com/releases") {
        content { includeGroup("com.terraformersmc") }
    }
    maven("https://maven.teamresourceful.com/repository/maven-public/") {
        content { includeGroupAndSubgroups("me.owdding"); includeGroupAndSubgroups("tech.thatgravyboat") }
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
    scopedMaven("https://central.sonatype.com/repository/maven-snapshots/", "net.kyori")
    scopedMaven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1", "me.djtheredstoner")
    scopedMaven("https://maven.azureaaron.net/snapshots", "net.azureaaron")
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
val enableMoulRelocatorKsp = loader == "fabric"
        && stonecutter.eval(stonecutter.current.version, "> 1.21.10")
        && versionedCatalog.has("moulconfig")


val includeInLoader = Attribute.of("org.polyfrost.oneconfig.loader.include", Boolean::class.javaObjectType)
val jijInLoader = Attribute.of("org.polyfrost.oneconfig.loader.jij", Boolean::class.javaObjectType)

if (loader == "fabric") {
    val modMenuShimClasses = layout.buildDirectory.dir("classes/modMenuShim")
    val compileModMenuApiShimJava = tasks.register<JavaCompile>("compileModMenuApiShimJava") {
        val mainSourceSet = sourceSets.named("main").get()
        source(rootProject.projectDir.resolve("minecraft/src/modMenuShim/java"))
        classpath = files(mainSourceSet.output.classesDirs, mainSourceSet.compileClasspath)
        destinationDirectory.set(modMenuShimClasses)
        dependsOn(tasks.named("compileJava"), tasks.named("compileKotlin"))
    }
    val modMenuApiShimJar = tasks.register<Jar>("modMenuApiShimJar") {
        archiveFileName.set("modmenu-api-shim.jar")
        from(modMenuShimClasses)
        dependsOn(compileModMenuApiShimJava)
    }
    tasks.named<ProcessResources>("processResources") {
        from(modMenuApiShimJar) {
            into("META-INF/oneconfig")
        }
    }
}

// The legacy fade_in_blur post/program shaders use the pre-1.21.4 format and are unused on 1.21.1+
if (stonecutter.eval(stonecutter.current.version, ">= 1.21.4")
    && stonecutter.eval(stonecutter.current.version, "< 1.21.5")) {
    tasks.named<ProcessResources>("processResources") {
        exclude("assets/minecraft/shaders/post/fade_in_blur.json")
        exclude("assets/minecraft/shaders/program/fade_in_blur.json")
        exclude("assets/minecraft/shaders/program/fade_in_blur.fsh")
    }
}


fun DependencyHandlerScope.handleApiDep(dependency: String, isMod: Boolean = false, transitive: Boolean = false) {
    val dep = project.dependencies.create(dependency) as ExternalModuleDependency
    this.handleApiDep(dep, isMod, transitive)
}

fun DependencyHandlerScope.handleApiDep(
    dependency: ExternalModuleDependency,
    isMod: Boolean = false,
    transitive: Boolean = false,
) {
    this.handleApiDep(project.provider { dependency }, isMod, transitive)
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
    dependency.get().forEach { handleApiDep(it, isMod, transitive) }
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
        "api"(dependency) {
            isTransitive = transitive
        }
    }
}


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

dependencies {
    listOf("compat", "common-compat").forEach {
        versionedCatalog.bundles.getOrNull(it)?.let { bundle ->
            "modCompileOnly"(bundle) {
                isTransitive = false
            }
        }
    }
    fun moulConfig(vararg configurations: Configuration) {
        if (!versionedCatalog.has("moulconfig")) return
        compileOnly(versionedCatalog["moulconfig"])
        configurations.forEach { configuration ->
            configuration(versionedCatalog["moulconfig"])
            compileOnly(prebundle(configuration))
        }
    }

    moulConfig(skyhanniRelocatedConfiguration, firmamentRelocatedConfiguration)

    "api"(versionedCatalog["jetbrains.compose.foundation"])
    "api"(versionedCatalog["jetbrains.compose.material"])
    "api"(versionedCatalog["jetbrains.compose.runtime"])
    "api"(versionedCatalog["jetbrains.compose.ui"])
    "api"(versionedCatalog["jetbrains.compose.ui.tooling.preview"])
    "api"(versionedCatalog["jetbrains.compose.ui.util"])
    "api"(versionedCatalog["jetbrains.skiko.awt"])
    "api"(versionedCatalog["jetbrains.skiko.awt.runtime.windows.x64"])
    "api"(versionedCatalog["jetbrains.skiko.awt.runtime.linux.x64"])
    "api"(versionedCatalog["jetbrains.skiko.awt.runtime.macos.x64"])
    "api"(versionedCatalog["jetbrains.skiko.awt.runtime.macos.arm64"])
    "api"(versionedCatalog["jetbrains.compose.navigation"])
    "api"(versionedCatalog["jetbrains.lifecycle"])
    "api"(versionedCatalog["jetbrains.viewmodel"])
    "api"(versionedCatalog["commonmark"])
    handleApiDep(versionedCatalog.bundles["adventure"])

    if (loader == "fabric") {
        val adventurePlatformVersion =
            when {
                stonecutter.eval(stonecutter.current.version, ">= 26.2") -> "7.0.0-SNAPSHOT"
                stonecutter.eval(stonecutter.current.version, ">= 26.1") -> "6.9.0"
                stonecutter.eval(stonecutter.current.version, ">= 1.21.11") -> "6.8.0"
                stonecutter.eval(stonecutter.current.version, ">= 1.21.10") -> "6.7.0"
                stonecutter.eval(stonecutter.current.version, ">= 1.21.8") -> "6.6.0"
                stonecutter.eval(stonecutter.current.version, ">= 1.21.5") -> "6.4.0"
                stonecutter.eval(stonecutter.current.version, ">= 1.21.4") -> "6.3.0"
                stonecutter.eval(stonecutter.current.version, ">= 1.21.1") -> "5.14.2"
                else -> error("No adventure-platform-fabric version for ${stonecutter.current.version}")
            }
        val adventurePlatform = "net.kyori:adventure-platform-fabric:$adventurePlatformVersion"
        "modApi"(adventurePlatform) { exclude("net.fabricmc.fabric-api") }
        "modImplementation"(adventurePlatform) { exclude("net.fabricmc.fabric-api") }
    }

    handleApiDep(versionedCatalog.bundles["kotlin"])
    handleApiDep(versionedCatalog.bundles["kotlinx"])
    handleApiDep(versionedCatalog.bundles["nightconfig"])
    handleApiDep(versionedCatalog["snakeyaml"])
    handleApiDep(versionedCatalog["isolated-lwjgl3-loader"]) //todo check if needed
    handleApiDep(versionedCatalog["java-objc-bridge"])
    val hypixelModApiVersion = if (stonecutter.eval(stonecutter.current.version, ">= 26.1")) "1.0.2" else "1.0.1"
    handleApiDep("net.hypixel:mod-api:$hypixelModApiVersion")
    handleApiDep(versionedCatalog["hypixel-data"])

    if (loader == "fabric") {
        val hypixelFabricMod =
            if (stonecutter.eval(stonecutter.current.version, ">= 26.1")) {
                "maven.modrinth:hypixel-mod-api:1.0.2+build.1+mc26.1"
            } else {
                "maven.modrinth:hypixel-mod-api:1.0.1+build.1+mc1.21"
            }
        if (stonecutter.eval(stonecutter.current.version, ">= 26.1")) {
            "implementation"(hypixelFabricMod) { isTransitive = false }
        } else {
            "modImplementation"(hypixelFabricMod) { isTransitive = false }
        }
    }

    handleApiDep(versionedCatalog["mixin-squared"])
    handleApiDep(versionedCatalog["commonmark"])

    if (loader == "fabric") {
        handleApiDep(versionedCatalog["fabric-language-kotlin"], transitive = true)
        handleApiDep(versionedCatalog["fabric-loader"], isMod = true, transitive = true)
        handleApiDep(versionedCatalog.bundles["fabric-api"], true, transitive = true)

        val fullFabricApiVersion = when {
            stonecutter.eval(stonecutter.current.version, ">= 26.2") -> "0.155.0+26.2"
            stonecutter.eval(stonecutter.current.version, ">= 26.1") -> "0.155.0+26.1.2"
            stonecutter.eval(stonecutter.current.version, ">= 1.21.11") -> "0.141.5+1.21.11"
            stonecutter.eval(stonecutter.current.version, ">= 1.21.10") -> "0.138.4+1.21.10"
            stonecutter.eval(stonecutter.current.version, ">= 1.21.8") -> "0.136.1+1.21.8"
            stonecutter.eval(stonecutter.current.version, ">= 1.21.5") -> "0.128.2+1.21.5"
            stonecutter.eval(stonecutter.current.version, ">= 1.21.4") -> "0.119.4+1.21.4"
            else -> "0.116.14+1.21.1"
        }
        "modCompileOnly"("net.fabricmc.fabric-api:fabric-api:$fullFabricApiVersion")
        "modRuntimeOnly"("net.fabricmc.fabric-api:fabric-api:$fullFabricApiVersion")
    }

    val libsCatalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
    when (loader) {
        "fabric" -> "modRuntimeOnly"(libsCatalog.findLibrary("devauth-fabric").get())
        "neoforge" -> "runtimeOnly"(libsCatalog.findLibrary("devauth-neoforge").get())
    }

    if (versionedCatalog.has("cinnabar") && project.hasProperty("minecraft.vulkan")) {
        handleApiDep(versionedCatalog["cinnabar"])
    }

    if (versionedCatalog.has("vulkanmod")) {
        "modCompileOnly"(versionedCatalog["vulkanmod"]) { isTransitive = false }
        compileOnly(versionedCatalog["lwjgl-vulkan"])
    }

    if (versionedCatalog.has("skycubed")) {
        val mcVersion = stonecutter.current.version
        "modCompileOnly"(versionedCatalog["skycubed"]) { isTransitive = false }
        compileOnly(versionedCatalog["meowdding-lib"]) {
            isTransitive = false
            capabilities { requireCapability("me.owdding.meowdding-lib:meowdding-lib-$mcVersion") }
        }
        compileOnly(versionedCatalog["skyblock-api"]) {
            isTransitive = false
            capabilities { requireCapability("tech.thatgravyboat:skyblock-api-$mcVersion") }
        }
    }

    //if (mcData.isFabric) {
    //    provideFabricApiDependency(tripleVersion).forEach {
    //        @Suppress("USELESS_CAST")
    //        maybeModApi(if (it.dep is String) it.dep as String else "${(it.dep as ExternalModuleDependency).group}:${(it.dep as ExternalModuleDependency).name}:${(it.dep as ExternalModuleDependency).version}") {
    //            isTransitive = false
    //        }
    //    }
    //}
    "annotationProcessor"(versionedCatalog["mixin.squared"])

    if (enableMoulRelocatorKsp) {
        "ksp"(rootProject.project(":modules:relocator"))
    }

    for (project in rootProject.project(":modules").subprojects) {
        if ("relocator" in project.path) {
            "compileOnly"(project(project.path))
        } else if ("dependencies" !in project.path) {
            "api"(project(project.path)) {
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
        }/*
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

if (enableMoulRelocatorKsp) {
    extensions.configure<com.google.devtools.ksp.gradle.KspExtension> {
        arg("relocator.mcVersion", stonecutter.current.version)
    }
} else {
    tasks.matching { it.name.startsWith("ksp") }.configureEach {
        enabled = false
    }
}

group = rootProject.group
version = rootProject.version

afterEvaluate {
    configure<PublishingExtension> {
        repositories {
            listOf("releases", "snapshots").forEach { type ->
                maven {
                    name = type
                    url = uri("https://repo.polyfrost.org/$type")
                    credentials {
                        username = providers.gradleProperty("polyfrostRepoUsername").orNull
                        password = providers.gradleProperty("polyfrostRepoToken").orNull
                    }
                    authentication { create<BasicAuthentication>("basic") }
                }
            }
        }

        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                groupId = rootProject.group.toString()
                artifactId = project.name
            }
        }
    }

    configure<SigningExtension> {
        isRequired = project.properties["signing.keyId"] != null
        if (isRequired) {
            sign(extensions.getByType<PublishingExtension>().publications["mavenJava"])
        }
    }
}

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

    val mixinCompat = if (stonecutter.eval(stonecutter.current.version, ">= 26.1")) "JAVA_25" else "JAVA_21"
    this.inputs.property("mixin_compat", mixinCompat)
    this.filesMatching("mixins.oneconfigv1*.json") {
        filter { line -> line.replace("\"JAVA_21\"", "\"$mixinCompat\"") }
    }
}

val minJavaVersion = 21
val javaVersion = if (stonecutter.eval(stonecutter.current.version, ">= 26.1")) {
    25
} else {
    21
}

configure<JavaPluginExtension> {
    targetCompatibility = JavaVersion.toVersion(javaVersion)
    sourceCompatibility = JavaVersion.toVersion(minJavaVersion)
}

configure<KotlinJvmExtension> {
    jvmToolchain(javaVersion)
}

tasks.withType<Jar>().configureEach {
    exclude("**/*_Test.class")
    exclude("**/*_Test\$*.class")
}
