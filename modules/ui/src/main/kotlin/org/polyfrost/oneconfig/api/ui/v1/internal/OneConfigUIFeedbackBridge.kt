package org.polyfrost.oneconfig.api.ui.v1.internal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.InternalOneConfigUiApi

@ExperimentalOneConfigUiApi
@InternalOneConfigUiApi
public interface OneConfigUIFeedbackBridge {
    @Composable
    public fun NotificationsCenter(modifier: Modifier)
}

@ExperimentalOneConfigUiApi
private val loadedFeedbackBridge: OneConfigUIFeedbackBridge? by lazy {
    loadBridge(OneConfigUIFeedbackBridge::class.java, "the notification centre panel stays empty")
}

@ExperimentalOneConfigUiApi
internal fun feedbackBridge(): OneConfigUIFeedbackBridge? = loadedFeedbackBridge
