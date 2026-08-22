/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
 *
 * OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program. If not, see
 * <https://www.gnu.org/licenses/>. You should have also received a copy of the
 * Additional Terms Applicable to OneConfig, as published by Polyfrost. If not,
 * see <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.internal.compat

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import org.joml.Vector2i
import org.joml.Vector2ic
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.hud.v1.OneConfigHudWrapper
import org.polyfrost.oneconfig.api.hud.v1.events.HudEditorToggleEvent
import org.polyfrost.oneconfig.internal.ui.hud.CompatOverlayRenderer

object FirmamentHudCompat {
    private val LOGGER = org.apache.logging.log4j.LogManager.getLogger("OneConfig/FirmamentHudCompat")

    private const val JARVIS_INTEGRATION = "moe.nea.firmament.jarvis.JarvisIntegration"
    private const val HUD_META_HANDLER = "moe.nea.firmament.gui.config.HudMetaHandler"
    private const val MC_CONFIG_EDITOR_INTEGRATION = "moe.nea.firmament.compat.moulconfig.MCConfigEditorIntegration"
    private const val MOUL_COMPAT_FIRMAMENT = "org.polyfrost.oneconfig.internal.compat.MoulConfigCompat_firmament"

    private var jarvisIntegration: Any? = null
    private var closeHookRegistered = false
    private var overlayHookRegistered = false

    @JvmStatic
    fun register() {
        if (!CompatLoader.hasMod("firmament")) return
        registerOverlayHook()
        CompatLoader.requireTranslations(skip = true) {
            runCatching {
                val integration = jarvisIntegration()
                runCatching { call(integration, "getAllHuds") }

                @Suppress("UNCHECKED_CAST")
                val configs = call(integration, "getConfigs") as? List<Any> ?: return@requireTranslations

                var registered = 0
                for (config in configs) {
                    @Suppress("UNCHECKED_CAST")
                    val options = call(config, "getSortedOptions") as? List<Any> ?: continue
                    for (option in options) {
                        if (!isHudMetaOption(option)) continue
                        val hudMeta = call(option, "getValue") ?: continue
                        runCatching {
                            FirmamentHudWrapper(hudMeta, config, option, options).register()
                            registered++
                        }.onFailure { LOGGER.error("Failed to register Firmament HUD for option $option: $it") }
                    }
                }

                if (registered > 0) registerCloseHook()
                LOGGER.info("Registered $registered Firmament HUD(s).")
            }.onFailure {
                LOGGER.error("Failed to load Firmament HUD compat: $it")
            }
        }
    }

    private fun registerOverlayHook() {
        if (overlayHookRegistered) return
        overlayHookRegistered = true
        CompatOverlayRenderer.register(::renderOverlays)
    }

    private val hudRenderEventCtor: java.lang.reflect.Constructor<*>? by lazy {
        runCatching {
            val eventCls = Class.forName("moe.nea.firmament.events.HudRenderEvent")
            (eventCls.declaredConstructors.firstOrNull { it.parameterCount == 2 }
                ?: error("HudRenderEvent 2-arg constructor not found")).also { it.isAccessible = true }
        }.onFailure { LOGGER.warn("Could not resolve HudRenderEvent constructor for Firmament render: $it") }
            .getOrNull()
    }

    private val zeroDeltaTracker: Any? by lazy {
        runCatching {
            val deltaClass = hudRenderEventCtor?.parameterTypes?.getOrNull(1)
                ?: error("HudRenderEvent constructor unavailable")
            val zero = deltaClass.fields.firstOrNull {
                java.lang.reflect.Modifier.isStatic(it.modifiers) && it.type == deltaClass
            } ?: error("no static ${deltaClass.name} constant (DeltaTracker.ZERO) found")
            zero.get(null)
        }.onFailure { LOGGER.warn("Could not resolve DeltaTracker.ZERO for Firmament render: $it") }
            .getOrNull()
    }

