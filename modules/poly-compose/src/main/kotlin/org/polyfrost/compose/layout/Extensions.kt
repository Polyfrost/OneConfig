package org.polyfrost.compose.layout

val Float.px get() = PolySize.Px(this)
val Int.px get() = PolySize.Px(this.toFloat())
val Double.px get() = PolySize.Px(this.toFloat())

val Float.pct get() = PolySize.Percent(this / 100f)
val Int.pct get() = PolySize.Percent(this / 100f)
val Double.pct get() = PolySize.Percent(this.toFloat() / 100f)

fun insets(all: Float) = PolyInsets(all, all, all, all)
fun insets(horizontal: Float = 0f, vertical: Float = 0f) = PolyInsets(horizontal, vertical, horizontal, vertical)
