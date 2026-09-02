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

@file:JvmSynthetic
@file:OptIn(ExperimentalTypeInference::class)

package org.polyfrost.oneconfig.api.event.v1

import org.polyfrost.oneconfig.api.event.v1.events.ChatEvent
import org.polyfrost.oneconfig.api.event.v1.events.Event
import org.polyfrost.oneconfig.api.event.v1.events.FramebufferRenderEvent
import org.polyfrost.oneconfig.api.event.v1.events.HudEvent
import org.polyfrost.oneconfig.api.event.v1.events.PacketEvent
import org.polyfrost.oneconfig.api.event.v1.events.RenderLivingEvent
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent
import org.polyfrost.oneconfig.api.event.v1.events.WindowFocusEvent
import org.polyfrost.oneconfig.api.event.v1.events.WorldEvent
import org.polyfrost.oneconfig.api.event.v1.invoke.EventHandler
import kotlin.experimental.ExperimentalTypeInference

/**
 * Kotlin specific API for registering event handlers
 *
 * Intended usage
 *
 * ```
 * eventHandler { event: KeyInputEvent ->
 *     println("Key event: $event")
 * }
 * ```
 */
@OverloadResolutionByLambdaReturnType
@JvmSynthetic
@JvmName("eventHandlerZ")
@EventHandlerKt
inline fun <reified E : Event> eventHandler(crossinline handler: (E) -> Boolean) = object : EventHandler<E>() {
    override fun handle(event: E) = handler(event)

    override fun getEventClass() = E::class.java
}.register()

/**
 * Kotlin specific API for registering event handlers
 *
 * Intended usage
 *
 * ```
 * eventHandler { event: KeyInputEvent ->
 *     println("Key event: $event")
 * }
 * ```
 */
@OverloadResolutionByLambdaReturnType
@EventHandlerKt
inline fun <reified E : Event> eventHandler(crossinline handler: (E) -> Unit) = object : EventHandler<E>() {
    override fun handle(event: E): Boolean {
        handler(event)
        return false
    }

    override fun getEventClass() = E::class.java
}.register()

@DslMarker
private annotation class EventHandlerKt

// Compile-time rejection of handlers for abstract parent event types, which are never posted
// directly and whose handlers would therefore never be called. These overloads are more specific
// than the generic ones above, so they win overload resolution and surface as compile errors.

private fun neverPosted(): Nothing =
    throw UnsupportedOperationException("This event type is never posted directly")

@Deprecated("Event is never posted directly; register for a concrete event type instead.", level = DeprecationLevel.ERROR)
@OverloadResolutionByLambdaReturnType
@JvmSynthetic
@JvmName("eventHandlerEventZ")
fun eventHandler(handler: (Event) -> Boolean): Nothing = neverPosted()

@Deprecated("Event is never posted directly; register for a concrete event type instead.", level = DeprecationLevel.ERROR)
@OverloadResolutionByLambdaReturnType
@JvmSynthetic
@JvmName("eventHandlerEvent")
fun eventHandler(handler: (Event) -> Unit): Nothing = neverPosted()

@Deprecated("Event.Cancellable is never posted directly; register for a concrete event type instead.", level = DeprecationLevel.ERROR)
@OverloadResolutionByLambdaReturnType
@JvmSynthetic
@JvmName("eventHandlerCancellableZ")
fun eventHandler(handler: (Event.Cancellable) -> Boolean): Nothing = neverPosted()

@Deprecated("Event.Cancellable is never posted directly; register for a concrete event type instead.", level = DeprecationLevel.ERROR)
@OverloadResolutionByLambdaReturnType
@JvmSynthetic
@JvmName("eventHandlerCancellable")
fun eventHandler(handler: (Event.Cancellable) -> Unit): Nothing = neverPosted()

@Deprecated("TickEvent is never posted directly; register for TickEvent.Start or TickEvent.End instead.", level = DeprecationLevel.ERROR)
@OverloadResolutionByLambdaReturnType
@JvmSynthetic
@JvmName("eventHandlerTickZ")
fun eventHandler(handler: (TickEvent) -> Boolean): Nothing = neverPosted()

@Deprecated("TickEvent is never posted directly; register for TickEvent.Start or TickEvent.End instead.", level = DeprecationLevel.ERROR)
@OverloadResolutionByLambdaReturnType
@JvmSynthetic
@JvmName("eventHandlerTick")
fun eventHandler(handler: (TickEvent) -> Unit): Nothing = neverPosted()

@Deprecated("WindowFocusEvent is never posted directly; register for WindowFocusEvent.Gained or WindowFocusEvent.Lost instead.", level = DeprecationLevel.ERROR)
@OverloadResolutionByLambdaReturnType
@JvmSynthetic
@JvmName("eventHandlerWindowFocusZ")
fun eventHandler(handler: (WindowFocusEvent) -> Boolean): Nothing = neverPosted()