    private fun renderOverlays(ctx: GuiGraphicsExtractor) {
        val ctor = hudRenderEventCtor ?: return
        val delta = zeroDeltaTracker ?: return
        capturingHudIds = HashSet()
        capturingBounds = HashMap()
        capturingPos = HashMap()
        currentCapturingHudId = null
        runCatching {
            val eventCls = ctor.declaringClass
            val event = ctor.newInstance(ctx, delta)
            val companion = eventCls.getField("Companion").get(null)
            val publish = companion.javaClass.methods.firstOrNull { it.name == "publish" && it.parameterCount == 1 }
                ?: error("HudRenderEvent.Companion.publish not found")
            publish.invoke(companion, event)
        }.onFailure { LOGGER.debug("Firmament above-blur render failed: $it") }
        visibleHudIds = capturingHudIds ?: emptySet()
        capturedBounds = finalizeCapturedBounds()
        capturingHudIds = null
        capturingBounds = null
        capturingPos = null
        currentCapturingHudId = null
    }

    private fun finalizeCapturedBounds(): Map<String, FloatArray> {
        val bounds = capturingBounds ?: return emptyMap()
        val positions = capturingPos ?: return emptyMap()
        if (bounds.isEmpty()) return emptyMap()
        val out = HashMap<String, FloatArray>(bounds.size)
        for ((id, box) in bounds) {
            val pos = positions[id] ?: continue
            out[id] = floatArrayOf(box[0] - pos[0], box[1] - pos[1], box[2] - box[0], box[3] - box[1])
        }
        return out
    }

    @Volatile private var capturingHudIds: MutableSet<String>? = null

    @Volatile private var visibleHudIds: Set<String> = emptySet()

    @Volatile private var currentCapturingHudId: String? = null

    private var capturingBounds: MutableMap<String, FloatArray>? = null

    private var capturingPos: MutableMap<String, IntArray>? = null

    @Volatile private var capturedBounds: Map<String, FloatArray> = emptyMap()

    @JvmStatic
    fun noteHudRendered(hudMeta: Any) {
        val capturing = capturingHudIds ?: return
        val id = runCatching { call(hudMeta, "getHudId")?.toString() }.getOrNull() ?: return
        capturing.add(id)
        currentCapturingHudId = id
        val pos = livePosition(hudMeta) ?: return
        capturingPos?.put(id, intArrayOf(pos.x(), pos.y()))
    }

    @JvmStatic
    fun isCapturing(): Boolean = capturingBounds != null && currentCapturingHudId != null

    @JvmStatic
    fun noteContentDrawn(minX: Float, minY: Float, maxX: Float, maxY: Float) {
        val id = currentCapturingHudId ?: return
        val cur = capturingBounds?.get(id)
        if (cur == null) {
            capturingBounds?.put(id, floatArrayOf(minX, minY, maxX, maxY))
        } else {
            cur[0] = minOf(cur[0], minX)
            cur[1] = minOf(cur[1], minY)
            cur[2] = maxOf(cur[2], maxX)
            cur[3] = maxOf(cur[3], maxY)
        }
    }

    private fun renderedRecently(hudId: String): Boolean = hudId in visibleHudIds

    private fun livePosition(hudMeta: Any): Vector2ic? {
        val method = hudMeta.javaClass.methods.firstOrNull {
            it.name == "getPosition" && it.parameterCount == 0 &&
                Vector2ic::class.java.isAssignableFrom(it.returnType)
        } ?: return null
        return runCatching { method.invoke(hudMeta) as? Vector2ic }.getOrNull()
    }

    private fun flush() {
        runCatching { call(jarvisIntegration(), "onHudEditorClosed") }
            .onFailure { LOGGER.error("Failed to flush Firmament HUD edits: $it") }
    }

    private fun registerCloseHook() {
        if (closeHookRegistered) return
        closeHookRegistered = true
        EventManager.register(HudEditorToggleEvent::class.java) { event ->
            if (!event.open) flush()
        }
    }

