@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.mojang.blaze3d.pipeline.RenderTarget
import java.util.UUID
import kotlin.math.ceil
import net.minecraft.client.Minecraft
import org.polyfrost.compose.backend.drawPoly
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.components.PlayerHead
import org.polyfrost.oneconfig.internal.ui.PreviewNameTag
import org.slf4j.LoggerFactory

//? if > 1.8.9 {
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.entity.player.Player
import org.joml.Quaternionf
//?} else {
/*import com.mojang.blaze3d.platform.GlStateManager
import net.minecraft.client.render.platform.GLX
import net.minecraft.client.render.platform.Lighting
import net.minecraft.entity.living.player.PlayerEntity as Player
import org.lwjgl.opengl.GL11
*///?}

//? if >= 26.1 {
import net.minecraft.client.renderer.state.level.CameraRenderState
//?} elif >= 1.21.10
//import net.minecraft.client.renderer.state.CameraRenderState

//? if >= 1.21.10 {
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.world.phys.Vec3
//?}

//? if >= 26.2
import net.minecraft.client.renderer.SubmitNodeStorage

//? if >= 26.3 {
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher
import java.util.Optional
import java.util.OptionalDouble
//?}

private const val MAX_DIM = 1024
private const val FULL_BRIGHT = 0xF000F0
private const val OFFSET_Y = 0.0625f
private const val HEIGHT_CATCHUP = 12f
private const val IDLE_NANOS = 250_000_000L
private const val FIT_HEIGHT = 2.1f
private const val FIT_WIDTH = 1.1f
private const val NAME_TAG_HEIGHT = 0.6f
private const val FALLBACK_HEAD_FRACTION = 0.5f

private val MinPreviewWidth = 48.dp
private val MinPreviewHeight = 96.dp

@ExperimentalOneConfigUiApi
@Composable
fun EntityPreview(
    playerId: UUID?,
    modifier: Modifier = Modifier,
    yaw: Float = 0f,
    pitch: Float = 0f,
    scale: Float = 1f,
    showNameTag: Boolean = false,
) {
    val slot = remember { PreviewSlot(playerId) }
    SideEffect {
        if (slot.playerId != playerId) slot.retarget(playerId)
        slot.yaw = yaw
        slot.pitch = pitch
        slot.scale = scale
        slot.showNameTag = showNameTag
    }
    DisposableEffect(slot) {
        EntityPreviews.register(slot)
        onDispose { EntityPreviews.unregister(slot) }
    }
    val frameNanos by produceState(0L) { while (true) withFrameNanos { value = it } }

    BoxWithConstraints(
        modifier.defaultMinSize(MinPreviewWidth, MinPreviewHeight),
        contentAlignment = Alignment.Center,
    ) {
        if (slot.available) {
            Box(
                Modifier.fillMaxSize().drawBehind {
                    if (frameNanos == 0L) return@drawBehind
                    val w = size.width
                    val h = size.height
                    slot.layoutWidth = w
                    slot.layoutHeight = h
                    drawPoly { slot.draw(it, w, h) }
                },
            )
        } else {
            val headId = playerId ?: localProfileId()
            if (headId != null) {
                val w = if (constraints.hasBoundedWidth) maxWidth else minWidth
                val h = if (constraints.hasBoundedHeight) maxHeight else minHeight
                PlayerHead(headId, min(w, h) * FALLBACK_HEAD_FRACTION)
            }
        }
    }
}

private class PreviewSlot(initialId: UUID?) {
    var playerId: UUID? = initialId
        private set
    var yaw = 0f
    var pitch = 0f
    var scale = 1f
    var showNameTag = false

    var available by mutableStateOf(findPlayer(initialId) != null)

    var layoutWidth = 0f
    var layoutHeight = 0f
    var lastDrawNanos = 0L
    var failed = false

    var anchorHeight = -1f
    var lastAnchorNanos = 0L

    var offscreen: PolyOffscreen? = null

    fun retarget(id: UUID?) {
        playerId = id
        failed = false
        anchorHeight = -1f
        lastAnchorNanos = 0L
    }

    fun draw(ctx: RenderContext, w: Float, h: Float) {
        lastDrawNanos = System.nanoTime()
        val target = offscreen ?: return
        with(target) { ctx.blit(0f, 0f, w, h) }
    }

    fun release() {
        offscreen?.close()
        offscreen = null
    }

