package org.polyfrost.oneconfig.internal.ui.compose

import com.google.gson.JsonObject
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipInputStream
import kotlin.jvm.optionals.getOrNull
import net.minecraft.client.Minecraft
import net.minecraft.server.packs.resources.ResourceManager
import org.polyfrost.compose.mc.McFontQueue
import org.polyfrost.compose.mc.McGlyph
import org.polyfrost.compose.mc.McGlyphs
import org.polyfrost.compose.render.FontManager
import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.event.v1.events.ResourceFinishedLoading
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.utils.v1.JsonUtils
import org.polyfrost.oneconfig.utils.v1.Multithreading
import org.slf4j.LoggerFactory
//? if > 1.8.9 {
import net.minecraft.resources.Identifier
//?} else {
/*import net.ornithemc.osl.core.api.util.NamespacedIdentifiers
*///?}

// below 1.21.11 the tessellator can't draw with the game's font, so its providers are read from the active resources instead
internal object McResourceFont {
    private val LOGGER = LoggerFactory.getLogger("OneConfig/McResourceFont")

    //~ if = 1.8.9 'minecraft:default' -> 'oneconfig:legacy_default'
    private const val ROOT_FONT = "minecraft:default"
    private const val OPTION_UNIFORM = 1
    private const val OPTION_JP = 2

    private class Unihex(val rows: IntArray, val left: Int, val right: Int)

    private class Font(private val glyphs: Map<Int, McGlyph>, private val unihex: Map<Int, Unihex>) {
        private val baked = ConcurrentHashMap<Int, McGlyph>()

        fun glyph(cp: Int): McGlyph? = glyphs[cp] ?: unihex[cp]?.let { u -> baked.getOrPut(cp) { bake(u) } }
    }

    @Volatile private var font: Font? = null
    @Volatile private var requested = -1
    private val generation = AtomicInteger()

    fun init() {
        McGlyphs.source = { cp -> font?.glyph(cp) }
        McFontQueue.measureWidth = McGlyphs::width
        EventManager.register(ResourceFinishedLoading::class.java, Runnable(::reload))
        // vanilla applies font option changes without a resource reload
        EventManager.register(TickEvent.End::class.java, Runnable { if (requested != fontOptions()) reload() })
    }

    private fun reload() {
        val options = fontOptions()
        requested = options
        val gen = generation.incrementAndGet()
        //~ if = 1.8.9 'Minecraft.getInstance().resourceManager' -> 'ResourceManager.client()'
        val rm = Minecraft.getInstance().resourceManager
        Multithreading.submit {
            val loaded = runCatching { load(rm, options) }.onFailure { LOGGER.warn("Failed to load the game font", it) }.getOrNull()
            Minecraft.getInstance().schedule(Runnable {
                if (gen != generation.get()) return@Runnable
                font = loaded
                FontManager.invalidateMetrics()
                HudManager.invalidate()
            })
        }
    }

    private fun fontOptions(): Int {
        val options = Minecraft.getInstance().options
        return (if (options.forceUnicodeFont().get()) OPTION_UNIFORM else 0) or (if (options.japaneseGlyphVariants().get()) OPTION_JP else 0)
    }

    private fun load(rm: ResourceManager, options: Int): Font? {
        val providers = ArrayList<JsonObject>()
        collectProviders(rm, ROOT_FONT, providers, HashSet(), options)
        val glyphs = HashMap<Int, McGlyph>()
        val unihex = HashMap<Int, Unihex>()
        // like vanilla, the first provider with a codepoint wins
        fun unclaimed(cp: Int) = cp !in glyphs && cp !in unihex
        for (provider in providers) {
            val type = provider.get("type")?.asString
            runCatching {
                when (type) {
                    "space" -> provider.getAsJsonObject("advances")?.entrySet()?.forEach { (key, advance) ->
                        if (key.isEmpty()) return@forEach
                        val cp = key.codePointAt(0)
                        if (unclaimed(cp)) glyphs[cp] = McGlyph(null, 0f, 0f, 0f, 0f, 0f, 0f, 0f, advance.asFloat, 1f)
                    }
                    "bitmap" -> loadBitmap(rm, provider).forEach { (cp, g) -> if (unclaimed(cp)) glyphs[cp] = g }
                    "unihex" -> loadUnihex(rm, provider).forEach { (cp, u) -> if (unclaimed(cp)) unihex[cp] = u }
                }
            }.onFailure { LOGGER.warn("Failed to load {} font provider, skipping", type, it) }
        }
        return if (glyphs.isEmpty() && unihex.isEmpty()) null else Font(glyphs, unihex)
    }

    private fun collectProviders(rm: ResourceManager, ref: String, out: MutableList<JsonObject>, visited: MutableSet<String>, options: Int) {
        if (!visited.add(ref)) return
        val stack = runCatching { rm.getResourceStack(id(ref, "font/", ".json")) }.getOrDefault(emptyList())
        // vanilla lets the highest-priority pack's providers go first
        for (resource in stack.asReversed()) {
            val root = runCatching { resource.open().use { JsonUtils.parseOrNull(String(it.readBytes(), Charsets.UTF_8)) } }.getOrNull()
            val providers = (root as? JsonObject)?.getAsJsonArray("providers") ?: continue
            for (element in providers) {
                val provider = element as? JsonObject ?: continue
                if (!filterPasses(provider, options)) continue
                if (provider.get("type")?.asString == "reference") provider.get("id")?.asString?.let { collectProviders(rm, it, out, visited, options) }
                else out += provider
            }
        }
    }

