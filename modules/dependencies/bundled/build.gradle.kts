dependencies {
    for (project in rootProject.project(":modules").subprojects) {
        if ("dependencies" !in project.path) {
            api(project(project.path)) {
                isTransitive = false
            }
        }
    }
}