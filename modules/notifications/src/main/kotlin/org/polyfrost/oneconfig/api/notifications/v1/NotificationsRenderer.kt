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

import org.apache.logging.log4j.LogManager
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.compose.node.PolyNode
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.compose.runtime.PolyComposeHost
import org.polyfrost.compose.runtime.PolyComposeRuntime
import org.polyfrost.oneconfig.api.platform.v1.Platform

@ApiStatus.Internal
object NotificationsRenderer {
    private val LOGGER = LogManager.getLogger("OneConfig/Notifications")

    private const val BASE_SCALE = 0.5f

    private val clock = PolyComposeHost.notifications

    private var runtime: PolyComposeRuntime? = null

    private var lastViewWidth = 0f
    private var lastViewHeight = 0f

    private fun runtime(): PolyComposeRuntime =
        runtime ?: PolyComposeRuntime(clock).also {
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
        clock.frame()
        if (clock.appliedChange || viewWidth != lastViewWidth || viewHeight != lastViewHeight) {
            rt.layout(viewWidth, viewHeight)
            lastViewWidth = viewWidth
            lastViewHeight = viewHeight
        }
        handleInput(rt.root, scale)

        ctx.save()
        ctx.scale(scale, scale)
        rt.root.render(ctx)
        ctx.restore()
    }

    private fun handleInput(root: PolyNode, scale: Float) {
        val screenOpen = Platform.screen().current<Any?>() != null
        var top: ToastHit? = null

        if (screenOpen && ToastInput.mouseX >= 0f && ToastInput.mouseY >= 0f) {
            val pixelRatio = Platform.screen().pixelRatio()
            top = topHit(root, ToastInput.mouseX * pixelRatio / scale, ToastInput.mouseY * pixelRatio / scale, null)
        }

        val hovered = top?.notification
        for (notification in NotificationsManager.active) {
            val isHovered = notification === hovered
            if (notification.hovered != isHovered) notification.hovered = isHovered
        }

        ToastInput.hoverTarget = top
        val hoveredAction = (top as? ToastHit.Action)?.action
        if (ToastInput.hoveredAction !== hoveredAction) ToastInput.hoveredAction = hoveredAction
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

    private fun topHit(node: PolyNode, x: Float, y: Float, current: ToastHit?): ToastHit? {
        var top = current
        val tag = node.style.tag
        if (tag is ToastHit && contains(node, x, y)) top = tag
        for (child in node.children) top = topHit(child, x, y, top)
        return top
    }

    private fun contains(node: PolyNode, x: Float, y: Float): Boolean =
        x >= node.x && x <= node.x + node.width && y >= node.y && y <= node.y + node.height
}
