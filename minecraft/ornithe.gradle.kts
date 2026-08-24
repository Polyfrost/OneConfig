plugins {
    id("net.fabricmc.fabric-loom-remap")
    id("ploceus")
    `oneconfig-fabric`
}

repositories {
    exclusiveContent {
        forRepository { mavenCentral() }
        filter { includeGroup("org.lwjgl") }
    }

    maven("https://maven.axolotlclient.com/releases") {
        content {
            includeGroup("io.github.moehreag")
            includeGroup("io.github.moehreag.legacy-lwjgl3")
            includeGroup("io.github.moehreag.hypixel")
        }
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

    modApi(versionedCatalog["legacy-lwjgl3"]) {
        exclude(group = "net.ornithemc.osl-gen2")
    }

    api("com.mojang:brigadier:1.0.18")
}
