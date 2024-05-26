plugins {
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.pgt.defaults.repo) apply false
    alias(libs.plugins.licenser)
    `java-library`
}

val major = project.properties["version_major"]
val minor = project.properties["version_minor"]
val patch = project.properties["version_patch"]

version = "$major.$minor.$patch"
group = properties["group"].toString()

allprojects {
    apply(plugin = rootProject.libs.plugins.licenser.get().pluginId)
    license {
        rule("${rootProject.rootDir}/FILEHEADER")
        include("**/*.kt")
        include("**/*.java")
    }
}
