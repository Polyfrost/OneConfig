plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.1-fabric"
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
