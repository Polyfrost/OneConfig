//~ main_render_target
package org.polyfrost.oneconfig.internal.ui.compose

//? if >= 1.21.11 {
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.systems.RenderSystem
//? if >= 26.3 {
import com.mojang.renderpearl.api.pipeline.RenderPipeline
//?} else
//import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.renderpearl.api.textures.FilterMode
import com.mojang.renderpearl.api.textures.GpuTexture
import com.mojang.renderpearl.api.textures.GpuTextureView
import java.util.Collections
import java.util.IdentityHashMap
//? if < 26.2
//import java.lang.reflect.Method
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
//? if >= 26.1 {
import net.minecraft.client.renderer.state.gui.GuiElementRenderState
//?} else
//import net.minecraft.client.gui.render.state.GuiElementRenderState
import net.minecraft.resources.Identifier
import org.polyfrost.oneconfig.internal.mixin.render.RenderPipelinesAccessor
import org.polyfrost.oneconfig.internal.ui.hud.GuiTargetRedirect
import kotlin.math.max
import kotlin.math.min

internal object LiquidGlass {
    val pipeline: RenderPipeline = RenderPipeline.builder(RenderPipelinesAccessor.`oneconfig$getGuiTexturedSnippet`())
        .withLocation(Identifier.fromNamespaceAndPath("oneconfig", "pipeline/liquid_glass"))
        //? if >= 26.3 {
        .withFragmentShader(Identifier.fromNamespaceAndPath("oneconfig", "core/liquid_glass"))
        //?} else
        //.withFragmentShader(Identifier.fromNamespaceAndPath("oneconfig", "core/liquid_glass_legacy"))
        .build()

    private var texture: GpuTexture? = null
    private var view: GpuTextureView? = null
    private var setup: TextureSetup = TextureSetup.noTexture()
    private val drawBounds = IdentityHashMap<Any, ScreenRectangle?>()
    private val glassDraws: MutableSet<Any> = Collections.newSetFromMap(IdentityHashMap())
    private val dirty = ArrayList<ScreenRectangle?>()
    private var submitted = false

    fun interface Range {
        fun draw(from: Int, to: Int)
    }

    fun setup(): TextureSetup {
        val main = Minecraft.getInstance().gameRenderer.mainRenderTarget()
        val color = main.colorTexture ?: return setup
        val current = texture
        if (current == null || current.isClosed || current.getWidth(0) != main.width || current.getHeight(0) != main.height || current.format != color.format) {
            view?.close()
            current?.close()
            val device = RenderSystem.getDevice()
            val created = device.createTexture({ "OneConfig liquid glass backdrop" }, GpuTexture.USAGE_COPY_DST or GpuTexture.USAGE_TEXTURE_BINDING, color.format, main.width, main.height, 1, 1)
            val createdView = device.createTextureView(created)
            texture = created
            view = createdView
            setup = TextureSetup.singleTexture(createdView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
        }
        return setup
    }

    fun submit() {
        submitted = true
    }

    //? if < 26.2 {
    /*private val byIndex = HashMap<Int, Any>()
    private var drawClass: Class<*>? = null
    private var drawPipeline: Method? = null

    @JvmStatic
    fun added(index: Int, element: GuiElementRenderState) {
        if (!submitted) return
        added(byIndex.getOrPut(index) { Any() }, element)
    }

    private fun pipelineOf(draw: Any): Any? {
        val cls = draw.javaClass
        if (cls !== drawClass) {
            drawClass = cls
            drawPipeline = runCatching { cls.recordComponents?.firstOrNull { it.type === RenderPipeline::class.java }?.accessor?.also { it.isAccessible = true } }.getOrNull()
        }
        return drawPipeline?.invoke(draw)
    }

    @JvmStatic
    fun recorded(draws: List<*>) {
        if (byIndex.isEmpty()) return
        val byPipeline = draws.firstOrNull()?.let { pipelineOf(it) } != null
        var aligned = true
        for ((i, key) in byIndex) {
            val glass = glassDraws.remove(key)
            val known = drawBounds.containsKey(key)
            val bounds = drawBounds.remove(key)
            val draw = draws.getOrNull(i)
            if (draw == null) {
                if (glass) aligned = false
                continue
            }
            if (byPipeline && glass != (pipelineOf(draw) === pipeline)) aligned = false
            if (known) drawBounds[draw] = bounds
            if (!byPipeline && glass) glassDraws.add(draw)
        }
        if (byPipeline) for (draw in draws) if (draw != null && pipelineOf(draw) === pipeline) glassDraws.add(draw)
        if (!aligned) for (draw in draws) drawBounds.remove(draw)
        byIndex.clear()
    }
    *///?}

    @JvmStatic
    fun added(draw: Any, element: GuiElementRenderState) {
        if (!submitted) return
        val b = element.bounds()
        if (drawBounds.containsKey(draw)) {
            val prev = drawBounds[draw]
            drawBounds[draw] = if (prev == null || b == null) null else union(prev, b)
        } else {
            drawBounds[draw] = b
        }
        if (element.pipeline() === pipeline) glassDraws.add(draw)
    }

    @JvmStatic
    fun has(draws: List<*>, start: Int, end: Int): Boolean {
        if (glassDraws.isEmpty()) return false
        for (i in start until end) if (draws[i] in glassDraws) return true
        return false
    }

    @JvmStatic
    fun split(draws: List<*>, start: Int, end: Int, target: RenderTarget, range: Range) {
        val redirected = GuiTargetRedirect.target != null
        var copied = false
        dirty.clear()
        var from = start
        var scanned = start
        for (i in start until end) {
            val draw = draws[i] ?: continue
            if (draw !in glassDraws) continue
            if (redirected) {
                if (i > from) range.draw(from, i)
                from = i + 1
                continue
            }
            for (k in scanned until i) dirty.add(draws[k]?.let { drawBounds[it] })
            scanned = i
            if (copied && !covers(drawBounds[draw])) continue
            if (i > from) range.draw(from, i)
            if (!copy(target)) {
                from = i + 1
                scanned = i + 1
                continue
            }
            copied = true
            dirty.clear()
            from = i
        }
        if (end > from) range.draw(from, end)
        dirty.clear()
    }

    @JvmStatic
    fun endFrame() {
        if (GuiTargetRedirect.target != null) return
        submitted = false
        drawBounds.clear()
        glassDraws.clear()
    }

    private fun covers(region: ScreenRectangle?): Boolean {
        if (region == null) return true
        for (d in dirty) {
            if (d == null) return true
            if (d.left() < region.right() && region.left() < d.right() && d.top() < region.bottom() && region.top() < d.bottom()) return true
        }
        return false
    }

    private fun copy(target: RenderTarget): Boolean {
        val src = target.colorTexture ?: return false
        val dst = texture ?: return false
        if (dst.isClosed) return false
        val w = src.getWidth(0)
        val h = src.getHeight(0)
        if (dst.getWidth(0) != w || dst.getHeight(0) != h || dst.format != src.format) return false
        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(src, dst, 0, 0, 0, 0, 0, w, h)
        return true
    }

    private fun union(a: ScreenRectangle, b: ScreenRectangle): ScreenRectangle {
        val l = min(a.left(), b.left())
        val t = min(a.top(), b.top())
        return ScreenRectangle(l, t, max(a.right(), b.right()) - l, max(a.bottom(), b.bottom()) - t)
    }
}
//?}
