plugins {
    id("net.fabricmc.fabric-loom-remap")
    id("ploceus")
    `oneconfig-fabric`
}

repositories {
    maven("https://maven.cloverclient.com/releases") {
        content { includeGroup("pl.tomgirl") }
    }
}

ploceus {
    setIntermediaryGeneration(2)
}

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
        api("$lwjglStb:natives-$natives") { isTransitive = false }
    }

    api("com.mojang:brigadier:1.0.18")
}