    fun smoothAnchorHeight(target: Float): Float {
        val now = System.nanoTime()
        val previous = anchorHeight
        val elapsed = if (lastAnchorNanos == 0L) 0.0 else (now - lastAnchorNanos) / 1_000_000_000.0
        lastAnchorNanos = now
        if (previous <= 0f || elapsed <= 0.0 || elapsed > 0.5) {
            anchorHeight = target
            return target
        }
        val step = (1.0 - Math.exp(-HEIGHT_CATCHUP * elapsed)).toFloat()
        anchorHeight = previous + (target - previous) * step
        return anchorHeight
    }

    fun modelSize(width: Int, height: Int): Float {
        val tag = if (showNameTag) NAME_TAG_HEIGHT else 0f
        return scale * minOf(height / (FIT_HEIGHT + tag), width / FIT_WIDTH)
    }
}

private object EntityPreviews {
    private val LOGGER = LoggerFactory.getLogger("OneConfig/EntityPreview")

    private val slots = LinkedHashSet<PreviewSlot>()
    private var hook: AutoCloseable? = null

    fun register(slot: PreviewSlot) {
        slots += slot
        if (hook == null) hook = PolyOffscreen.onBeforePresent(::renderAll)
    }

    fun unregister(slot: PreviewSlot) {
        slots -= slot
        slot.release()
        if (slots.isEmpty()) {
            hook?.close()
            hook = null
        }
    }

    private fun renderAll() {
        if (slots.isEmpty()) return
        val now = System.nanoTime()
        val ratio = Platform.screen().surfaceRatio().coerceAtLeast(0.0001f)
        val partialTick = partialTick()
        for (slot in slots.toList()) {
            val player = if (slot.failed) null else findPlayer(slot.playerId)
            val available = player != null
            if (slot.available != available) slot.available = available
            if (player == null) {
                slot.release()
                continue
            }
            if (now - slot.lastDrawNanos > IDLE_NANOS) continue
            val requestedWidth = ceil(slot.layoutWidth * ratio).toInt()
            val requestedHeight = ceil(slot.layoutHeight * ratio).toInt()
            if (requestedWidth <= 0 || requestedHeight <= 0) continue
            val fit = minOf(1f, MAX_DIM.toFloat() / requestedWidth, MAX_DIM.toFloat() / requestedHeight)
            val width = (requestedWidth * fit).toInt().coerceIn(1, MAX_DIM)
            val height = (requestedHeight * fit).toInt().coerceIn(1, MAX_DIM)

            val offscreen = slot.offscreen ?: PolyOffscreen(width, height).also { slot.offscreen = it }
            offscreen.resize(width, height)
            try {
                offscreen.render {
                    val rt = offscreen.renderTarget ?: return@render
                    renderPlayer(slot, rt, player, partialTick)
                }
            } catch (t: Throwable) {
                LOGGER.warn("Entity preview render failed; showing the player head instead", t)
                slot.failed = true
                slot.available = false
                slot.release()
            }
        }
    }
}

private fun findPlayer(id: UUID?): Player? {
    val client = Minecraft.getInstance()
    val level = client.level ?: return null
    if (id == null) return client.player
    //? if > 1.8.9 {
    return level.getPlayerByUUID(id)
    //?} else
    //return level.getPlayer(id)
}

private fun localProfileId(): UUID? = runCatching { Minecraft.getInstance().user.profileId }.getOrNull()

private fun partialTick(): Float {
    val client = Minecraft.getInstance()
    //? if >= 1.21.4 {
    return client.deltaTracker.getGameTimeDeltaPartialTick(false)
    //?} elif > 1.8.9 {
    /*return client.timer.getGameTimeDeltaPartialTick(false)
    *///?} else
    //return 1f
}

