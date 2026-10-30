plugins {
    id("net.fabricmc.fabric-loom-remap")
    id("ploceus")
    `oneconfig-fabric`
}

repositories {
    maven("https://maven.cloverclient.com/releases") {
        content { includeGroup("pl.tomgirl") }
    }
    // Mojang's library repository only has STB natives for the platforms vanilla ships
    exclusiveContent {
        forRepository { mavenCentral() }
        filter { includeModule("org.lwjgl", "lwjgl-stb") }
    }
}

ploceus {
    setIntermediaryGeneration(2)
}

val stbNatives = configurations.create("stbNatives") { isTransitive = false }

dependencies {
    modImplementation(versionedCatalog["fabric-language-kotlin"])
    modImplementation(versionedCatalog["fabric-loader"])

    // ploceus.featherMappings() is itself a layered dependency - nesting it in loom.layered fails on a cold cache
    // since Loom doesn't order the generation of nested layered mappings
    mappings(ploceus.layeredMappings {
        mappings("net.ornithemc:feather-gen2:${versionedCatalog.versions["minecraft"].requiredVersion}+build.${versionedCatalog.versions["feather.build"].requiredVersion}:v2")
        mappings(rootProject.file("mappings/feather-overrides.tiny"))
    })
    ploceus.dependOsl(versionedCatalog.versions["osl"].requiredVersion)

    configurations.configureEach {
        exclude(group = "org.lwjgl.lwjgl")
    }

    modApi(versionedCatalog["pylon"])
    val lwjglStb = "org.lwjgl:lwjgl-stb:3.4.3"
    api(lwjglStb) { isTransitive = false }
    // Pylon doesn't provide lwjgl_stb
    val platforms = listOf("linux", "linux-arm64", "macos", "macos-arm64", "windows", "windows-x86", "windows-arm64")
    for (natives in platforms) {
        stbNatives("$lwjglStb:natives-$natives")
    }

    api("com.mojang:brigadier:1.0.18")
}

// embedded rather than declared: Mojang's repo lacks some STB natives, and consumers resolve the
// whole module from there, so publishing them as dependencies breaks every dependent build
interface Injected {
    @get:Inject val archives: ArchiveOperations
}

tasks.processResources {
    val archives = objects.newInstance<Injected>().archives
    from(stbNatives.elements.map { jars -> jars.map { archives.zipTree(it) } }) {
        exclude("META-INF/MANIFEST.MF", "META-INF/INDEX.LIST", "META-INF/versions/**")
    }
}
