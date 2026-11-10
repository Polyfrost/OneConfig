package org.polyfrost.oneconfig.api.platform.v1

public interface Keys {
    public fun keyName(key: Int): String
    public fun mouseName(button: Int): String

    public val keyA: Int
    public val keyC: Int
    public val keyD: Int
    public val keyE: Int
    public val keyF: Int
    public val keyH: Int
    public val keyL: Int
    public val keyR: Int
    public val keyV: Int
    public val keyX: Int
    public val keyY: Int
    public val keyZ: Int
    public val keyDelete: Int
    public val keyLeftShift: Int
    public val keyRightShift: Int
    public val keyLeftControl: Int
    public val keyRightControl: Int
    public val keyLeftAlt: Int
    public val keyRightAlt: Int
    public val keyLeftSuper: Int
    public val keyRightSuper: Int

    public val mouseButtonLeft: Int

}