plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.2-fabric"
stonecutter tasks {
    order("publishModrinth")
}

tasks.register("assembleAllNodes") {
    group = "build"
    description = "Assembles the production jar of every bootstrap node."
    dependsOn(stonecutter.tasks.named("assemble"))
}

tasks.register("publishMods") {
    group = "publishing"
    description = "Publishes all bootstrap nodes to Modrinth."
    dependsOn(stonecutter.tasks.named("publishMods"))
}

stonecutter {
    parameters {
        constants {
            match(
                current.project.substringAfterLast("-"),
                "fabric",
                "neoforge"
            )
        }
    }
}
