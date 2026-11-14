@file:JvmName("HudColors")
@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.hud.v1

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
public fun PolyColor.toComposeColor(): Color = Color(argb)

@ExperimentalOneConfigUiApi
public fun Color.toPolyColor(): PolyColor = PolyColor(toArgb())

@ExperimentalOneConfigUiApi
public fun Int.argbToColor(): Color = Color(this)
