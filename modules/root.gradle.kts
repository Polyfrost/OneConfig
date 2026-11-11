@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("maven-publish")
    id("signing")
}

val rootModuleProject = project

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "kotlin")
    apply(plugin = "jvm-test-suite")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "oneconfig-lint")

    group = rootProject.group
    version = rootProject.version

    repositories {
        maven("https://repo.polyfrost.org/releases")
        maven("https://repo.polyfrost.org/snapshots")
        google()
    }

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    configurations.configureEach {
        exclude(group = "org.jetbrains.skiko")
        exclude(group = "org.jetbrains.compose.ui", module = "ui-skiko")
        exclude(group = "org.jetbrains.compose.ui", module = "ui-skiko-desktop")
    }

    dependencies {
        "implementation"(rootProject.libs.annotations)
        "compileOnly"(rootProject.libs.logging.api)
        "testImplementation"(rootProject.libs.bundles.test.core)
        "testImplementation"(platform(rootProject.libs.junit.bom))
        // mods compile against mod-toggles alone, so it brings nothing along
        if (name != "mod-toggles") "api"(rootProject.libs.bundles.adventure)
    }

    configure<TestingExtension> {
        suites {
            val sourceSets = extensions.getByType<JavaPluginExtension>().sourceSets
            val test = sourceSets.getByName("test")
            val main = sourceSets.getByName("main")

            fun createTestSuite(name: String, javaVersion: Int) {
                val suite = register<JvmTestSuite>(name) {
                    useJUnitJupiter()

                    dependencies {
                        // otherwise the suite compiles against the ancient version pulled in by the Kotlin stdlib
                        compileOnly(rootProject.libs.annotations)
                    }

                    sources {
                        java { srcDir("src/test/java") }
                        extensions.findByName("kotlin")?.let { (it as SourceDirectorySet).srcDir("src/test/kotlin") }
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
            createTestSuite("j21Tests", 21)
        }
    }

    // tests run via the j21Tests suite because the default test task does not
    // use the JUnit Platform and fails on Gradle 9+
    tasks.named<Test>("test") {
        enabled = false
        dependsOn("j21Tests")
    }

    tasks.withType<Javadoc> {
        options {
            (this as CoreJavadocOptions).addBooleanOption("Xdoclint:none", true)
        }
    }

    base.archivesName = name

    configure<KotlinJvmProjectExtension> {
        // the suite is registered directly rather than as the default test compilation, which is otherwise associated
        target.compilations.matching { it.name == "j21Tests" }.configureEach {
            associateWith(target.compilations.getByName("main"))
        }
    }

    if (name !in listOf("internal", "compose-bundle")) {
        configure<KotlinJvmProjectExtension> {
            explicitApi()
            @OptIn(ExperimentalAbiValidation::class) // no stable alternative as of Kotlin 2.4
            abiValidation {
                filters.exclude {
                    byNames.add("org.polyfrost.oneconfig.api.**.internal.**")
                    annotatedWith.add("org.jetbrains.annotations.ApiStatus.Internal")
                }
            }
        }
        // otherwise the compile finalizer fails the build before updateKotlinAbi gets to run
        tasks.named("checkKotlinAbi") { mustRunAfter("updateKotlinAbi") }
        tasks.withType<JavaCompile>().configureEach { finalizedBy("checkKotlinAbi") }
        tasks.withType<KotlinCompile>().configureEach { finalizedBy("checkKotlinAbi") }
    }

    val kotlinModuleName = project.name
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions.moduleName = kotlinModuleName
        if (name.contains("Test")) {
            // JUnit 6's nullable assertion parameters get misread as non-null
            compilerOptions.freeCompilerArgs.add("-Xjspecify-annotations=ignore")
        }
    }

    configure<JavaPluginExtension> {
        withJavadocJar()
        withSourcesJar()

        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    configure<PublishingExtension> {
        repositories {
            listOf("releases", "snapshots").forEach { type ->
                maven {
                    name = type
                    url = uri("https://repo.polyfrost.org/$type")
                    credentials {
                        username = providers.gradleProperty("polyfrostRepoUsername").orNull
                        password = providers.gradleProperty("polyfrostRepoToken").orNull
                    }
                    authentication { create<BasicAuthentication>("basic") }
                }
            }
        }

        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                artifactId = project.name
                groupId = project.group.toString()
                suppressAllPomMetadataWarnings()
            }
        }
    }

    configure<SigningExtension> {
        isRequired = providers.gradleProperty("signing.keyId").isPresent
        if (isRequired) {
            sign(extensions.getByType<PublishingExtension>().publications["mavenJava"])
        }
    }

    /*
    todo
    afterEvaluate {
        publishing {
            publications {
                named<MavenPublication>("mavenJava") {
                    artifactId = project.name
                    groupId = project.group.toString()
                    suppressAllPomMetadataWarnings()

                    signing {
                        isRequired = providers.gradleProperty("signing.keyId").isPresent
                        sign(this@named)
                    }
                }
            }
        }
    }
     */

    tasks {
        named<Jar>("jar") {
            manifest {
                attributes(mapOf(
                    "Fabric-Loom-Remap" to false
                ))
            }
            archiveBaseName.set(project.name)
        }
    }
}

