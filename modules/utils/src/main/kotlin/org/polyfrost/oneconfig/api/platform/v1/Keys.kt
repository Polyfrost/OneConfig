package org.polyfrost.oneconfig.api.platform.v1

interface Keys {
    fun keyName(key: Int): String
    fun mouseName(button: Int): String

    val keyA: Int
    val keyC: Int
    val keyD: Int
    val keyE: Int
    val keyH: Int
    val keyL: Int
    val keyR: Int
    val keyV: Int
    val keyX: Int
    val keyDelete: Int
    val keyLeftShift: Int
    val keyRightShift: Int
    val keyLeftControl: Int
    val keyRightControl: Int
    val keyLeftAlt: Int
    val keyRightAlt: Int
    val keyLeftSuper: Int
    val keyRightSuper: Int

    val mouseButtonLeft: Int

}