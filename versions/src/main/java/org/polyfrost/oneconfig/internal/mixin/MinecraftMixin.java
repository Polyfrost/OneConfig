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

package org.polyfrost.oneconfig.internal.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.FramebufferRenderEvent;
import org.polyfrost.oneconfig.api.event.v1.events.KeyInputEvent;
import org.polyfrost.oneconfig.api.event.v1.events.MouseInputEvent;
import org.polyfrost.oneconfig.api.event.v1.events.RenderEvent;
import org.polyfrost.oneconfig.api.event.v1.events.ResizeEvent;
import org.polyfrost.oneconfig.api.event.v1.events.ShutdownEvent;
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC<=11202
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
//#endif

//#if FORGE
//#if MC<11900
import net.minecraftforge.client.event.GuiOpenEvent;
//#else
//$$ import net.minecraftforge.client.event.ScreenEvent.Opening;
//#endif
import net.minecraftforge.fml.common.eventhandler.Event;
//#else
//$$ import org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent;
//#endif

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    private Timer timer;

    @Unique
    private static final String UPDATE_CAMERA_AND_RENDER =
            //#if MC>=11300
            //$$ "Lnet/minecraft/client/renderer/GameRenderer;updateCameraAndRender(FJZ)V";
            //#else
            "Lnet/minecraft/client/renderer/EntityRenderer;updateCameraAndRender(FJ)V";
    //#endif

    @Inject(method = "shutdownMinecraftApplet", at = @At("HEAD"))
    private void ocfg$shutdownCallback(CallbackInfo ci) {
        EventManager.INSTANCE.post(ShutdownEvent.INSTANCE);
    }

    //#if MC<=11300
    @Inject(method = "resize", at = @At("HEAD"))
    private void ocfg$resizeCallback(int width, int height, CallbackInfo ci) {
        EventManager.INSTANCE.post(new ResizeEvent(width, height));
    }
    //#else
    //$$ @Shadow
    //$$ public abstract net.minecraft.client.MainWindow getMainWindow();
    //$$
    //$$ @Inject(method = "updateWindowSize", at = @At("HEAD"))
    //$$ private void ocfg$resizeCallback(CallbackInfo ci) {
    //$$     int[] w = new int[1];
    //$$     int[] h = new int[1];
    //$$     org.lwjgl.glfw.GLFW.glfwGetWindowSize(this.getMainWindow().getHandle(), w, h);
    //$$     EventManager.INSTANCE.post(new ResizeEvent(w[0], h[0]));
    //$$ }
    //#endif

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = UPDATE_CAMERA_AND_RENDER))
    private void ocfg$renderTickStartCallback(CallbackInfo ci) {
        RenderEvent e = RenderEvent.Start.INSTANCE;
        e.deltaTicks = this.timer.renderPartialTicks;
        EventManager.INSTANCE.post(e);
    }

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = UPDATE_CAMERA_AND_RENDER, shift = At.Shift.AFTER))
    private void ocfg$renderTickEndCallback(CallbackInfo ci) {
        RenderEvent e = RenderEvent.End.INSTANCE;
        e.deltaTicks = this.timer.renderPartialTicks;
        EventManager.INSTANCE.post(e);
    }

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/shader/Framebuffer;framebufferRender(II)V"))
    private void ocfg$preFramebufferRenderCallback(CallbackInfo ci) {
        EventManager.INSTANCE.post(FramebufferRenderEvent.Start.INSTANCE);
    }

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/shader/Framebuffer;framebufferRender(II)V", shift = At.Shift.AFTER))
    private void ocfg$postFramebufferRenderCallback(CallbackInfo ci) {
        EventManager.INSTANCE.post(FramebufferRenderEvent.End.INSTANCE);
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;startSection(Ljava/lang/String;)V", ordinal = 0))
    private void ocfg$tickStartCallback(CallbackInfo ci) {
        EventManager.INSTANCE.post(TickEvent.Start.INSTANCE);
    }

    @Inject(method = "runTick", at = @At("TAIL"))
    private void ocfg$tickEndCallback(CallbackInfo ci) {
        EventManager.INSTANCE.post(TickEvent.End.INSTANCE);
    }

    //#if FORGE
    @ModifyArg(method =
            //#if MC>11700
            //$$ "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/eventbus/api/IEventBus;post(Lnet/minecraftforge/eventbus/api/Event;)Z"
            //#else
            "displayGuiScreen", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/eventhandler/EventBus;post(Lnet/minecraftforge/fml/common/eventhandler/Event;)Z"
            //#endif
            , remap = false))
    private Event ocfg$screenOpenCallback(Event a) {
        if (a instanceof
                //#if MC<11900
                GuiOpenEvent
                //#else
                //$$ Opening
                //#endif
        ) {
            // w: not imported because 1.18+ they renamed it to be the same (breh)
            //#if MC<11900
            GuiOpenEvent forgeEvent = (GuiOpenEvent) a;
            //#else
            //$$ Opening forgeEvent = (Opening) a;
            //#endif
            org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent event =
                    new org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent(forgeEvent.
                            //#if MC<=10809
                            gui
                            //#else
                            //#if MC<11900
                            //$$ getGui()
                            //#else
                            //$$ getNewScreen()
                            //#endif
                            //#endif
                    );
            EventManager.INSTANCE.post(event);
            if (event.cancelled) {
                forgeEvent.setCanceled(true);
            }
            return forgeEvent;
        }
        return a;
    }
    //#else
    //$$  @Inject(method = "openScreen", at = @At(value = "INVOKE", target =
    //#if MC>=11300
    //$$  "Lnet/minecraft/client/network/ClientPlayerEntity;requestRespawn()V", shift = At.Shift.BY, by = 2
    //#elseif MC>=11200
    //$$  "Lnet/minecraft/client/gui/screen/DeathScreen;<init>(Lnet/minecraft/text/Text;)V", shift = At.Shift.BY, by = 3
    //#else
    //$$  "Lnet/minecraft/client/gui/screen/DeathScreen;<init>()V", shift = At.Shift.BY, by = 3
    //#endif
    //$$  ), cancellable = true)
    //$$  private void ocfg$screenOpenCallback(net.minecraft.client.gui.screen.Screen screen, CallbackInfo ci) {
    //$$      ScreenOpenEvent event = new ScreenOpenEvent(screen);
    //$$      EventManager.INSTANCE.post(event);
    //$$      if (event.cancelled) {
    //$$          ci.cancel();
    //$$      }
    //$$  }
    //#endif


    //#if MC<=11202
    //#if FORGE
    //#if MC<=10809
    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/settings/KeyBinding;setKeyBindState(IZ)V", ordinal = 1))
    private void ocfg$keyCallback(CallbackInfo ci) {
        int state = 0;
        if (Keyboard.getEventKeyState()) {
            if (Keyboard.isRepeatEvent()) {
                state = 2;
            } else {
                state = 1;
            }
        }
        EventManager.INSTANCE.post(new KeyInputEvent(Keyboard.getEventKey(), Keyboard.getEventCharacter(), state));
    }


    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;postMouseEvent()Z", remap = false))
    private void ocfg$mouseCallback(CallbackInfo ci) {
        EventManager.INSTANCE.post(new MouseInputEvent(Mouse.getEventButton(), Mouse.getEventButtonState() ? 1 : 0));
    }

    //#else
    //$$ @Inject(method = "runTickKeyboard", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;debugCrashKeyPressTime:J", opcode = org.objectweb.asm.Opcodes.PUTFIELD))
    //$$ private void ocfg$keyCallback(CallbackInfo ci) {
    //$$     int state = 0;
    //$$     if (Keyboard.getEventKeyState()) {
    //$$         if (Keyboard.isRepeatEvent()) {
    //$$             state = 2;
    //$$         } else {
    //$$             state = 1;
    //$$         }
    //$$     }
    //$$     EventManager.INSTANCE.post(new KeyInputEvent(Keyboard.getEventKey(), Keyboard.getEventCharacter(), state));
    //$$ }
    //$$
    //$$ @Inject(method = "runTickMouse", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;postMouseEvent()Z", remap = false), remap = true)
    //$$ private void ocfg$mouseCallback(CallbackInfo ci) {
    //$$     EventManager.INSTANCE.post(new MouseInputEvent(Mouse.getEventButton(), Mouse.getEventButtonState() ? 1 : 0));
    //$$ }
    //$$
    //#endif
    //#else
    //#if MC<=10809
    //$$ @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/options/KeyBinding;setKeyPressed(IZ)V", ordinal = 1))
    //$$ private void ocfg$keyCallback(CallbackInfo ci) {
    //$$     int state = 0;
    //$$     if (Keyboard.getEventKeyState()) {
    //$$         if (Keyboard.isRepeatEvent()) {
    //$$             state = 2;
    //$$         } else {
    //$$             state = 1;
    //$$         }
    //$$     }
    //$$     EventManager.INSTANCE.post(new KeyInputEvent(Keyboard.getEventKey(), Keyboard.getEventCharacter(), state));
    //$$ }
    //$$
    //$$ @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventButton()I", remap = false))
    //$$ private void ocfg$mouseCallback(CallbackInfo ci) {
    //$$     EventManager.INSTANCE.post(new MouseInputEvent(Mouse.getEventButton(), Mouse.getEventButtonState() ? 1 : 0));
    //$$ }
    //$$
    //#else
    //$$ @Inject(method = "method_12145", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;f3CTime:J", opcode = org.objectweb.asm.Opcodes.PUTFIELD))
    //$$ private void ocfg$keyCallback(CallbackInfo ci) {
    //$$     int state = 0;
    //$$     if (Keyboard.getEventKeyState()) {
    //$$         if (Keyboard.isRepeatEvent()) {
    //$$             state = 2;
    //$$         } else {
    //$$             state = 1;
    //$$         }
    //$$     }
    //$$     EventManager.INSTANCE.post(new KeyInputEvent(Keyboard.getEventKey(), Keyboard.getEventCharacter(), state));
    //$$ }
    //$$
    //$$ @Inject(method = "method_12141", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/options/KeyBinding;setKeyPressed(IZ)V"))
    //$$ private void ocfg$mouseCallback(CallbackInfo ci) {
    //$$     EventManager.INSTANCE.post(new MouseInputEvent(Mouse.getEventButton(), Mouse.getEventButtonState() ? 1 : 0));
    //$$ }
    //$$
    //#endif
    //#endif
    //#endif
}