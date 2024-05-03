// contains shared logic between all the modules to reduce boilerplate.

plugins {
    java
    id(libs.plugins.kotlinx.api.validator.get().pluginId)
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

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
    }
}

apiValidation {
    ignoredPackages.add("org.polyfrost.oneconfig.internal")
    ignoredProjects.add("ui-impl")
}
