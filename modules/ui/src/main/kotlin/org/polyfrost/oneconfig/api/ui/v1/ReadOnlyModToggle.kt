@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1

import org.polyfrost.oneconfig.api.toggle.v1.ModToggle

@ExperimentalOneConfigUiApi
public interface ReadOnlyModToggle : ModToggle {
    public val lockedReason: String?

    override fun setEnabled(enabled: Boolean) {}
}

internal class LambdaReadOnlyModToggle(
    private val enabled: () -> Boolean,
    private val reason: () -> String?,
) : ReadOnlyModToggle {
    override val lockedReason: String? get() = reason()

    override fun isEnabled(): Boolean = enabled()
}
