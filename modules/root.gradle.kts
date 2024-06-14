// Shared build logic between all OneConfig modules to reduce boilerplate.

plugins {
    id(libs.plugins.kotlinx.api.validator.get().pluginId)
    `java-library`
    `maven-publish`
    signing
    id(libs.plugins.shadow.get().pluginId)
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "java-library")

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://repo.polyfrost.org/releases")
        maven("https://repo.hypixel.net/repository/Hypixel")
    }

    dependencies {
        implementation(rootProject.libs.annotations)
        compileOnly(rootProject.libs.logging.api)
        testImplementation(rootProject.libs.bundles.test.core)
        testImplementation(platform(rootProject.libs.junit.bom))
    }

    tasks {
        test {
            useJUnitPlatform()
            // run tests with java 17 because it is better for compatability and makes the debugger work
            // (especially for testing of reflection due to the tighter rules)
            javaLauncher = this@subprojects.javaToolchains.launcherFor {
                languageVersion = JavaLanguageVersion.of(17)
            }
        }

        javadoc {
            options {
                (this as CoreJavadocOptions).addBooleanOption("Xdoclint:none", true)
            }
        }
    }

    base.archivesName = name
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

//signing {
//    sign(publishing.publications)
//}

publishing {
    publications {
        for (project in subprojects) {
            if (project.name == "internal") continue
            val hasParent = project.parent != null && project.parent != getProject()
            val projectName = if (hasParent) {
                project.parent!!.name + "_" + project.name
            } else {
                project.name
            }
            if (project.plugins.hasPlugin(libs.plugins.shadow.get().pluginId)) {
                shadow {
                    component(create<MavenPublication>(projectName) {
                        groupId = rootProject.group.toString()
                        artifactId = project.base.archivesName.get()
                        version = rootProject.version.toString()

                        from(project.components["java"])
                    })
                }
            } else {
                register<MavenPublication>(projectName) {
                    groupId = rootProject.group.toString()
                    artifactId = project.base.archivesName.get()
                    version = rootProject.version.toString()

                    from(project.components["java"])
                }
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
    for (project in subprojects) {
        ignoredPackages.add("org.polyfrost.oneconfig.api.${project.name}.v1.internal")
    }
    ignoredPackages.add("org.polyfrost.oneconfig.api.hypixel.v0.internal")
    ignoredProjects.add("internal")
	ignoredProjects.add("dependencies")
	ignoredProjects.add("legacy")
	ignoredProjects.add("modern")
    ignoredProjects.add("bundled")
}
