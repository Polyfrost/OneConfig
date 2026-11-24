@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.feedbackBridge

@ExperimentalOneConfigUiApi
@Composable
public fun NotificationsCenterPanel(modifier: Modifier = Modifier) {
    feedbackBridge()?.NotificationsCenter(modifier)
}
