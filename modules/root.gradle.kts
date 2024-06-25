@file:Suppress("UnstableApiUsage")

// Shared build logic between all OneConfig modules to reduce boilerplate.

plugins {
    id(libs.plugins.kotlinx.api.validator.get().pluginId)
    `java-library`
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "kotlin")
    apply(plugin = "jvm-test-suite")

    dependencies {
        implementation(rootProject.libs.annotations)
        compileOnly(rootProject.libs.logging.api)
        testImplementation(rootProject.libs.bundles.test.core)
        testImplementation(platform(rootProject.libs.junit.bom))
    }

    configure<TestingExtension> {
        suites {
            val test by sourceSets
            val main by sourceSets

            fun createTestSuite(name: String, javaVersion: Int) {
                val suite = register<JvmTestSuite>(name) {
                    useJUnitJupiter()

                    sources {
                        java { srcDir("src/test/java") }
                        resources { srcDir("src/test/resources") }
                        compileClasspath += test.compileClasspath + main.output
                        runtimeClasspath += test.runtimeClasspath + main.output
                    }

                    targets.all {
                        testTask.configure {
                            javaLauncher = this@subprojects.javaToolchains.launcherFor {
                                languageVersion = JavaLanguageVersion.of(javaVersion)
                            }
                            outputs.upToDateWhen { false }
                        }
                    }
                }
                tasks.named("check") {
                    dependsOn(suite)
                }
            }
//            createTestSuite("j8Tests", 8) // TODO(nextday): re-enable (pretty please)
            createTestSuite("j17Tests", 17)
            createTestSuite("j21Tests", 21)
        }
    }

    tasks.javadoc {
        options {
            (this as CoreJavadocOptions).addBooleanOption("Xdoclint:none", true)
        }
    }

    base.archivesName = name

    java {
        withJavadocJar()

        toolchain {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
    }
}

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
            register<MavenPublication>(projectName) {
                from(project.components["java"])

                groupId = group.toString()
                artifactId = project.base.archivesName.get()
                version = rootProject.version.toString()

                signing {
                    isRequired = project.properties["signing.keyId"] != null
                    sign(this@register)
                }
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
