// These inspections trigger because we're using the library versions that
// are bundled with Minecraft, which are not the latest versions.
@file:Suppress("GradlePackageUpdate", "VulnerableLibrariesLocal", "DSL_SCOPE_VIOLATION")

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinx.abi)
    alias(libs.plugins.pgt.defaults.repo)
    alias(libs.plugins.blossom)
    id("maven-publish")
    id("signing")
    java
}

val modMajor = project.properties["mod_major_version"]
val modMinor = project.properties["mod_minor_version"]

version = "$modMajor$modMinor"
group = "org.polyfrost"

java {
    withSourcesJar()
}

tasks {
    withType(Jar::class.java) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        if (!name.contains("sourcesjar", ignoreCase = true) || !name.contains("dokka", ignoreCase = true)) {
            exclude("**/**_Test.**")
            exclude("**/**_Test$**.**")
        }
    }
    named<Jar>("sourcesJar") {
        exclude("**/internal/**")
        archiveClassifier.set("sources")
        doFirst {
            archiveClassifier.set("sources")
        }
    }
}

apiValidation {
    ignoredPackages.add("org.lwjgl")
    ignoredPackages.add("org.polyfrost.oneconfig.libs")
    ignoredPackages.add("org.polyfrost.oneconfig.internal")
    ignoredPackages.add("org.polyfrost.oneconfig.test")
}