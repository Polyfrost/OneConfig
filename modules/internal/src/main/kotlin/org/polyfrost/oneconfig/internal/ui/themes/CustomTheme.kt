package org.polyfrost.oneconfig.internal.ui.themes

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.utils.v1.JsonUtils
import org.polyfrost.oneconfig.utils.v1.Multithreading
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.Deflater
import java.util.zip.Inflater

enum class CustomBase { POLYFROSTED, POLYGLASS, MINECRAFT }

enum class CustomCorners { ROUNDED, SOFT, SQUARE }

@Immutable
data class CustomThemeSpec(
    val id: String,
    val name: String,
    val base: CustomBase,
    val light: Boolean,
    val background: Color,
    val surface: Color,
    val text: Color,
    val textSecondary: Color,
    val border: Color,
    val accent: Color?,
    val corners: CustomCorners,
    val glass: Int,
) {
    val themeId: String get() = "custom-$id"

    fun toUITheme(): UITheme {
        val baseTheme = baseTheme(base, light)
        val glassBase = base == CustomBase.POLYGLASS
        val g = glass.coerceIn(0, 100) / 100f
        val shapes = cornerShapes(corners)
        val theme = baseTheme.copy(
            previewImage = themeId,
            name = name,
            pageBackground = background.copy(alpha = if (glassBase) 1f - 0.45f * g else baseTheme.pageBackground.alpha),
            sidebarBackground = lerp(background, surface, 0.25f).copy(alpha = if (glassBase) 1f - 0.4f * g else baseTheme.sidebarBackground.alpha),
            chipBackground = surface.copy(alpha = if (glassBase) 0.7f else baseTheme.chipBackground.alpha),
            modCardBackground = surface.copy(alpha = if (glassBase) 0.35f else baseTheme.modCardBackground.alpha),
            componentBackground = surface.copy(alpha = if (glassBase) 1f - 0.55f * g else baseTheme.componentBackground.alpha),
            popupBackground = surface.copy(alpha = if (glassBase) 1f - 0.2f * g else baseTheme.popupBackground.alpha),
            borderColor = border,
            textColor = text,
            textColorSecondary = textSecondary,
            accentTextColor = accent?.let(::readableOn) ?: baseTheme.accentTextColor,
            controlThumbColor = baseTheme.controlThumbColor,
            backgroundShape = shapes.background,
            sideBarNavigationEntryShape = shapes.small,
            modCardShape = shapes.medium,
            checkBoxShape = shapes.tiny,
            buttonShape = shapes.small,
            popupShape = shapes.medium,
            circleShape = shapes.circle,
        )
        UITheme.copyExtras(baseTheme.previewImage, themeId)
        if (glassBase && glass <= 0) theme.withoutGlassEffects()
        clearPalette(themeId)
        theme.withShapes(shapes.modIcon, shapes.scrollBar)
        accent?.let { theme.withPalette(PaletteInfo(themeId, name, listOf(PaletteAccentOption(name, it)), 0)) }
        return theme.withControlTrackColor(lerp(text, background, 0.5f))
    }

    fun encode(): String {
        val bytes = toJson(false).toString().toByteArray(StandardCharsets.UTF_8)
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(bytes)
        deflater.finish()
        val out = ByteArrayOutputStream()
        val buf = ByteArray(256)
        while (!deflater.finished()) {
            val n = deflater.deflate(buf)
            out.write(buf, 0, n)
        }
        deflater.end()
        return CODE_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray())
    }

    internal fun toJson(withId: Boolean): JsonObject = JsonObject().apply {
        addProperty("v", 1)
        if (withId) addProperty("id", id)
        addProperty("name", name)
        addProperty("base", base.name.lowercase())
        addProperty("light", light)
        add("c", JsonObject().apply {
            addProperty("bg", hex(background))
            addProperty("surface", hex(surface))
            addProperty("text", hex(text))
            addProperty("text2", hex(textSecondary))
            addProperty("border", hex(border))
            accent?.let { addProperty("accent", hex(it)) }
        })
        addProperty("corners", corners.name.lowercase())
        addProperty("glass", glass.coerceIn(0, 100))
    }

    companion object {
        const val MAX = 8
        const val NAME_LIMIT = 24
        const val CODE_PREFIX = "OC1."

        private const val MAX_CODE_LENGTH = 1024
        private const val MAX_JSON_BYTES = 4096
        private val HEX = Regex("^#[0-9A-Fa-f]{8}$")

        fun fromBase(id: String, name: String, base: CustomBase, light: Boolean): CustomThemeSpec {
            val theme = baseTheme(base, light)
            return CustomThemeSpec(
                id = id,
                name = sanitizeName(name),
                base = base,
                light = light,
                background = theme.pageBackground.copy(alpha = 1f),
                surface = theme.componentBackground.copy(alpha = 1f),
                text = theme.textColor,
                textSecondary = theme.textColorSecondary,
                border = theme.borderColor,
                accent = null,
                corners = if (base == CustomBase.MINECRAFT) CustomCorners.SQUARE else CustomCorners.ROUNDED,
                glass = when (base) {
                    CustomBase.POLYGLASS -> 100
                    CustomBase.POLYFROSTED -> 50
                    CustomBase.MINECRAFT -> 0
                },
            )
        }

        fun decode(code: String, id: String): CustomThemeSpec? {
            val trimmed = code.trim()
            if (trimmed.length > MAX_CODE_LENGTH || !trimmed.startsWith(CODE_PREFIX)) return null
            val compressed = runCatching { Base64.getUrlDecoder().decode(trimmed.substring(CODE_PREFIX.length)) }.getOrNull() ?: return null
            val json = inflate(compressed) ?: return null
            val root = JsonUtils.parseOrNull(json)?.takeIf { it.isJsonObject }?.asJsonObject ?: return null
            return fromJson(root, id)
        }

        internal fun fromJson(root: JsonObject, id: String): CustomThemeSpec? = runCatching {
            if (root.int("v") != 1) return null
            val base = root.string("base")?.let { b -> CustomBase.entries.firstOrNull { it.name.equals(b, true) } } ?: return null
            val light = root.get("light")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }?.asBoolean ?: return null
            val c = root.get("c")?.takeIf { it.isJsonObject }?.asJsonObject ?: return null
            val accentRaw = c.get("accent")
            val accent = if (accentRaw == null || accentRaw.isJsonNull) null else parseHex(accentRaw)?.copy(alpha = 1f) ?: return null
            val corners = when (val raw = root.string("corners")) {
                null -> CustomCorners.ROUNDED
                else -> CustomCorners.entries.firstOrNull { it.name.equals(raw, true) } ?: return null
            }
            CustomThemeSpec(
                id = id,
                name = sanitizeName(root.string("name") ?: ""),
                base = base,
                light = light,
                background = parseHex(c.get("bg"))?.copy(alpha = 1f) ?: return null,
                surface = parseHex(c.get("surface"))?.copy(alpha = 1f) ?: return null,
                text = parseHex(c.get("text"))?.copy(alpha = 1f) ?: return null,
                textSecondary = parseHex(c.get("text2"))?.copy(alpha = 1f) ?: return null,
                border = parseHex(c.get("border")) ?: return null,
                accent = accent,
                corners = corners,
                glass = (root.int("glass") ?: 0).coerceIn(0, 100),
            )
        }.getOrNull()

        fun sanitizeName(raw: String): String {
            val cleaned = raw.filter { it != '§' && !it.isISOControl() }.trim().take(NAME_LIMIT).trim()
            return cleaned.ifEmpty { "Custom" }
        }

        private fun inflate(data: ByteArray): String? {
            val inflater = Inflater()
            return try {
                inflater.setInput(data)
                val out = ByteArrayOutputStream()
                val buf = ByteArray(512)
                while (!inflater.finished()) {
                    val n = inflater.inflate(buf)
                    if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) return null
                    out.write(buf, 0, n)
                    if (out.size() > MAX_JSON_BYTES) return null
                }
                String(out.toByteArray(), StandardCharsets.UTF_8)
            } catch (_: Exception) {
                null
            } finally {
                inflater.end()
            }
        }

        private fun parseHex(element: JsonElement?): Color? {
            if (element == null || !element.isJsonPrimitive || !element.asJsonPrimitive.isString) return null
            val s = element.asString
            if (!HEX.matches(s)) return null
            return Color(s.substring(1).toLong(16).toInt())
        }

        private fun hex(color: Color): String = "#%08X".format(color.toArgb())

        private fun JsonObject.string(key: String): String? =
            get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

        private fun JsonObject.int(key: String): Int? =
            get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asDouble?.takeIf { it.isFinite() }?.toInt()
    }
}

