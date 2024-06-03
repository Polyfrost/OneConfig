import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

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
    create("bundle", ShadowJar::class.java) {
        configurations = listOf(shade)
    }

    build {
        dependsOn("bundle")
    }
}