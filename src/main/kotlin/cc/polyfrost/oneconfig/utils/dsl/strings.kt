package cc.polyfrost.oneconfig.utils.dsl

import cc.polyfrost.oneconfig.utils.StringUtils

fun String.substringSafe(startIndex: Int, endIndex: Int): String {
    return StringUtils.substringSafe(this, startIndex, endIndex)
}

fun String.substringSafe(startIndex: Int): String {
    return StringUtils.substringSafe(this, startIndex)
}

fun String.isValidSequence(startIndex: Int, endIndex: Int): Boolean {
    return StringUtils.isValidSequence(this, startIndex, endIndex)
}

fun String.nullToEmpty(): String {
    return StringUtils.nullToEmpty(this)
}

fun String.addStringAt(index: Int, string: String): String {
    return StringUtils.addStringAt(this, index, string)
}

fun String.substringIf(startIndex: Int, endIndex: Int, condition: Boolean): String {
    return StringUtils.substringIf(this, startIndex, endIndex, condition)
}

fun String.substringToLastIndexOf(string: String): String {
    return StringUtils.substringToLastIndexOf(this, string)
}

fun String.substringTo(to: Int): String {
    return StringUtils.substringTo(this, to)
}

fun String.substringTo(String: String): String {
    return StringUtils.substringTo(this, String)
}

fun String.substringOrDont(startIndex: Int, endIndex: Int): String {
    return StringUtils.substringOrDont(this, startIndex, endIndex)
}

fun String.substringOrElse(startIndex: Int, endIndex: Int, elseString: String): String {
    return StringUtils.substringOrElse(this, startIndex, endIndex, elseString)
}

fun String.substringOrEmpty(startIndex: Int, endIndex: Int): String {
    return StringUtils.substringOrEmpty(this, startIndex, endIndex)
}