object CustomThemes {
    private val logger = LoggerFactory.getLogger("OneConfig/CustomThemes")
    private const val FILE_NAME = "custom-themes.json"

    private val _specs = mutableStateListOf<CustomThemeSpec>()
    private var loaded = false
    private var pendingSave: ScheduledFuture<*>? = null

    @Volatile
    private var pendingBytes: ByteArray? = null
    private val writeSeq = AtomicLong()
    private val writeLock = Any()

    val specs: List<CustomThemeSpec> get() = _specs

    internal fun loadSpecs() {
        if (loaded) return
        loaded = true
        Runtime.getRuntime().addShutdownHook(Thread({ flush() }, "OneConfig custom themes save"))
        try {
            val path = file()
            if (!Files.exists(path)) return
            val root = JsonUtils.parseOrNull(String(Files.readAllBytes(path), StandardCharsets.UTF_8))
            val array = root?.takeIf { it.isJsonArray }?.asJsonArray
            if (array == null) {
                backUp(path)
                return
            }
            for (element in array) {
                if (_specs.size >= CustomThemeSpec.MAX) break
                val obj = element.takeIf { it.isJsonObject }?.asJsonObject ?: continue
                val id = obj.get("id")?.takeIf { it.isJsonPrimitive }?.asString?.filter { it.isLetterOrDigit() }?.take(16) ?: continue
                if (id.isEmpty() || _specs.any { it.id == id }) continue
                CustomThemeSpec.fromJson(obj, id)?.let(_specs::add)
            }
        } catch (e: Exception) {
            logger.error("Failed to load {}", FILE_NAME, e)
            runCatching { backUp(file()) }
        }
    }

