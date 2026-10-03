plugins {
    id("me.modmuss50.mod-publish-plugin")
}

val modrinthId = findProperty("publish.modrinth.compose-bundle")
    ?.toString()
    ?.takeIf { it.isNotBlank() }
val modrinthToken = findProperty("modrinth.token")
    ?.toString()
    ?.takeIf { it.isNotBlank() }

publishMods {
    displayName = "Compose Multiplatform ${project.version}"
    changelog = "Compose Multiplatform ${project.version}"
    version = "v${project.version}"
    type = STABLE

    dryRun = modrinthId == null || modrinthToken == null

    if (modrinthId != null) {
        val modrinthOptions = modrinthOptions {
            projectId = modrinthId
            accessToken = modrinthToken.orEmpty()
            requires("fabric-language-kotlin")
        }

        modrinth("modrinthFabric") {
            from(modrinthOptions)
            file = tasks.named<Jar>("jar").flatMap { it.archiveFile }
            modLoaders.add("fabric")
            minecraftVersions.addAll(
                "1.21",
                "1.21.1",
                "1.21.2",
                "1.21.3",
                "1.21.4",
                "1.21.5",
                "1.21.6",
                "1.21.7",
                "1.21.8",
                "1.21.9",
                "1.21.10",
                "1.21.11",
                "26.1",
                "26.1.1",
                "26.1.2",
                "26.2",
                "26.3"
            )
        }

        // this is identical to the Fabric version, but has to be published separately,
        // because Modrinth's API rejects a version claiming to support Fabric on 1.8.9
        modrinth("modrinthOrnithe") {
            from(modrinthOptions)
            file = tasks.named<Zip>("ornitheJar").flatMap { it.archiveFile }
            modLoaders.add("ornithe")
            minecraftVersions.add("1.8.9")
        }
    }
}