    private fun jarvisIntegration(): Any =
        jarvisIntegration ?: Class.forName(JARVIS_INTEGRATION)
            .getDeclaredConstructor().newInstance().also { jarvisIntegration = it }

    private fun isHudMetaOption(option: Any): Boolean {
        val handler = call(option, "getHandler") ?: return false
        return isInstanceOf(handler, HUD_META_HANDLER)
    }

    private val editorIntegration: Any? by lazy {
        runCatching { Class.forName(MC_CONFIG_EDITOR_INTEGRATION).getDeclaredConstructor().newInstance() }
            .onFailure { LOGGER.warn("Firmament MoulConfig editor integration unavailable; HUD settings disabled: $it") }
            .getOrNull()
    }

    private val processedOptions: List<Pair<Any, Any?>> by lazy {
        val integration = editorIntegration ?: return@lazy emptyList()
        runCatching {
            @Suppress("UNCHECKED_CAST")
            val categories = call(integration, "getCategories") as? List<Any> ?: return@lazy emptyList()
            categories.flatMap { category ->
                @Suppress("UNCHECKED_CAST")
                val opts = call(category, "getOptions") as? List<Any> ?: emptyList()
                opts.map { it to runCatching { call(it, "getManagedOption") }.getOrNull() }
            }
        }.getOrElse { emptyList() }
    }

    private fun buildLinkedProperties(
        hudMetaOption: Any,
        owningConfigOptions: List<Any>,
        label: String,
    ): List<Property<*>> {
        val integration = editorIntegration ?: return emptyList()
        val configObject = runCatching { call(integration, "getConfigObject") }.getOrNull() ?: return emptyList()

        val siblings = siblingsForHud(hudMetaOption, owningConfigOptions)
        if (siblings.isEmpty()) return emptyList()

        val processed = processedOptions.mapNotNull { (proc, managed) ->
            if (managed != null && siblings.any { it === managed }) proc else null
        }
        if (processed.isEmpty()) return emptyList()

        return runCatching {
            val compat = Class.forName(MOUL_COMPAT_FIRMAMENT)
            val method = compat.methods.firstOrNull {
                it.name == "buildPropertiesForOptions" && it.parameterCount == 4
            } ?: error("buildPropertiesForOptions not found on $MOUL_COMPAT_FIRMAMENT")
            @Suppress("UNCHECKED_CAST")
            val props = (method.invoke(null, configObject, processed, label, label) as? List<Property<*>>) ?: emptyList()
            props.onEach(::resolvePropertyText)
        }.getOrElse {
            LOGGER.error("Failed to build Firmament HUD settings for '$label': $it")
            emptyList()
        }
    }

    private fun resolvePropertyText(prop: Property<*>) {
        (prop.title as? String)?.let { title -> translate(title)?.let { prop.addMetadata("title", it) } }
        (prop.description as? String)?.let { desc ->
            prop.description = translate(desc) ?: if (looksLikeKey(desc)) "" else desc
        }
    }

    private fun looksLikeKey(text: String): Boolean =
        text.startsWith("firmament.") && !text.contains(' ')

    private fun siblingsForHud(hudMetaOption: Any, options: List<Any>): List<Any> {
        val hudOptions = options.filter { isHudMetaOption(it) }
        val nonHud = options.filter { opt -> hudOptions.none { it === opt } }
        if (hudOptions.size <= 1) return nonHud

        val bases = hudOptions.map { baseName(propertyName(it)) }
        val myBase = baseName(propertyName(hudMetaOption))
        return nonHud.filter { opt ->
            val name = propertyName(opt)
            val best = bases.filter { name == it || name.startsWith("$it-") || name.startsWith(it) }
                .maxByOrNull { it.length }
            best == myBase
        }
    }

    private fun propertyName(option: Any): String = call(option, "getPropertyName") as? String ?: ""

