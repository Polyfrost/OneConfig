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
    public val keyF1: Int
    public val keySlash: Int
    public val keyLeftShift: Int
    public val keyRightShift: Int
    public val keyLeftControl: Int
    public val keyRightControl: Int
    public val keyLeftAlt: Int
    public val keyRightAlt: Int
    public val keyLeftSuper: Int
    public val keyRightSuper: Int

    public val mouseButtonLeft: Int

    public val keyB: Int
    public val keyG: Int
    public val keyI: Int
    public val keyJ: Int
    public val keyK: Int
    public val keyM: Int
    public val keyN: Int
    public val keyO: Int
    public val keyP: Int
    public val keyQ: Int
    public val keyS: Int
    public val keyT: Int
    public val keyU: Int
    public val keyW: Int
    public val key0: Int
    public val key1: Int
    public val key2: Int
    public val key3: Int
    public val key4: Int
    public val key5: Int
    public val key6: Int
    public val key7: Int
    public val key8: Int
    public val key9: Int
    public val keyF2: Int
    public val keyF3: Int
    public val keyF4: Int
    public val keyF5: Int
    public val keyF6: Int
    public val keyF7: Int
    public val keyF8: Int
    public val keyF9: Int
    public val keyF10: Int
    public val keyF11: Int
    public val keyF12: Int
    public val keyEscape: Int
    public val keyEnter: Int
    public val keyTab: Int
    public val keySpace: Int
    public val keyBackspace: Int
    public val keyInsert: Int
    public val keyHome: Int
    public val keyEnd: Int
    public val keyPageUp: Int
    public val keyPageDown: Int
    public val keyUp: Int
    public val keyDown: Int
    public val keyLeft: Int
    public val keyRight: Int

    public val mouseButtonRight: Int
    public val mouseButtonMiddle: Int

    public fun keyFromNative(key: Int): Int
    public fun keyToNative(key: Int): Int
    public fun mouseFromNative(button: Int): Int
    public fun mouseToNative(button: Int): Int

}