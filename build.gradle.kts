import org.quiltmc.gradle.licenser.extension.QuiltLicenserGradleExtension

plugins {
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.pgt.defaults.repo) apply false
    alias(libs.plugins.licenser) apply false
    alias(libs.plugins.jetbrains.idea.ext)
}

val major = project.properties["version_major"]
val minor = project.properties["version_minor"]
val patch = project.properties["version_patch"]

version = "$major.$minor.$patch"
group = properties["group"] as String

subprojects {
    version = rootProject.version
    group = "${rootProject.group}.${rootProject.properties["mod_id"] as String}"

    apply(plugin = rootProject.libs.plugins.licenser.get().pluginId)
    // Note for future devs: DON'T apply the java-library plugin to subprojects here.
    // This will cause loom to completely break apart and throw itself into oblivion.
    // I have no idea how to fix it, and honestly, I don't want to know.
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    afterEvaluate { // ew.
        if (project.pluginManager.hasPlugin("java")) {
            configure<JavaPluginExtension> {
                withSourcesJar()
            }
        }
    }

    configure<QuiltLicenserGradleExtension> {
        rule("${rootProject.rootDir}/FILEHEADER")
        include("**/*.kt")
        include("**/*.java")
    }

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
