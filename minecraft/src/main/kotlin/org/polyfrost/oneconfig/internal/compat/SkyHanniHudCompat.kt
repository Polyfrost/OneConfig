package org.polyfrost.oneconfig.internal.compat

import net.minecraft.client.gui.GuiGraphicsExtractor
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.hud.v1.OneConfigHudWrapper
import org.polyfrost.oneconfig.api.hud.v1.events.HudEditorToggleEvent
import org.polyfrost.oneconfig.internal.ui.hud.CompatOverlayRenderer
import java.lang.reflect.Field
import java.lang.reflect.Method

object SkyHanniHudCompat {
    private val LOGGER = LogManager.getLogger("OneConfig/SkyHanni-Compat")

    private const val GUI_EDIT_MANAGER = "at.hannibal2.skyhanni.data.GuiEditManager"
    private const val RENDER_DATA = "at.hannibal2.skyhanni.data.RenderData"
    private const val SKYHANNI_MOD = "at.hannibal2.skyhanni.SkyHanniMod"
    private const val POSITION = "at.hannibal2.skyhanni.config.core.config.Position"
    private const val CONFIG_LINK = "at.hannibal2.skyhanni.deps.moulconfig.annotations.ConfigLink"
    private const val MOUL_COMPAT_SKYHANNI = "org.polyfrost.oneconfig.internal.compat.MoulConfigCompat_skyhanni"

    internal const val DUMMY_ANCHOR_SIZE = 5

    private var initialized = false

    @Volatile
    private var translationsReady = false

    private var dirty = false

    @Volatile
    private var redrawing = false

    @JvmStatic
    fun isRedrawing(): Boolean = redrawing

    private val registered = HashSet<String>()

    @JvmStatic
    fun ensureRegistered() {
        if (!initialized) {
            initialized = true
            CompatLoader.requireTranslations(skip = true) { translationsReady = true }
            EventManager.register(HudEditorToggleEvent::class.java) { event ->
                if (!event.open) save()
            }
            CompatOverlayRenderer.register(::renderOverlay)
        }
        if (!translationsReady) return
        registerNewElements()
    }

    private fun registerNewElements() {
        val positions = currentPositions ?: return
        for (name in positionNames(positions)) {
            if (name in registered) continue
            if (name.startsWith("none ")) continue
            val entry = runCatching { positions[name] }.getOrNull() ?: continue
            if (isChestGuiOverlay(entry)) continue
            registered.add(name)
            runCatching { SkyHanniHudWrapper(name).register() }
                .onFailure { LOGGER.warn("Failed to register SkyHanni GUI element '{}'", name, it) }
        }
    }

    private fun positionNames(positions: Map<*, *>): List<String> = runCatching {
        val out = ArrayList<String>()
        for (key in positions.keys) (key as? String)?.let(out::add)
        out
    }.getOrElse { emptyList() }

    private fun renderOverlay(ctx: GuiGraphicsExtractor) {
        val method = postRenderOverlay ?: return
        redrawing = true
        try {
            method.invoke(null, ctx)
        } catch (t: Throwable) {
            LOGGER.debug("Failed to render SkyHanni elements above the blur", t)
        } finally {
            redrawing = false
        }
    }

    internal fun markDirty() {
        dirty = true
    }

    internal fun flush() {
        markDirty()
        save()
    }

    private fun save() {
        if (!dirty) return
        dirty = false
        runCatching { call(skyHanniConfig ?: return, "saveNow") }
            .onFailure { LOGGER.warn("Failed to save SkyHanni config", it) }
    }

    private val guiEditManagerClass: Class<*>? by lazy {
        runCatching { Class.forName(GUI_EDIT_MANAGER) }.getOrNull()
    }

    private val postRenderOverlay: Method? by lazy {
        runCatching {
            Class.forName(RENDER_DATA).methods.firstOrNull {
                it.name == "postRenderOverlay" && it.parameterCount == 1
            } ?: error("RenderData.postRenderOverlay not found")
        }.onFailure { LOGGER.warn("SkyHanni above-blur render unavailable: $it") }.getOrNull()
    }

    private val currentPositions: Map<*, *>?
        get() = readStaticField(guiEditManagerClass, "currentPositions") as? Map<*, *>

    private val currentBorderSize: Map<*, *>?
        get() = readStaticField(guiEditManagerClass, "currentBorderSize") as? Map<*, *>

    private val currentPositionPositionField: Field? by lazy {
        runCatching {
            Class.forName("at.hannibal2.skyhanni.data.CurrentPosition")
                .getDeclaredField("position").apply { isAccessible = true }
        }.getOrNull()
    }

    private val currentPositionChestField: Field? by lazy {
        runCatching {
            Class.forName("at.hannibal2.skyhanni.data.CurrentPosition")
                .getDeclaredField("isChestGuiOverlay").apply { isAccessible = true }
        }.getOrNull()
    }

