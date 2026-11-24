@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.page

import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
public sealed interface OneConfigDestination {
    @ExperimentalOneConfigUiApi
    public data class Page(public val id: String) : OneConfigDestination

    @ExperimentalOneConfigUiApi
    public data class ModConfig(public val target: ModConfigTarget) : OneConfigDestination

    @ExperimentalOneConfigUiApi
    public data class Custom(public val page: OneConfigPage) : OneConfigDestination
}