    private fun backUp(path: Path) {
        if (!Files.exists(path)) return
        val backup = path.resolveSibling("$FILE_NAME.bak")
        Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING)
        logger.warn("{} could not be read, kept a copy at {}", FILE_NAME, backup.fileName)
    }

    fun create(base: CustomBase, light: Boolean): CustomThemeSpec? {
        if (_specs.size >= CustomThemeSpec.MAX) return null
        val spec = CustomThemeSpec.fromBase(newId(), nextName(), base, light)
        update(spec)
        return spec
    }

    fun createFrom(theme: UITheme, name: String? = null): CustomThemeSpec? {
        if (_specs.size >= CustomThemeSpec.MAX) return null
        val id = newId()
        val source = theme.customSpec
        val spec = source?.copy(id = id) ?: run {
            val base = when {
                theme.liquidGlass -> CustomBase.POLYGLASS
                theme.isMinecraftStyle -> CustomBase.MINECRAFT
                else -> CustomBase.POLYFROSTED
            }
            CustomThemeSpec.fromBase(id, theme.name, base, theme.textColor.luminance() < 0.5f).copy(
                background = theme.pageBackground.copy(alpha = 1f),
                surface = theme.componentBackground.copy(alpha = 1f),
                text = theme.textColor,
                textSecondary = theme.textColorSecondary,
                border = theme.borderColor,
                accent = theme.palette?.let(PaletteAccent::resolve),
            )
        }
        val named = spec.copy(name = CustomThemeSpec.sanitizeName(name ?: "${source?.name ?: theme.name} copy"))
        update(named)
        return named
    }

    fun update(spec: CustomThemeSpec) {
        val clean = spec.copy(name = CustomThemeSpec.sanitizeName(spec.name), glass = spec.glass.coerceIn(0, 100))
        val index = _specs.indexOfFirst { it.id == clean.id }
        if (index >= 0) {
            _specs[index] = clean
        } else {
            if (_specs.size >= CustomThemeSpec.MAX) return
            _specs += clean
        }
        ThemeRegistry.upsertCustom(clean.toUITheme())
        scheduleSave()
    }

    fun remove(id: String) {
        val spec = _specs.firstOrNull { it.id == id } ?: return
        _specs.remove(spec)
        ThemeRegistry.removeCustom(spec.themeId)
        scheduleSave()
    }

    fun import(code: String): CustomThemeSpec? {
        if (_specs.size >= CustomThemeSpec.MAX) return null
        val decoded = CustomThemeSpec.decode(code, newId()) ?: return null
        val spec = decoded.copy(name = uniqueName(decoded.name))
        update(spec)
        return spec
    }

    private fun newId(): String {
        var id: String
        do {
            id = UUID.randomUUID().toString().replace("-", "").take(8)
        } while (_specs.any { it.id == id })
        return id
    }

    private fun uniqueName(name: String): String {
        if (_specs.none { it.name == name }) return name
        var n = 2
        while (true) {
            val suffix = " #$n"
            val candidate = name.take(CustomThemeSpec.NAME_LIMIT - suffix.length).trimEnd() + suffix
            if (_specs.none { it.name == candidate }) return candidate
            n++
        }
    }

    private fun nextName(): String {
        var n = 1
        while (_specs.any { it.name == "Custom $n" }) n++
        return "Custom $n"
    }

    private fun file(): Path = ConfigManager.internal().folder.resolve(FILE_NAME)

    private fun scheduleSave() {
        val bytes = JsonArray().apply { _specs.forEach { add(it.toJson(true)) } }.toString().toByteArray(StandardCharsets.UTF_8)
        val seq = writeSeq.incrementAndGet()
        synchronized(writeLock) { pendingBytes = bytes }
        pendingSave?.cancel(false)
        pendingSave = Multithreading.submitScheduled({
            synchronized(writeLock) {
                if (seq == writeSeq.get()) writePending()
            }
        }, 500, TimeUnit.MILLISECONDS)
    }

    private fun flush() {
        synchronized(writeLock) { writePending() }
    }

    private fun writePending() {
        val bytes = pendingBytes ?: return
        try {
            val path = file()
            Files.createDirectories(path.parent)
            val temp = path.resolveSibling("$FILE_NAME.tmp")
            Files.write(temp, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
            try {
                Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: Exception) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING)
            }
            if (pendingBytes === bytes) pendingBytes = null
        } catch (e: Exception) {
            logger.error("Failed to save {}", FILE_NAME, e)
        }
    }
}

