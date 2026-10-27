//~ main_render_target
package org.polyfrost.oneconfig.internal.ui.compose

//? if >= 1.21.11 {
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.renderpearl.api.buffers.GpuBuffer
import com.mojang.renderpearl.api.textures.FilterMode
import com.mojang.renderpearl.api.textures.GpuTextureView
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.PostChain
import net.minecraft.client.renderer.PostPass
import net.minecraft.resources.Identifier
import org.lwjgl.system.MemoryStack
import org.polyfrost.oneconfig.internal.mixin.render.PostChainAccessor
import org.polyfrost.oneconfig.internal.mixin.render.PostPassAccessor
import org.polyfrost.oneconfig.internal.ui.OffscreenTarget
//? if >= 26.2 {
import java.util.Optional
import org.joml.Vector4fc
//?} else
//import java.util.OptionalInt

object UiBlur {
    private val CHAIN = Identifier.fromNamespaceAndPath("oneconfig", "ui_blur")
    private val OUT = Identifier.fromNamespaceAndPath("oneconfig", "ui_blur_out")
    private val TARGETS = setOf(PostChain.MAIN_TARGET_ID, OUT)
    private const val REUSE_NS = 50_000_000L

    private val out = OffscreenTarget()
    private val a = OffscreenTarget()
    private val b = OffscreenTarget()
    private var pending = false
    private var blurred: RenderTarget? = null
    private var blurredAt = 0L

    private var downInfo: GpuBuffer? = null
    private var blurInfo: GpuBuffer? = null
    private var infoKey = 0L

    fun extract(ctx: GuiGraphicsExtractor, alpha: Float) {
        if (alpha <= 0f) return
        val main = Minecraft.getInstance().gameRenderer.mainRenderTarget()
        if (!out.resolve((main.width + 3) / 4, (main.height + 3) / 4)) return
        val target = out.target ?: return
        pending = true
        ctx.blurBeforeThisStratum()
        val renderer = BlazeRenderer(Minecraft.getInstance().window.guiScale.toFloat())
        renderer.texture(target, 0f, 0f, main.width.toFloat(), main.height.toFloat(), 0f, 1f, 1f, 0f, alpha)
        for (op in renderer.finish()) op(ctx)
    }

    @JvmStatic
    fun process(): Boolean {
        if (!pending) return false
        pending = false
        val target = out.target ?: return false
        val now = System.nanoTime()
        if (target === blurred && now - blurredAt < REUSE_NS && Minecraft.getInstance().isPaused) return true
        val chain = Minecraft.getInstance().shaderManager.getPostChain(CHAIN, TARGETS) ?: return false
        val passes = (chain as PostChainAccessor).`oneconfig$getPasses`()
        if (passes.size != 3) return false
        val w = target.width
        val h = target.height
        if (!a.resolve(w, h) || !b.resolve(w, h)) return false
        val main = Minecraft.getInstance().gameRenderer.mainRenderTarget()
        val down = downInfo(main.width, main.height, w, h)
        val blur = blurInfo ?: return false
        val src = main.colorTextureView ?: return false
        val ta = a.target ?: return false
        val tb = b.target ?: return false
        draw(passes[0], src, ta, down)
        draw(passes[1], ta.colorTextureView ?: return false, tb, blur)
        draw(passes[2], tb.colorTextureView ?: return false, target, blur)
        blurred = target
        blurredAt = now
        return true
    }

    private fun downInfo(mainW: Int, mainH: Int, w: Int, h: Int): GpuBuffer {
        val key = mainW.toLong() shl 32 or mainH.toLong()
        val down = downInfo
        if (down != null && key == infoKey) return down
        downInfo?.close()
        blurInfo?.close()
        infoKey = key
        blurInfo = info(w, h, w, h)
        return info(w, h, mainW, mainH).also { downInfo = it }
    }

    private fun info(outW: Int, outH: Int, inW: Int, inH: Int): GpuBuffer = MemoryStack.stackPush().use { stack ->
        val data = stack.malloc(16)
        data.putFloat(outW.toFloat()).putFloat(outH.toFloat()).putFloat(inW.toFloat()).putFloat(inH.toFloat()).flip()
        RenderSystem.getDevice().createBuffer({ "OneConfig UI blur SamplerInfo" }, GpuBuffer.USAGE_UNIFORM, data)
    }

    private fun draw(pass: PostPass, input: GpuTextureView, output: RenderTarget, info: GpuBuffer) {
        val access = pass as PostPassAccessor
        val view = output.colorTextureView ?: return
        val sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
        //? if >= 26.2 {
        val clear = Optional.empty<Vector4fc>()
        //?} else
        //val clear = OptionalInt.empty()
        RenderSystem.getDevice().createCommandEncoder().createRenderPass({ "OneConfig UI blur" }, view, clear).use { rp ->
            //? if >= 26.3 {
            rp.setPipeline(RenderSystem.getCompiledPipeline(access.`oneconfig$getPipeline`()))
            //?} else
            //rp.setPipeline(access.`oneconfig$getPipeline`())
            RenderSystem.bindDefaultUniforms(rp)
            rp.setUniform("SamplerInfo", info)
            for ((name, buffer) in access.`oneconfig$getCustomUniforms`()) rp.setUniform(name, buffer)
            //? if >= 26.3 {
            rp.setUniform("InSampler", input, sampler)
            //?} else
            //rp.bindTexture("InSampler", input, sampler)
            //? if >= 26.2 {
            rp.draw(3, 1, 0, 0)
            //?} else
            //rp.draw(0, 3)
        }
    }
}
//?}
