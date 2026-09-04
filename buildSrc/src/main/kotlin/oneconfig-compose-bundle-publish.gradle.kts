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

            minecraftVersions.add("1.8.9")

            requires("fabric-language-kotlin")
        }
    }
}
