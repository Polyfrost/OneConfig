package org.polyfrost.oneconfig.api.ui.v1.page

import androidx.compose.runtime.Composable
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
abstract class OneConfigPage(val id: String, val title: PageText, val icon: String, val group: PageGroup) {
    init {
        OneConfigPages.requireNamespacedId(id)
    }

    open val order: Int get() = 0

    open val showInSidebar: Boolean get() = true

    open val searchPlaceholder: PageText? get() = null

    @Composable
    abstract fun Content()

    override fun toString(): String = "OneConfigPage(id=$id)"
}
