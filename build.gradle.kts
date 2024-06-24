plugins {
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.pgt.defaults.repo) apply false
    alias(libs.plugins.licenser)
}

val major = project.properties["version_major"]
val minor = project.properties["version_minor"]
val patch = project.properties["version_patch"]

version = "$major.$minor.$patch"
group = properties["group"].toString()

subprojects {
    version = rootProject.version
    group = "${rootProject.group}.${rootProject.name}"

    apply(plugin = rootProject.libs.plugins.licenser.get().pluginId)
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
    }

    license {
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