//? if >= 1.21.10 {
private fun renderPlayer(slot: PreviewSlot, rt: RenderTarget, player: Player, partialTick: Float) {
    val client = Minecraft.getInstance()
    val width = rt.width
    val height = rt.height

    PreviewNameTag.rendering = true
    PreviewNameTag.show = slot.showNameTag
    val state = try {
        client.entityRenderDispatcher.extractEntity(player, partialTick) as? AvatarRenderState ?: return
    } finally {
        PreviewNameTag.rendering = false
        PreviewNameTag.show = false
    }

    state.lightCoords = FULL_BRIGHT
    if (!slot.showNameTag) state.nameTag = null
    state.bodyRot = 180f + slot.yaw
    state.yRot = 0f
    state.xRot = 0f
    val boxHeight = state.boundingBoxHeight / state.scale
    state.boundingBoxWidth /= state.scale
    state.boundingBoxHeight = boxHeight
    state.scale = 1f

    val anchor = slot.smoothAnchorHeight(boxHeight)
    val tag = if (slot.showNameTag) NAME_TAG_HEIGHT else 0f
    val size = slot.modelSize(width, height)
    val tilt = Math.toRadians(slot.pitch.toDouble()).toFloat()
    val rotation = Quaternionf().rotateZ(Math.PI.toFloat())
    if (tilt != 0f) rotation.mul(Quaternionf().rotateX(tilt))

    val pose = PoseStack()
    pose.translate(width / 2f, height / 2f, 0f)
    pose.scale(size, size, -size)
    pose.translate(0f, (anchor + tag) / 2f + OFFSET_Y, 0f)
    //? if >= 26.3 {
    pose.rotate(rotation)
    //? } else {
    /*pose.mulPose(rotation)
    *///? }

    //? if >= 26.2 {
    client.gameRenderer.lighting().setupFor(Lighting.Entry.ENTITY_IN_UI)
    //? } else {
    /*client.gameRenderer.lighting.setupFor(Lighting.Entry.ENTITY_IN_UI)
    *///? }

    val camera = CameraRenderState().apply {
        orientation = Quaternionf().rotateY(Math.PI.toFloat()).apply { if (tilt != 0f) mul(Quaternionf().rotateX(tilt)) }
        pos = Vec3.ZERO
        //? if < 26.1 {
        /*entityPos = Vec3.ZERO
        *///? }
    }

    //? if >= 26.3 {
    val colorView = rt.colorTextureView ?: return
    val depthView = rt.depthTextureView ?: return
    val storage = SubmitNodeStorage()
    client.entityRenderDispatcher.submit(state, camera, 0.0, 0.0, 0.0, pose, storage)
    client.gameRenderer.featureRenderDispatcher().prepareFrame(storage).use { frame ->
        RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            { "oneconfig_entity_preview" },
            colorView,
            Optional.empty(),
            depthView,
            OptionalDouble.empty(),
        ).use { pass ->
            RenderSystem.bindDefaultUniforms(pass)
            FeatureRenderDispatcher.renderAllFeatures(pass, frame)
        }
    }
    //? } else if >= 26.2 {
    /*val storage = SubmitNodeStorage()
    client.entityRenderDispatcher.submit(state, camera, 0.0, 0.0, 0.0, pose, storage)
    client.gameRenderer.featureRenderDispatcher().renderAllFeatures(storage)
    *///? } else {
    /*val features = client.gameRenderer.featureRenderDispatcher
    client.entityRenderDispatcher.submit(state, camera, 0.0, 0.0, 0.0, pose, features.submitNodeStorage)
    features.renderAllFeatures()
    client.renderBuffers().bufferSource().endBatch()
    *///? }
}
//?} else if > 1.8.9 {
/*private fun renderPlayer(slot: PreviewSlot, rt: RenderTarget, player: Player, partialTick: Float) {
    val client = Minecraft.getInstance()
    val width = rt.width
    val height = rt.height
    val entityScale = player.scale.coerceAtLeast(0.0001f)
    val bodyRot = 180f + slot.yaw

    val anchor = slot.smoothAnchorHeight(player.bbHeight / entityScale)
    val tag = if (slot.showNameTag) NAME_TAG_HEIGHT else 0f
    val size = slot.modelSize(width, height)
    val modelScale = size / entityScale
    val tilt = Math.toRadians(slot.pitch.toDouble()).toFloat()
    val cameraRotation = Quaternionf().rotateX(tilt)
    val rotation = Quaternionf().rotateZ(Math.PI.toFloat()).mul(cameraRotation)

    val pose = PoseStack()
    pose.translate(width / 2f, height / 2f, 0f)
    pose.scale(modelScale, modelScale, -modelScale)
    pose.translate(0f, entityScale * ((anchor + tag) / 2f + OFFSET_Y), 0f)
    pose.mulPose(rotation)

    val savedBodyRot = player.yBodyRot
    val savedBodyRotO = player.yBodyRotO
    val savedHeadRot = player.yHeadRot
    val savedHeadRotO = player.yHeadRotO
    val savedYRot = player.yRot
    val savedXRot = player.xRot
    val savedXRotO = player.xRotO

    val dispatcher = client.entityRenderDispatcher
    val buffers = client.renderBuffers().bufferSource()

    PreviewNameTag.rendering = true
    PreviewNameTag.show = slot.showNameTag
    player.yBodyRot = bodyRot
    player.yBodyRotO = bodyRot
    player.yHeadRot = bodyRot
    player.yHeadRotO = bodyRot
    player.yRot = bodyRot
    player.xRot = 0f
    player.xRotO = 0f
    //? if >= 1.21.8 {
    client.gameRenderer.lighting.setupFor(Lighting.Entry.ENTITY_IN_UI)
    //?} else
    //Lighting.setupForEntityInInventory()
    dispatcher.overrideCameraOrientation(cameraRotation.conjugate(Quaternionf()).rotateY(Math.PI.toFloat()))
    dispatcher.setRenderShadow(false)

    //? if = 1.21.8 {
    val savedHitboxes = dispatcher.shouldRenderHitBoxes()
    dispatcher.setRenderHitBoxes(false)
    //?}

    try {
        //? if >= 1.21.4 {
        dispatcher.render(player, 0.0, 0.0, 0.0, partialTick, pose, buffers, FULL_BRIGHT)
        //?} else
        //RenderSystem.runAsFancy { dispatcher.render(player, 0.0, 0.0, 0.0, 0f, partialTick, pose, buffers, FULL_BRIGHT) }
        buffers.endBatch()
    } finally {
        dispatcher.setRenderShadow(true)
        //? if = 1.21.8
        dispatcher.setRenderHitBoxes(savedHitboxes)
        //? if < 1.21.8
        //Lighting.setupFor3DItems()
        player.yBodyRot = savedBodyRot
        player.yBodyRotO = savedBodyRotO
        player.yHeadRot = savedHeadRot
        player.yHeadRotO = savedHeadRotO
        player.yRot = savedYRot
        player.xRot = savedXRot
        player.xRotO = savedXRotO
        PreviewNameTag.rendering = false
        PreviewNameTag.show = false
    }
}
*///?} else {
/*private fun renderPlayer(slot: PreviewSlot, rt: RenderTarget, player: Player, partialTick: Float) {
    val client = Minecraft.getInstance()
    val width = rt.width
    val height = rt.height
    val bodyRot = slot.yaw

    val anchor = slot.smoothAnchorHeight(player.height)
    val tag = if (slot.showNameTag) NAME_TAG_HEIGHT else 0f
    val size = slot.modelSize(width, height)

    val savedBodyRot = player.bodyYaw
    val savedBodyRotO = player.lastBodyYaw
    val savedHeadRot = player.headYaw
    val savedHeadRotO = player.lastHeadYaw
    val savedYRot = player.yaw
    val savedYRotO = player.lastYaw
    val savedXRot = player.pitch
    val savedXRotO = player.lastPitch

    val dispatcher = client.entityRenderDispatcher

    PreviewNameTag.rendering = true
    PreviewNameTag.show = slot.showNameTag
    player.bodyYaw = bodyRot
    player.lastBodyYaw = bodyRot
    player.headYaw = bodyRot
    player.lastHeadYaw = bodyRot
    player.yaw = bodyRot
    player.lastYaw = bodyRot
    player.pitch = 0f
    player.lastPitch = 0f

    GL11.glEnable(GL11.GL_NORMALIZE)
    GlStateManager.enableColorMaterial()
    GlStateManager.pushMatrix()
    try {
        GlStateManager.translatef(width / 2f, height / 2f, 50f)
        GlStateManager.scalef(-size, size, size)
        GlStateManager.translatef(0f, (anchor + tag) / 2f + OFFSET_Y, 0f)
        GlStateManager.rotatef(180f, 0f, 0f, 1f)
        if (slot.pitch != 0f) GlStateManager.rotatef(slot.pitch, 1f, 0f, 0f)
        GlStateManager.rotatef(135f, 0f, 1f, 0f)
        Lighting.turnOn()
        GlStateManager.rotatef(-135f, 0f, 1f, 0f)
        dispatcher.setCameraYaw(180f)
        dispatcher.setRenderShadow(false)
        dispatcher.render(player, 0.0, 0.0, 0.0, 0f, partialTick, true)
    } finally {
        dispatcher.setRenderShadow(true)
        GlStateManager.popMatrix()
        Lighting.turnOff()
        GlStateManager.disableRescaleNormal()
        GlStateManager.activeTexture(GLX.GL_TEXTURE1)
        GlStateManager.disableTexture()
        GlStateManager.activeTexture(GLX.GL_TEXTURE0)
        GL11.glDisable(GL11.GL_NORMALIZE)
        player.bodyYaw = savedBodyRot
        player.lastBodyYaw = savedBodyRotO
        player.headYaw = savedHeadRot
        player.lastHeadYaw = savedHeadRotO
        player.yaw = savedYRot
        player.lastYaw = savedYRotO
        player.pitch = savedXRot
        player.lastPitch = savedXRotO
        PreviewNameTag.rendering = false
        PreviewNameTag.show = false
    }
}
*///?}
