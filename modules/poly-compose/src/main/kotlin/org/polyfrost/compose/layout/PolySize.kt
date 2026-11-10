package org.polyfrost.compose.layout

public sealed class PolySize {

    public object Wrap : PolySize()

    public data class Px(val value: Float) : PolySize()

    public data class Percent(val value: Float) : PolySize()

    public object Fill : PolySize()

}
