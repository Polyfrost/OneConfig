@file:OptIn(ExperimentalOneConfigUiApi::class)
@file:JvmName("HudSuppression")

package org.polyfrost.oneconfig.api.hud.v1

import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
public fun Hud.suppress(otherHudId: String, reason: String): AutoCloseable = HudManager.addSuppression(this, otherHudId, reason)
