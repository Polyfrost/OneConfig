val shade by configurations.creating {
    configurations.api.get().extendsFrom(this)
}

base.archivesName = "oneconfig-api"

dependencies {
    for (project in rootProject.project(":modules").subprojects) {
        if("dependencies" !in project.path) {
            shade(project(project.path)) {
                isTransitive = false
            }
        }
    }
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        configurations = listOf(shade)
    }
}