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

    api("com.mojang:brigadier:1.0.18")
}
