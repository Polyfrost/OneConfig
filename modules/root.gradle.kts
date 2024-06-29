@file:Suppress("UnstableApiUsage")

import org.gradle.configurationcache.extensions.capitalized


// Shared build logic between all OneConfig modules to reduce boilerplate.

plugins {
    id(libs.plugins.kotlinx.api.validator.get().pluginId)
}

val rootModuleProject = project

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "kotlin")
    apply(plugin = "jvm-test-suite")

    if (project.parent?.name == "dependencies")
        this.group = "${project.group}.dependencies"

    dependencies {
        "implementation"(rootProject.libs.annotations)
        "compileOnly"(rootProject.libs.logging.api)
        "testImplementation"(rootProject.libs.bundles.test.core)
        "testImplementation"(platform(rootProject.libs.junit.bom))
    }

    configure<TestingExtension> {
        suites {
            val sourceSets = extensions.getByType<JavaPluginExtension>().sourceSets
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

                    val toolchainService = this@subprojects.extensions.getByName<JavaToolchainService>("javaToolchains")
                    targets.all {
                        testTask.configure {
                            javaLauncher = toolchainService.launcherFor {
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
            createTestSuite("j8Tests", 8)
            createTestSuite("j17Tests", 17)
            createTestSuite("j21Tests", 21)
        }
    }

    tasks.withType<Javadoc> {
        options {
            (this as CoreJavadocOptions).addBooleanOption("Xdoclint:none", true)
        }
    }

    base.archivesName = name

    configure<JavaPluginExtension> {
        withJavadocJar()

        toolchain {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
    }

    rootModuleProject.publishing {
        publications {
            if (project.name == "internal") return@publications

            register<MavenPublication>("module" + project.name.capitalized()) {
                from(components["java"])

                groupId = project.group.toString()
                artifactId = project.name

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
        ignoredPackages.add("org.polyfrost.oneconfig.api.${rootProject.properties["mod_id"] as String}.v1.internal")
    }
    ignoredPackages.add("org.polyfrost.oneconfig.api.hypixel.v0.internal")
    ignoredProjects.add("internal")
    ignoredProjects.add("dependencies")
    ignoredProjects.add("legacy")
    ignoredProjects.add("modern")
    ignoredProjects.add("bundled")
}