@Deprecated("WindowFocusEvent is never posted directly; register for WindowFocusEvent.Gained or WindowFocusEvent.Lost instead.", level = DeprecationLevel.ERROR)
@OverloadResolutionByLambdaReturnType
@JvmSynthetic
@JvmName("eventHandlerWindowFocus")
fun eventHandler(handler: (WindowFocusEvent) -> Unit): Nothing = neverPosted()

@Deprecated("FramebufferRenderEvent is never posted directly; register for FramebufferRenderEvent.Start or FramebufferRenderEvent.End instead.", level = DeprecationLevel.ERROR)
@OverloadResolutionByLambdaReturnType
@JvmSynthetic
@JvmName("eventHandlerFramebufferRenderZ")
fun eventHandler(handler: (FramebufferRenderEvent) -> Boolean): Nothing = neverPosted()

@Deprecated("FramebufferRenderEvent is never posted directly; register for FramebufferRenderEvent.Start or FramebufferRenderEvent.End instead.", level = DeprecationLevel.ERROR)
@OverloadResolutionByLambdaReturnType
@JvmSynthetic
@JvmName("eventHandlerFramebufferRender")
fun eventHandler(handler: (FramebufferRenderEvent) -> Unit): Nothing = neverPosted()

@Deprecated("ChatEvent is never posted directly; register for ChatEvent.Send or ChatEvent.Receive instead.", level = DeprecationLevel.ERROR)
@OverloadResolutionByLambdaReturnType
@JvmSynthetic
@JvmName("eventHandlerChatZ")
fun eventHandler(handler: (ChatEvent) -> Boolean): Nothing = neverPosted()

@Deprecated("ChatEvent is never posted directly; register for ChatEvent.Send or ChatEvent.Receive instead.", level = DeprecationLevel.ERROR)
@OverloadResolutionByLambdaReturnType
@JvmSynthetic
@JvmName("eventHandlerChat")
fun eventHandler(handler: (ChatEvent) -> Unit): Nothing = neverPosted()

@Deprecated("HudEvent is never posted directly; register for HudEvent.Tab or HudEvent.Debug instead.", level = DeprecationLevel.ERROR)
@OverloadResolutionByLambdaReturnType
@JvmSynthetic
@JvmName("eventHandlerHudZ")
fun eventHandler(handler: (HudEvent) -> Boolean): Nothing = neverPosted()

@Deprecated("HudEvent is never posted directly; register for HudEvent.Tab or HudEvent.Debug instead.", level = DeprecationLevel.ERROR)
@OverloadResolutionByLambdaReturnType
@JvmSynthetic
@JvmName("eventHandlerHud")
fun eventHandler(handler: (HudEvent) -> Unit): Nothing = neverPosted()

@Deprecated("WorldEvent is never posted directly; register for WorldEvent.Load or WorldEvent.Unload instead.", level = DeprecationLevel.ERROR)
@OverloadResolutionByLambdaReturnType
@JvmSynthetic
@JvmName("eventHandlerWorldZ")
fun eventHandler(handler: (WorldEvent) -> Boolean): Nothing = neverPosted()

@Deprecated("WorldEvent is never posted directly; register for WorldEvent.Load or WorldEvent.Unload instead.", level = DeprecationLevel.ERROR)
@OverloadResolutionByLambdaReturnType
@JvmSynthetic
@JvmName("eventHandlerWorld")
fun eventHandler(handler: (WorldEvent) -> Unit): Nothing = neverPosted()

@Deprecated("PacketEvent is never posted directly; register for PacketEvent.Send or PacketEvent.Receive instead.", level = DeprecationLevel.ERROR)
@OverloadResolutionByLambdaReturnType
@JvmSynthetic
@JvmName("eventHandlerPacketZ")
fun eventHandler(handler: (PacketEvent) -> Boolean): Nothing = neverPosted()

@Deprecated("PacketEvent is never posted directly; register for PacketEvent.Send or PacketEvent.Receive instead.", level = DeprecationLevel.ERROR)
@OverloadResolutionByLambdaReturnType
@JvmSynthetic
@JvmName("eventHandlerPacket")
fun eventHandler(handler: (PacketEvent) -> Unit): Nothing = neverPosted()

@Deprecated("RenderLivingEvent is never posted directly; register for RenderLivingEvent.Pre or RenderLivingEvent.Post instead.", level = DeprecationLevel.ERROR)
@OverloadResolutionByLambdaReturnType
@JvmSynthetic
@JvmName("eventHandlerRenderLivingZ")
fun eventHandler(handler: (RenderLivingEvent) -> Boolean): Nothing = neverPosted()

@Deprecated("RenderLivingEvent is never posted directly; register for RenderLivingEvent.Pre or RenderLivingEvent.Post instead.", level = DeprecationLevel.ERROR)
@OverloadResolutionByLambdaReturnType
@JvmSynthetic
@JvmName("eventHandlerRenderLiving")
fun eventHandler(handler: (RenderLivingEvent) -> Unit): Nothing = neverPosted()