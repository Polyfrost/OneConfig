package org.polyfrost.compose.render

private const val TRUETYPE = "\u0000\u0001\u0000\u0000"

// stb_truetype reads font data unchecked, so this rejects damaged fonts (truncated tables, offsets past the end) before
// they reach it; it isn't a full sanitizer, and fonts crafted to pass these checks can still crash it
internal fun isReadableFont(bytes: ByteArray, at: Int = 0): Boolean {
    fun u16(o: Int) = (bytes[o].toInt() and 0xFF shl 8) or (bytes[o + 1].toInt() and 0xFF)
    fun u32(o: Int) = (u16(o).toLong() shl 16) or u16(o + 2).toLong()
    if (at < 0 || bytes.size - at < 12) return false
    when (String(bytes, at, 4, Charsets.ISO_8859_1)) {
        "ttcf" -> return at == 0 && bytes.size >= 16 && u32(12) < bytes.size && isReadableFont(bytes, u32(12).toInt())
        TRUETYPE, "OTTO", "true" -> {}
        else -> return false
    }
    val count = u16(at + 4)
    if (at + 12 + count * 16 > bytes.size) return false
    val offsets = HashMap<String, Int>()
    val lengths = HashMap<String, Long>()
    for (i in 0 until count) {
        val record = at + 12 + i * 16
        val tag = String(bytes, record, 4, Charsets.ISO_8859_1)
        if (u32(record + 8) + u32(record + 12) > bytes.size) return false
        offsets[tag] = u32(record + 8).toInt()
        lengths[tag] = u32(record + 12)
    }
    if (MIN_TABLE_LENGTHS.any { (tag, min) -> (lengths[tag] ?: -1) < min }) return false
    val glyphs = u16(offsets.getValue("maxp") + 4)

    val cmap = offsets.getValue("cmap")
    val cmapLength = lengths.getValue("cmap")
    val encodings = u16(cmap + 2)
    if (4 + 8L * encodings > cmapLength) return false
    for (i in 0 until encodings) {
        val sub = u32(cmap + 4 + i * 8 + 4)
        if (sub + 8 > cmapLength) return false
        val table = cmap + sub.toInt()
        val (length, needed) = when (u16(table)) {
            0 -> u16(table + 2).toLong() to 262L
            4 -> u16(table + 2).toLong() to 16L + 4 * u16(table + 6)
            6 -> u16(table + 2).toLong() to 10L + 2 * u16(table + 8)
            12, 13 -> if (sub + 16 > cmapLength) return false else u32(table + 4) to 16 + 12 * u32(table + 12)
            else -> continue
        }
        if (length < needed || sub + length > cmapLength) return false
    }

    val longMetrics = u16(offsets.getValue("hhea") + 34)
    if (longMetrics == 0 || 4L * longMetrics + 2L * maxOf(0, glyphs - longMetrics) > lengths.getValue("hmtx")) return false

    if ("CFF " in lengths) return true
    val loca = offsets["loca"] ?: return false
    val glyf = lengths["glyf"] ?: return false
    val long = u16(offsets.getValue("head") + 50) != 0
    if ((glyphs + 1L) * (if (long) 4 else 2) > lengths.getValue("loca")) return false
    for (i in 0..glyphs) if ((if (long) u32(loca + i * 4) else u16(loca + i * 2) * 2L) > glyf) return false
    return true
}

private val MIN_TABLE_LENGTHS = mapOf("cmap" to 4L, "head" to 54L, "hhea" to 36L, "hmtx" to 0L, "maxp" to 6L)
