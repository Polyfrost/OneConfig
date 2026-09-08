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
    file = tasks.named<Jar>("jar").flatMap { it.archiveFile }

    displayName = "Compose Multiplatform ${project.version}"
    changelog = "Compose Multiplatform ${project.version}"
    version = "v${project.version}"
    type = STABLE

    modLoaders.add("fabric")

    dryRun = modrinthId == null || modrinthToken == null

    if (modrinthId != null) {
        modrinth {
            projectId = modrinthId
            accessToken = modrinthToken.orEmpty()

            minecraftVersions.addAll(listOf(
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
                "26.2"
            ))

            requires("fabric-language-kotlin")
        }
    }
}
