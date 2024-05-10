import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName

// contains shared logic between all the modules to reduce boilerplate.

plugins {
    java
    id(libs.plugins.kotlinx.api.validator.get().pluginId)
    signing
    `maven-publish`
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "kotlin")

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://repo.polyfrost.org/releases")
    }

    dependencies {
        implementation(rootProject.libs.annotations)
        compileOnly(rootProject.libs.logging.api)
        testImplementation(rootProject.libs.bundles.test.core)
        testImplementation(platform(rootProject.libs.junit.bom))
    }

    tasks.test {
        useJUnitPlatform()
        // run tests with java 17 because it is better for compatability and makes the debugger work
        // (especially for testing of reflection due to the tighter rules)
        javaLauncher = javaToolchains.launcherFor {
            languageVersion = JavaLanguageVersion.of(17)
        }
    }

    tasks.javadoc {
        options {
            (this as CoreJavadocOptions).addBooleanOption("Xdoclint:none", true)
        }
    }

    base.archivesName = "${project.name}-api"
    version = rootProject.version
    group = rootProject.group

    java {
        withSourcesJar()
        withJavadocJar()
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
    }
}

signing {
    sign(publishing.publications)
}

publishing {
    publications {
        for(project in subprojects) {
            if (project.name == "ui-impl") return@publications
            register<MavenPublication>(project.name) {
                groupId = rootProject.group.toString()
                artifactId = project.archivesName.get()
                version = rootProject.version.toString()

                artifact(project.tasks["jar"])
                artifact(project.tasks["sourcesJar"])
                artifact(project.tasks["javadocJar"])
            }
        }
    }

    repositories {
        maven {
            name = "releases"
            url = uri("https://repo.polyfrost.org/releases")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
        maven {
            name = "snapshots"
            url = uri("https://repo.polyfrost.org/snapshots")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
        maven {
            name = "private"
            url = uri("https://repo.polyfrost.org/private")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

apiValidation {
    ignoredPackages.add("org.polyfrost.oneconfig.internal")
    ignoredProjects.add("ui-impl")
}
