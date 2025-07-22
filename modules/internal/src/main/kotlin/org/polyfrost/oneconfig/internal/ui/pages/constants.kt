package org.polyfrost.oneconfig.internal.ui.pages

import org.polyfrost.polyui.data.PolyImage
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.utils.image

internal val heart = PolyImage("assets/oneconfig/ico/plus.svg")
internal val defaultModImage = "assets/oneconfig/ico/default_mod.svg".image()
internal val modBoxTopRad = floatArrayOf(12f, 12f, 0f, 0f)
internal val modBoxBotRad = floatArrayOf(0f, 0f, 12f, 12f)
internal val modBoxAlign = Align(line = Align.Line.Start, mode = Align.Mode.Vertical, pad = Vec2.ZERO)
internal val imageAlign = Align(main = Align.Content.Center, pad = Vec2.ZERO)
internal val barAlign = Align(pad = Vec2(14f, 6f), main = Align.Content.Center)
