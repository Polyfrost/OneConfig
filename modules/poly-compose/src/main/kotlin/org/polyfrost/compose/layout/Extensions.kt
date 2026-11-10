package org.polyfrost.compose.layout

public val Float.px: PolySize.Px get() = PolySize.Px(this)
public val Int.px: PolySize.Px get() = PolySize.Px(this.toFloat())
public val Double.px: PolySize.Px get() = PolySize.Px(this.toFloat())

public val Float.pct: PolySize.Percent get() = PolySize.Percent(this / 100f)
public val Int.pct: PolySize.Percent get() = PolySize.Percent(this / 100f)
public val Double.pct: PolySize.Percent get() = PolySize.Percent(this.toFloat() / 100f)

public fun insets(all: Float): PolyInsets = PolyInsets(all, all, all, all)
public fun insets(horizontal: Float = 0f, vertical: Float = 0f): PolyInsets = PolyInsets(horizontal, vertical, horizontal, vertical)
