package org.polyfrost.oneconfig.api.ui.v1.page

import androidx.compose.runtime.Composable
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge

@ExperimentalOneConfigUiApi
val currentPageSearchQuery: String
    @Composable get() = componentsBridge().pageSearchQuery()
