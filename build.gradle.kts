plugins {
    alias(libs.plugins.kotlin) apply(false)
    alias(libs.plugins.licenser) apply(false)
    alias(libs.plugins.jetbrains.idea.ext)

    alias(libs.plugins.dgt.base) apply(false)
}

// Note for future devs: DON'T apply the java-library plugin to subprojects here.
// This will cause loom to completely break apart and throw itself into oblivion.
// I have no idea how to fix it, and honestly, I don't want to know.

subprojects {
    pluginManager.withPlugin("java") {
        apply(plugin = rootProject.libs.plugins.licenser.get().pluginId)
        apply(plugin = rootProject.libs.plugins.dgt.base.get().pluginId)
        apply(plugin = rootProject.libs.plugins.dgt.publishing.maven.get().pluginId)
        apply(plugin = "signing")

        configure<PublishingExtension> {
            repositories {
                arrayOf("releases", "snapshots", "private").forEach { type ->
                    maven {
                        name = type
                        url = uri("https://repo.polyfrost.org/$type")
                        credentials(PasswordCredentials::class)
                        authentication { create<BasicAuthentication>("basic") }
                    }
                }
            }
        }
    }
}