    private fun filterPasses(provider: JsonObject, options: Int): Boolean {
        val filter = provider.getAsJsonObject("filter") ?: return true
        filter.get("uniform")?.let { if (it.asBoolean != (options and OPTION_UNIFORM != 0)) return false }
        filter.get("jp")?.let { if (it.asBoolean != (options and OPTION_JP != 0)) return false }
        return true
    }

    private fun loadBitmap(rm: ResourceManager, provider: JsonObject): Map<Int, McGlyph> {
        val file = provider.get("file")?.asString ?: return emptyMap()
        val image = read(rm, file, "textures/")?.let(PolyBitmap::decode) ?: return emptyMap()
        val rows = provider.getAsJsonArray("chars")?.map { it.asString.codePoints().toArray() } ?: return emptyMap()
        val cols = rows.firstOrNull()?.size ?: 0
        if (cols == 0 || rows.any { it.size != cols }) return emptyMap()
        val cellW = image.width / cols
        val cellH = image.height / rows.size
        if (cellW == 0 || cellH == 0) return emptyMap()
        val ascent = provider.get("ascent")?.asInt ?: 7
        val scale = (provider.get("height")?.asInt ?: 8).toFloat() / cellH
        val out = HashMap<Int, McGlyph>()
        rows.forEachIndexed { r, cps ->
            cps.forEachIndexed { c, cp ->
                if (cp == 0 || cp in out) return@forEachIndexed
                val x = c * cellW
                val y = r * cellH
                val w = (cellW downTo 1).firstOrNull { px -> (y until y + cellH).any { py -> image.pixels[(py * image.width + x + px - 1) * 4 + 3].toInt() != 0 } } ?: 0
                out[cp] = McGlyph(
                    image.takeIf { w > 0 }, x / image.width.toFloat(), y / image.height.toFloat(), (x + w) / image.width.toFloat(), (y + cellH) / image.height.toFloat(),
                    (7 - ascent).toFloat(), w * scale, cellH * scale, Math.round(w * scale) + 1f, 1f,
                )
            }
        }
        return out
    }

    private fun loadUnihex(rm: ResourceManager, provider: JsonObject): Map<Int, Unihex> {
        val file = provider.get("hex_file")?.asString ?: return emptyMap()
        val bytes = read(rm, file, "") ?: return emptyMap<Int, Unihex>().also { LOGGER.warn("unihex font provider references missing file {}", file) }
        val overrides = provider.getAsJsonArray("size_overrides")?.mapNotNull { element ->
            runCatching {
                val o = element.asJsonObject
                intArrayOf(o.get("from").asString.codePointAt(0), o.get("to").asString.codePointAt(0), o.get("left").asInt, o.get("right").asInt)
            }.getOrNull()
        }.orEmpty()
        val out = HashMap<Int, Unihex>()
        var skipped = 0
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.name.endsWith(".hex")) continue
                for (line in String(zip.readBytes(), Charsets.US_ASCII).lineSequence()) {
                    if (line.isNotEmpty() && !parseHex(line, overrides, out)) skipped++
                }
            }
        }
        if (skipped > 0) LOGGER.warn("Skipped {} malformed unihex entries in {}", skipped, file)
        return out
    }

    // "<codepoint>:<16 rows of 2, 4, 6 or 8 hex digits>", rows stored left-aligned
    private fun parseHex(line: String, overrides: List<IntArray>, out: MutableMap<Int, Unihex>): Boolean {
        val colon = line.indexOf(':')
        val perRow = (line.length - colon - 1) / 16
        if (colon !in 4..6 || perRow !in 2..8 step 2 || line.length - colon - 1 != perRow * 16) return false
        val cp = line.substring(0, colon).toIntOrNull(16) ?: return false
        val rows = IntArray(16)
        var mask = 0
        var p = colon + 1
        for (r in 0 until 16) {
            var v = 0
            repeat(perRow) {
                val d = Character.digit(line[p++], 16)
                if (d < 0) return false
                v = (v shl 4) or d
            }
            rows[r] = v shl (32 - perRow * 4)
            mask = mask or rows[r]
        }
        // vanilla applies the first matching override
        val o = overrides.firstOrNull { cp in it[0]..it[1] }
        out[cp] = when {
            o != null -> Unihex(rows, o[2], o[3])
            mask == 0 -> Unihex(rows, 0, perRow * 4)
            else -> Unihex(rows, Integer.numberOfLeadingZeros(mask), 31 - Integer.numberOfTrailingZeros(mask))
        }
        return true
    }

    private fun bake(u: Unihex): McGlyph {
        val w = u.right - u.left + 1
        val bitmap = if (w <= 0 || u.rows.all { it == 0 }) null else PolyBitmap(w, 16, ByteArray(w * 16 * 4).also { px ->
            for (y in 0 until 16) for (x in 0 until w) {
                val bit = u.left + x
                if (bit in 0..31 && (u.rows[y] ushr (31 - bit)) and 1 != 0) px.fill(-1, (y * w + x) * 4, (y * w + x + 1) * 4)
            }
        })
        // unihex glyphs are 16px tall drawn at half size
        return McGlyph(bitmap, 0f, 0f, 1f, 1f, 0f, w / 2f, 8f, (w / 2 + 1).toFloat(), 0.5f)
    }

    private fun read(rm: ResourceManager, ref: String, folder: String): ByteArray? =
        runCatching { rm.getResource(id(ref, folder)).getOrNull()?.open()?.use { it.readBytes() } }.getOrNull()

    private fun id(ref: String, folder: String, suffix: String = "") =
        //~ if = 1.8.9 'Identifier.fromNamespaceAndPath' -> 'NamespacedIdentifiers.from'
        Identifier.fromNamespaceAndPath(ref.substringBefore(':', "minecraft"), folder + ref.substringAfter(':') + suffix)
}