val UITheme.isCustom: Boolean get() = previewImage.startsWith("custom-")

val UITheme.customSpec: CustomThemeSpec? get() = if (isCustom) CustomThemes.specs.firstOrNull { it.themeId == previewImage } else null

internal fun UITheme.accentTextOn(accent: Color): Color =
    if (isCustom || contrastRatio(accentTextColor, accent) < 3f) readableOn(accent) else accentTextColor

val UITheme.previewAsset: String get() = customSpec?.let { baseTheme(it.base, it.light).previewImage } ?: previewImage

fun contrastRatio(a: Color, b: Color): Float {
    val la = a.copy(alpha = 1f).luminance()
    val lb = b.copy(alpha = 1f).luminance()
    return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
}

internal fun readableOn(color: Color): Color =
    if (contrastRatio(color, Color.White) >= contrastRatio(color, Color.Black)) Color.White else Color.Black

internal fun baseTheme(base: CustomBase, light: Boolean): UITheme = when (base) {
    CustomBase.POLYFROSTED -> if (light) PolyfrostedLight else PolyfrostedDark
    CustomBase.POLYGLASS -> if (light) PolyGlassLight else PolyGlassDark
    CustomBase.MINECRAFT -> if (light) MinecraftLight else MinecraftDark
}

private class CornerShapes(
    val background: Shape,
    val small: Shape,
    val medium: Shape,
    val tiny: Shape,
    val circle: Shape,
    val modIcon: Shape,
    val scrollBar: Shape,
)

private fun rounded(dp: Dp): Shape = RoundedCornerShape(dp)

private fun cornerShapes(corners: CustomCorners): CornerShapes = when (corners) {
    CustomCorners.ROUNDED -> CornerShapes(rounded(Radii.LG), rounded(Radii.SM), rounded(Radii.MD), rounded(Radii.XS), CircleShape, rounded(4.dp), rounded(8.dp))
    CustomCorners.SOFT -> CornerShapes(rounded(Radii.SM), rounded(4.dp), rounded(Radii.SM), rounded(3.dp), rounded(4.dp), rounded(2.dp), rounded(4.dp))
    CustomCorners.SQUARE -> CornerShapes(rounded(0.dp), rounded(0.dp), rounded(0.dp), rounded(0.dp), rounded(0.dp), rounded(0.dp), rounded(0.dp))
}
