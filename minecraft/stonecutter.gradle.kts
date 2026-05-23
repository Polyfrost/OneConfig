import org.jetbrains.kotlin.gradle.plugin.extraProperties

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
            this["moul_compat"] = false
            this["yacl_compat"] = false
            this["modmenu_compat"] = false
        }
    }
}