    private fun baseName(propertyName: String): String = propertyName.removeSuffix("-hud")

    private fun call(target: Any, name: String, vararg args: Any?): Any? {
        val method = target.javaClass.methods.firstOrNull { it.name == name && it.parameterCount == args.size }
            ?: return null
        method.isAccessible = true
        return method.invoke(target, *args)
    }

    private fun translate(key: String?): String? {
        if (key.isNullOrBlank()) return null
        val resolved = runCatching { Component.translatable(key).string }.getOrNull()
        return if (resolved.isNullOrEmpty() || resolved == key) null else resolved
    }

    private fun isInstanceOf(value: Any, className: String): Boolean {
        var cls: Class<*>? = value.javaClass
        while (cls != null) {
            if (cls.name == className) return true
            if (cls.interfaces.any { it.name == className }) return true
            cls = cls.superclass
        }
        return false
    }

    private class FirmamentHudWrapper(
        initialHudMeta: Any,
        owningConfig: Any,
        private val hudMetaOption: Any,
        private val owningConfigOptions: List<Any>,
    ) : OneConfigHudWrapper {
        private val initialHudMeta: Any = initialHudMeta
        private val hudMeta: Any
            get() = runCatching { call(hudMetaOption, "getValue") }.getOrNull() ?: initialHudMeta

        override var id: String = (call(initialHudMeta, "getHudId")?.toString() ?: "firmament:hud")
        override var name: String = resolveLabel(call(initialHudMeta, "getLabel")) ?: id
        override val modId: String = "firmament"

        private fun capturedBox(): FloatArray? = capturedBounds[id]

        override val placementReady: Boolean get() = position() != null

        override var x: Float
            get() = (position()?.x()?.toFloat() ?: 0f) + (capturedBox()?.get(0) ?: 0f)
            set(value) {
                val dx = capturedBox()?.get(0) ?: 0f
                val py = position()?.y() ?: 0
                call(hudMeta, "setPosition", Vector2i((value - dx).toInt(), py) as Vector2ic)
            }

        override var y: Float
            get() = (position()?.y()?.toFloat() ?: 0f) + (capturedBox()?.get(1) ?: 0f)
            set(value) {
                val dy = capturedBox()?.get(1) ?: 0f
                val px = position()?.x() ?: 0
                call(hudMeta, "setPosition", Vector2i(px, (value - dy).toInt()) as Vector2ic)
            }

        override var scale: Float
            get() = (call(hudMeta, "getScale") as? Number)?.toFloat() ?: 1f
            set(value) {
                call(hudMeta, "setScale", value)
            }

        override var scaledWidth: Float
            get() = if (!renderedRecently(id)) 0f
            else capturedBox()?.get(2) ?: (call(hudMeta, "getEffectiveWidth") as? Number)?.toFloat() ?: 0f
            set(_) {}

        override var scaledHeight: Float
            get() = if (!renderedRecently(id)) 0f
            else capturedBox()?.get(3) ?: (call(hudMeta, "getEffectiveHeight") as? Number)?.toFloat() ?: 0f
            set(_) {}

        private val cachedProperties: List<Property<*>> by lazy {
            buildLinkedProperties(hudMetaOption, owningConfigOptions, name)
        }

        private val enabledProperty: Property<Boolean>? by lazy { CompatHudToggle.find(cachedProperties) }

        override var hidden: Boolean
            get() = enabledProperty?.get() == false
            set(value) { enabledProperty?.set(!value) }

        override fun linkedProperties(): List<Property<*>> = cachedProperties

        override fun save() = flush()

        private fun position(): Vector2ic? = livePosition(hudMeta)

        private fun resolveLabel(component: Any?): String? {
            component ?: return null
            return runCatching { (component as? Component)?.string }.getOrNull()?.takeIf { it.isNotEmpty() }
                ?: component.toString()
        }
    }
}
