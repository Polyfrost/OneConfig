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

//#if FORGE
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
//#else
//$$ import org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent;
//#endif

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    private Timer timer;

    //@formatter:off
    @Unique
    private static final String UPDATE_CAMERA_AND_RENDER =
            //#if MC>=11300
            //$$ "Lnet/minecraft/client/renderer/GameRenderer;updateCameraAndRender(FJZ)V";
            //#else
            "Lnet/minecraft/client/renderer/EntityRenderer;updateCameraAndRender(FJ)V";
            //#endif
    //@formatter:on

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
    @ModifyArg(method = "displayGuiScreen", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/eventhandler/EventBus;post(Lnet/minecraftforge/fml/common/eventhandler/Event;)Z", remap = false))
    private Event ocfg$screenOpenCallback(Event a) {
        if (a instanceof GuiOpenEvent) {
            // w: not imported because 1.18+ they renamed it to be the same (breh)
            GuiOpenEvent forgeEvent = (GuiOpenEvent) a;
            org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent event =
                    new org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent(forgeEvent.
                            //#if MC<=10809
                            gui
                            //#else
                            //$$ getGui()
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

    //@formatter:off
    //#if MC<=10809
    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/settings/KeyBinding;setKeyBindState(IZ)V", ordinal = 1))
    //#else
    //#if FORGE
    //$$ @Inject(method = "runTickKeyboard", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;debugCrashKeyPressTime:J", opcode = org.objectweb.asm.Opcodes.PUTFIELD))
    //#else
    //$$ @Inject(method = "method_12145", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;f3CTime:J", opcode = org.objectweb.asm.Opcodes.PUTFIELD))
    //#endif
    //#endif
    //@formatter:on
    private void ocfg$keyCallback(CallbackInfo ci) {
        int state = 0;
        if (org.lwjgl.input.Keyboard.getEventKeyState()) {
            if (org.lwjgl.input.Keyboard.isRepeatEvent()) {
                state = 2;
            } else {
                state = 1;
            }
        }
        EventManager.INSTANCE.post(new KeyInputEvent(org.lwjgl.input.Keyboard.getEventKey(), org.lwjgl.input.Keyboard.getEventCharacter(), state));
    }


    //@formatter:off
    //#if FORGE
    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;postMouseEvent()Z", remap = false))
    //#else
    //#if MC==10809
    //$$ @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventButton()I", remap = false))
    //#else
    //$$ @Inject(method = "method_12141", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/options/KeyBinding;setKeyPressed(IZ)V"))
    //#endif
    //#endif
    //@formatter:on
    private void ocfg$mouseCallback(CallbackInfo ci) {
        EventManager.INSTANCE.post(new MouseInputEvent(org.lwjgl.input.Mouse.getEventButton(), org.lwjgl.input.Mouse.getEventButtonState() ? 1 : 0));
    }

    //#endif
}