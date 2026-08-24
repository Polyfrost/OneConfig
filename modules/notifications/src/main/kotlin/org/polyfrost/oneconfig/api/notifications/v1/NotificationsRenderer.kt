/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.api.notifications.v1

import androidx.compose.runtime.snapshots.Snapshot
import org.apache.logging.log4j.LogManager
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.compose.node.PolyNode
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.compose.runtime.PolyComposeRuntime
import org.polyfrost.oneconfig.api.platform.v1.Platform

@ApiStatus.Internal
object NotificationsRenderer {
    private val LOGGER = LogManager.getLogger("OneConfig/Notifications")

    private const val BASE_SCALE = 0.5f

    @Volatile
    private var runtime: PolyComposeRuntime? = null

    private fun runtime(): PolyComposeRuntime =
        runtime ?: PolyComposeRuntime().also {
            runtime = it
            it.setContent { NotificationToasts() }
            ToastInput.install()
        }

    /**
     * Renders the toast stack into [ctx]
     */
    @JvmStatic
    fun render(ctx: RenderContext, screenWidth: Float, screenHeight: Float) {
        val guiScale = Platform.compatibility().options().guiScale
        val scale = guiScale * BASE_SCALE
        val viewWidth = screenWidth * guiScale / scale
        val viewHeight = screenHeight * guiScale / scale
        ToastViewport.width = viewWidth
        ToastViewport.height = viewHeight
        ToastViewport.scale = scale

        val rt = runtime()
        rt.frame(viewWidth, viewHeight)
        handleInput(rt.root, scale)

        ctx.save()
        ctx.scale(scale, scale)
        rt.root.render(ctx)
        ctx.restore()
    }

    private fun handleInput(root: PolyNode, scale: Float) {
        for (notification in NotificationsManager.active) notification.hovered = false
        ToastInput.hoveredAction = null
        ToastInput.hoverTarget = null

        val screenOpen = Platform.screen().current<Any?>() != null
        val mx = ToastInput.mouseX / scale
        val my = ToastInput.mouseY / scale
        if (!screenOpen || ToastInput.mouseX < 0f || ToastInput.mouseY < 0f) return

        val hits = ArrayList<PolyNode>()
        collectHits(root, hits)

        var top: ToastHit? = null
        for (node in hits) {
            val tag = node.style.tag as? ToastHit ?: continue
            if (!contains(node, mx, my)) continue
            tag.notification.hovered = true
            top = tag
        }

        ToastInput.hoverTarget = top
        if (top is ToastHit.Action) ToastInput.hoveredAction = top.action
    }

    internal fun dispatchClick(target: ToastHit?) {
        if (target == null) return
        if (!NotificationsManager.contains(target.notification) || target.notification.dismissRequested) {
            ToastInput.clearHover()
            return
        }
        when (target) {
            is ToastHit.Action -> {
                LOGGER.info("Notification #{} action '{}' clicked", target.notification.id, target.action.label)
                runCatching { target.action.onClick.run() }
                    .onFailure { LOGGER.error("Notification action '${target.action.label}' threw", it) }
                NotificationsManager.remove(target.notification)
            }
            is ToastHit.Body -> {
                LOGGER.info("Notification #{} body clicked", target.notification.id)
                runCatching { target.notification.onClick?.run() }
                    .onFailure { LOGGER.error("Notification onClick threw", it) }
                target.notification.dismissRequested = true
            }
        }
    }

    private fun collectHits(node: PolyNode, out: MutableList<PolyNode>) {
        if (node.style.tag is ToastHit) out.add(node)
        for (child in node.children) collectHits(child, out)
    }

    private fun contains(node: PolyNode, x: Float, y: Float): Boolean =
        x >= node.x && x <= node.x + node.width && y >= node.y && y <= node.y + node.height
}
