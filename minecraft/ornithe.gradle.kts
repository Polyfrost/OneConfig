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

loom {
    accessWidenerPath.set(rootProject.file("minecraft/src/main/resources/oneconfigv1.classtweaker"))
}

dependencies {
    modImplementation(versionedCatalog["fabric-language-kotlin"])
    modImplementation(versionedCatalog["fabric-loader"])

    mappings(loom.layered {
        mappings(ploceus.featherMappings(versionedCatalog.versions["feather.build"].requiredVersion))
        mappings(rootProject.file("mappings/feather-overrides.tiny"))
    })
    ploceus.dependOsl(versionedCatalog.versions["osl"].requiredVersion)

    configurations.configureEach {
        exclude(group = "org.lwjgl.lwjgl")
    }

    modApi(versionedCatalog["lenis"])

    api("com.mojang:brigadier:1.0.18")
}
