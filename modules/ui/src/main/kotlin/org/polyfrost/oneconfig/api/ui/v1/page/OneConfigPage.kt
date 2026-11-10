package org.polyfrost.oneconfig.api.ui.v1.page

import androidx.compose.runtime.Composable
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
public abstract class OneConfigPage(public val id: String, public val title: PageText, public val icon: String, public val group: PageGroup) {
    init {
        OneConfigPages.requireNamespacedId(id)
    }

    public open val order: Int get() = 0

    public open val showInSidebar: Boolean get() = true

    public open val searchPlaceholder: PageText? get() = null

    @Composable
    public abstract fun Content()

    override fun toString(): String = "OneConfigPage(id=$id)"
}
