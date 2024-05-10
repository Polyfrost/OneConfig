plugins {
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.pgt.defaults.repo) apply false
    alias(libs.plugins.licenser)
}

val modMajor = project.properties["mod_major_version"]
val modMinor = project.properties["mod_minor_version"]

version = "$modMajor$modMinor"
group = "org.polyfrost.oneconfig"

allprojects {
    apply(plugin = rootProject.libs.plugins.licenser.get().pluginId)
    license {
        rule("${rootProject.rootDir}/FILEHEADER")
        include("**/*.kt")
        include("**/*.java")
    }
}
