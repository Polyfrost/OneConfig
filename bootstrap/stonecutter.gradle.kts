plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.1-fabric"
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