    private fun isChestGuiOverlay(entry: Any?): Boolean =
        runCatching { currentPositionChestField?.getBoolean(entry) }.getOrNull() ?: false

    internal fun position(internalName: String): Any? = runCatching {
        val entry = currentPositions?.get(internalName) ?: return null
        currentPositionPositionField?.get(entry)
    }.getOrNull()

    internal fun borderSize(internalName: String): Pair<Int, Int>? = runCatching {
        val pair = currentBorderSize?.get(internalName) ?: return null
        val first = call(pair, "getFirst") as? Number ?: return null
        val second = call(pair, "getSecond") as? Number ?: return null
        first.toInt() to second.toInt()
    }.getOrNull()

    private val skyHanniMod: Class<*>? by lazy { runCatching { Class.forName(SKYHANNI_MOD) }.getOrNull() }

    private val skyHanniConfig: Any?
        get() = readStaticField(skyHanniMod, "feature")

    private val processor: Any? by lazy {
        runCatching {
            val mod = skyHanniMod ?: error("SkyHanniMod not found")
            val instance = mod.getField("INSTANCE").get(null)
            val configManager = call(instance, "getConfigManager") ?: error("configManager unavailable")
            call(configManager, "getProcessor") ?: error("processor unavailable")
        }.onFailure { LOGGER.warn("SkyHanni MoulConfig processor unavailable; HUD settings disabled: $it") }
            .getOrNull()
    }

    private val absXMethod: Method? by lazy { extensionMethod("getAbsX") }
    private val absYMethod: Method? by lazy { extensionMethod("getAbsY") }

    private val guiEditManagerInstance: Any? by lazy {
        runCatching { guiEditManagerClass?.getField("INSTANCE")?.get(null) }.getOrNull()
    }

    private fun extensionMethod(name: String): Method? = runCatching {
        guiEditManagerClass?.methods?.firstOrNull {
            it.name == name && it.parameterCount == 1 && it.parameterTypes[0].name == POSITION
        }
    }.getOrNull()

    internal fun absX(position: Any): Int = runCatching {
        absXMethod?.invoke(guiEditManagerInstance, position) as? Number
            ?: call(position, "getAbsX0", DUMMY_ANCHOR_SIZE) as? Number
    }.getOrNull()?.toInt() ?: 0

    internal fun absY(position: Any): Int = runCatching {
        absYMethod?.invoke(guiEditManagerInstance, position) as? Number
            ?: call(position, "getAbsY0", DUMMY_ANCHOR_SIZE) as? Number
    }.getOrNull()?.toInt() ?: 0

    internal fun effectiveScale(position: Any): Float =
        (call(position, "getEffectiveScale") as? Number)?.toFloat() ?: 1f

    internal fun ignoresScale(position: Any): Boolean = runCatching {
        position.javaClass.getDeclaredField("ignoreCustomScale")
            .apply { isAccessible = true }.getBoolean(position)
    }.getOrNull() ?: false

    private fun linkField(position: Any): Field? =
        runCatching { call(position, "getLinkField") as? Field }.getOrNull()

    private val linksByOwner: Map<Class<*>, List<String>> by lazy { collectLinks() }

    private fun collectLinks(): Map<Class<*>, List<String>> = runCatching {
        @Suppress("UNCHECKED_CAST")
        val linkAnnotation = Class.forName(CONFIG_LINK) as Class<out Annotation>
        val ownerMethod = linkAnnotation.getMethod("owner")
        val fieldMethod = linkAnnotation.getMethod("field")
        val root = skyHanniConfig?.javaClass ?: error("SkyHanni config unavailable")

        val out = HashMap<Class<*>, MutableList<String>>()
        val visited = HashSet<Class<*>>()

        fun walk(cls: Class<*>) {
            if (!cls.name.startsWith("at.hannibal2.skyhanni.")) return
            if (!visited.add(cls)) return
            for (field in cls.declaredFields) {
                field.getAnnotation(linkAnnotation)?.let { link ->
                    val owner = ownerClassOf(ownerMethod.invoke(link))
                    val name = fieldMethod.invoke(link) as? String
                    if (owner != null && name != null) out.getOrPut(owner) { ArrayList() }.add(name)
                }
                walk(field.type)
            }
        }

        walk(root)
        out
    }.onFailure { LOGGER.warn("Failed to map SkyHanni @ConfigLink owners; settings may over-include: $it") }
        .getOrElse { emptyMap() }

    private fun ownerClassOf(value: Any?): Class<*>? =
        value as? Class<*> ?: (value as? kotlin.reflect.KClass<*>)?.java

