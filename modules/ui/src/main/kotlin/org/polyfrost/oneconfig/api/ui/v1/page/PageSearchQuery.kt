package org.polyfrost.oneconfig.api.ui.v1.page

import androidx.compose.runtime.Composable
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge

@Deprecated("Returns an empty query outside a OneConfig page. Use currentPageSearchQueryOrNull.", ReplaceWith("currentPageSearchQueryOrNull"))
@ExperimentalOneConfigUiApi
public val currentPageSearchQuery: String
    @Composable get() = componentsBridge().pageSearchQuery()

@ExperimentalOneConfigUiApi
public val currentPageSearchQueryOrNull: String?
    @Composable get() = componentsBridge().pageSearchQueryOrNull()
