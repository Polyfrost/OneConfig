plugins {
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.kotlinx.abi)
    alias(libs.plugins.pgt.defaults.repo) apply false
    alias(libs.plugins.licenser)
}

val modMajor = project.properties["mod_major_version"]
val modMinor = project.properties["mod_minor_version"]

version = "$modMajor$modMinor"
group = "org.polyfrost"

allprojects {
    apply(plugin = rootProject.libs.plugins.licenser.get().pluginId)
    license {
        rule("${rootProject.rootDir}/FILEHEADER")
        include("**/*.kt")
        include("**/*.java")
    }
}

apiValidation {
    nonPublicMarkers.add("org.polyfrost.oneconfig.api.PlatformDeclaration")
    ignoredProjects.add("OneConfig")
    ignoredProjects.add("ui-impl")
    ignoredPackages.add("org.polyfrost.oneconfig.internal")
    ignoredPackages.add("org.polyfrost.oneconfig.test")
}