    internal fun buildSettings(internalName: String, label: String): List<Property<*>> {
        val position = position(internalName) ?: return emptyList()
        val link = linkField(position) ?: return emptyList()
        val owner = link.declaringClass ?: return emptyList()
        val config = skyHanniConfig ?: return emptyList()
        val processor = processor ?: return emptyList()

        val all = runCatching {
            val compat = Class.forName(MOUL_COMPAT_SKYHANNI)
            val method = compat.methods.firstOrNull {
                it.name == "buildPropertiesForClass" && it.parameterCount == 5
            } ?: error("buildPropertiesForClass not found on $MOUL_COMPAT_SKYHANNI")
            @Suppress("UNCHECKED_CAST")
            method.invoke(null, processor, config, owner, label, "Settings") as? List<Property<*>> ?: emptyList()
        }.getOrElse {
            LOGGER.warn("Failed to build SkyHanni settings for '{}'", internalName, it)
            return emptyList()
        }

        val linked = linksByOwner[owner]
        val scoped = when {
            linked == null || linked.size <= 1 -> all
            else -> scopeToFeature(all, linked, link.name)
        }
        return scoped.onEach(::wireDirty)
    }

    private fun wireDirty(property: Property<*>) {
        @Suppress("UNCHECKED_CAST")
        (property as Property<Any?>).addCallback { markDirty(); false }
    }

    private fun scopeToFeature(
        properties: List<Property<*>>,
        linkedFields: List<String>,
        myField: String,
    ): List<Property<*>> = properties.filter { property ->
        val name = fieldNameOf(property) ?: return@filter false
        val best = linkedFields.filter { name == it || name.startsWith(it) }.maxByOrNull { it.length }
        best == myField
    }

    private fun fieldNameOf(property: Property<*>): String? =
        property.getMetadata<String>("oc_snapshot_key")?.substringAfterLast('#')

    private fun readStaticField(cls: Class<*>?, name: String): Any? = runCatching {
        val field = (cls ?: return null).getDeclaredField(name).apply { isAccessible = true }
        runCatching { field.get(null) }.getOrElse {
            field.get(cls.getField("INSTANCE").get(null))
        }
    }.getOrNull()

    internal fun call(target: Any, name: String, vararg args: Any?): Any? {
        val method = target.javaClass.methods.firstOrNull { it.name == name && it.parameterCount == args.size }
            ?: return null
        method.isAccessible = true
        return method.invoke(target, *args)
    }
}

private class SkyHanniHudWrapper(private val internalName: String) : OneConfigHudWrapper {
    private val position: Any? get() = SkyHanniHudCompat.position(internalName)

    override var id: String = "skyhanni_" + internalName.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    override var name: String = internalName

    override val modId: String = "skyhanni"

    override val placementReady: Boolean get() = position != null

    override var x: Float
        get() = position?.let { SkyHanniHudCompat.absX(it).toFloat() } ?: 0f
        set(value) = move(value, horizontal = true)

    override var y: Float
        get() = position?.let { SkyHanniHudCompat.absY(it).toFloat() } ?: 0f
        set(value) = move(value, horizontal = false)

    private fun move(value: Float, horizontal: Boolean) {
        val position = position ?: return
        val current = if (horizontal) SkyHanniHudCompat.absX(position) else SkyHanniHudCompat.absY(position)
        val delta = Math.round(value) - current
        if (delta == 0) return
        val method = if (horizontal) "moveX" else "moveY"
        runCatching {
            SkyHanniHudCompat.call(position, method, delta, SkyHanniHudCompat.DUMMY_ANCHOR_SIZE)
        }
        SkyHanniHudCompat.markDirty()
    }

    override var scale: Float
        get() = (position?.let { SkyHanniHudCompat.call(it, "getScale") } as? Number)?.toFloat() ?: 1f
        set(value) {
            val position = position ?: return
            runCatching { SkyHanniHudCompat.call(position, "setScale", value) }
            SkyHanniHudCompat.markDirty()
        }

    override val supportsScale: Boolean
        get() = position?.let { !SkyHanniHudCompat.ignoresScale(it) } ?: true

    override var scaledWidth: Float
        get() = scaledSize(width = true)
        set(_) {}

    override var scaledHeight: Float
        get() = scaledSize(width = false)
        set(_) {}

    private fun scaledSize(width: Boolean): Float {
        val position = position ?: return 0f
        val size = SkyHanniHudCompat.borderSize(internalName) ?: return 0f
        val raw = if (width) size.first else size.second
        return raw * SkyHanniHudCompat.effectiveScale(position)
    }

    private val cachedProperties: List<Property<*>> by lazy {
        SkyHanniHudCompat.buildSettings(internalName, name)
    }

    private val enabledProperty: Property<Boolean>? by lazy { CompatHudToggle.find(cachedProperties) }

    override var hidden: Boolean
        get() = enabledProperty?.get() == false
        set(value) { enabledProperty?.set(!value) }

    override fun linkedProperties(): List<Property<*>> = cachedProperties

    override fun save() = SkyHanniHudCompat.flush()
}
