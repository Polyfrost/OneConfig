import org.jetbrains.kotlin.gradle.plugin.extraProperties

plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.11-fabric"
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
            this["rconfig_compat"] = false
            this["cinnabar"] = false
        }

        replacements {
            string(current.version >= "26.1", "gui_graphics") {
                replace("GuiGraphics", "GuiGraphicsExtractor")
            }
        }
    }
}