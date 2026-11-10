package org.polyfrost.oneconfig.api.ui.v1

@Deprecated(
    "Moved to the mod-toggles module",
    ReplaceWith("ModToggle", "org.polyfrost.oneconfig.api.toggle.v1.ModToggle"),
)
public interface ModToggle : org.polyfrost.oneconfig.api.toggle.v1.ModToggle {
    override fun needsSetup(): Boolean = false
}
