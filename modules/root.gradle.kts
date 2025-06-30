@file:Suppress("UnstableApiUsage")

// Shared build logic between all OneConfig modules to reduce boilerplate.

plugins {
    alias(libs.plugins.kotlinx.api.validator)
    alias(libs.plugins.jetbrains.idea.ext)
    id("maven-publish")
    id("signing")
}

val rootModuleProject = project

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = rootProject.libs.plugins.dgt.java.get().pluginId)
    apply(plugin = rootProject.libs.plugins.dgt.kotlin.get().pluginId)
    apply(plugin = rootProject.libs.plugins.dgt.publishing.maven.get().pluginId)
    apply(plugin = "jvm-test-suite")

    if (project.parent?.name == "dependencies") {
        this.group = "${project.group}.dependencies"
    }

    repositories {
        maven("https://repo.polyfrost.org/releases")
        maven("https://repo.polyfrost.org/snapshots")
        maven("https://maven.deftu.dev/releases")
    }

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
        if("dependencies" !in project.path) {
            withJavadocJar()
            withSourcesJar()
        }

        toolchain {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
    }

    afterEvaluate {
        publishing {
            publications {
                named<MavenPublication>("mavenJava") {
                    artifactId = project.name
                    groupId = project.group.toString()

                    signing {
                        isRequired = project.properties["signing.keyId"] != null
                        sign(this@named)
                    }
                }
            }
        }
    }
}

apiValidation {
    for (project in subprojects) {
        ignoredPackages.add("org.polyfrost.oneconfig.api.${project.name}.v1.internal")
    }
    ignoredPackages.add("org.polyfrost.oneconfig.api.hypixel.v1.internal")
    ignoredProjects.add("internal")
    ignoredProjects.add("dependencies")
    ignoredProjects.add("agnostic")
    ignoredProjects.add("legacy")
}
