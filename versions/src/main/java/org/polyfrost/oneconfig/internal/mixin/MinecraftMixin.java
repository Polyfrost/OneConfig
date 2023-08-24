/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
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
 * <https://polyfrost.cc/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.internal.mixin;

import org.polyfrost.oneconfig.events.EventManager;
import org.polyfrost.oneconfig.events.event.*;
import org.polyfrost.oneconfig.internal.OneConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC<=11202
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
//#endif

//#if FORGE==1
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
//#endif

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow
    private Timer timer;

    private static final String UPDATE_CAMERA_AND_RENDER =
            //#if MC>=11300
            //$$ "Lnet/minecraft/client/renderer/GameRenderer;updateCameraAndRender(FJZ)V";
            //#else
            "Lnet/minecraft/client/renderer/EntityRenderer;updateCameraAndRender(FJ)V";
            //#endif

    @Inject(method = "shutdownMinecraftApplet", at = @At("HEAD"))
    private void onShutdown(CallbackInfo ci) {
        EventManager.INSTANCE.post(new PreShutdownEvent());
    }

    //#if FORGE==1 && MC<=11202
    @Inject(method = "startGame", at = @At("HEAD"))
    private void onStart(CallbackInfo ci) {
        EventManager.INSTANCE.post(new StartEvent());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> EventManager.INSTANCE.post(new ShutdownEvent())));
    }

    @Inject(method = "startGame", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/client/FMLClientHandler;onInitializationComplete()V", shift = At.Shift.AFTER, remap = false), remap = true)
    private void onInit(CallbackInfo ci) {
        EventManager.INSTANCE.post(new InitializationEvent());
        OneConfig.INSTANCE.init();
    }
    //#endif

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = UPDATE_CAMERA_AND_RENDER))
    private void onRenderTickStart(CallbackInfo ci) {
        EventManager.INSTANCE.post(new RenderEvent(Stage.START, this.timer.renderPartialTicks));
    }

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = UPDATE_CAMERA_AND_RENDER, shift = At.Shift.AFTER))
    private void onRenderTickEnd(CallbackInfo ci) {
        EventManager.INSTANCE.post(new RenderEvent(Stage.END, this.timer.renderPartialTicks));
    }

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/shader/Framebuffer;framebufferRender(II)V"))
    private void onFramebufferStart(CallbackInfo ci) {
        EventManager.INSTANCE.post(new FramebufferRenderEvent(Stage.START));
    }

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/shader/Framebuffer;framebufferRender(II)V", shift = At.Shift.AFTER))
    private void onFramebufferEnd(CallbackInfo ci) {
        EventManager.INSTANCE.post(new FramebufferRenderEvent(Stage.END));
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;startSection(Ljava/lang/String;)V", ordinal = 0))
    private void onClientTickStart(CallbackInfo ci) {
        EventManager.INSTANCE.post(new TickEvent(Stage.START));
    }

    @Inject(method = "runTick", at = @At("TAIL"))
    private void onClientTickEnd(CallbackInfo ci) {
        EventManager.INSTANCE.post(new TickEvent(Stage.END));
    }

    //#if FORGE==1
    @ModifyArg(method = "displayGuiScreen", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/eventhandler/EventBus;post(Lnet/minecraftforge/fml/common/eventhandler/Event;)Z", remap = false), remap = true)
    private Event onGuiOpenEvent(Event a) {
        if (a instanceof GuiOpenEvent) {
            GuiOpenEvent forgeEvent = (GuiOpenEvent) a;
            ScreenOpenEvent event = new ScreenOpenEvent(forgeEvent.
                    //#if MC<=10809
                    gui
                    //#else
                    //$$ getGui()
                    //#endif
            );
            EventManager.INSTANCE.post(event);
            if (event.isCancelled) {
                forgeEvent.setCanceled(true);
            }
            return forgeEvent;
        }
        return a;
    }
    //#else
    //$$  @Inject(method = "openScreen", at = @At(
    //#if MC>=11300
    //$$  value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;requestRespawn()V", shift = At.Shift.BY, by = 2
    //#elseif MC>=11200
    //$$  value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/DeathScreen;<init>(Lnet/minecraft/text/Text;)V", shift = At.Shift.BY, by = 3
    //#else
    //$$  value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/DeathScreen;<init>()V", shift = At.Shift.BY, by = 3
    //#endif
    //$$  ), cancellable = true)
    //$$  private void onGuiOpenEvent(net.minecraft.client.gui.screen.Screen screen, CallbackInfo ci) {
    //$$      ScreenOpenEvent event = new ScreenOpenEvent(screen);
    //$$      EventManager.INSTANCE.post(event);
    //$$      if (event.isCancelled) {
    //$$          ci.cancel();
    //$$      }
    //$$  }
    //#endif

    //#if MC<=11202
    @Inject(method = "runGameLoop", at = @At(value = "FIELD", target = "Lnet/minecraft/util/Timer;renderPartialTicks:F", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void onNonDeltaTickTimerUpdate(CallbackInfo ci) {
        EventManager.INSTANCE.post(new TimerUpdateEvent(this.timer, false));
    }
    //#endif

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE",
            //#if MC<=11202
            target = "Lnet/minecraft/util/Timer;updateTimer()V"
            //#if MC<=10809
            , shift = At.Shift.AFTER, ordinal = 1
            //#endif
            //#else
            //$$ target = "Lnet/minecraft/util/Timer;getPartialTicks(J)I"
            //#endif
    ))
    private void onDeltaTickTimerUpdate(CallbackInfo ci) {
        EventManager.INSTANCE.post(new TimerUpdateEvent(this.timer, true));
    }

    //#if MC<=11202
    //#if FORGE==1
    //#if MC<=10809
    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/settings/KeyBinding;setKeyBindState(IZ)V", ordinal = 1))
    private void onKeyEvent(CallbackInfo ci) {
        int state = 0;
        if (Keyboard.getEventKeyState()) {
            if (Keyboard.isRepeatEvent()) {
                state = 2;
            } else {
                state = 1;
            }
        }
        EventManager.INSTANCE.post(new RawKeyEvent(Keyboard.getEventKey(), state));
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/FMLCommonHandler;fireKeyInput()V", remap = false), remap = true)
    private void onKeyInputEvent(CallbackInfo ci) {
        EventManager.INSTANCE.post(new KeyInputEvent());
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;postMouseEvent()Z", remap = false), remap = true)
    private void onMouseEvent(CallbackInfo ci) {
        EventManager.INSTANCE.post(new RawMouseEvent(Mouse.getEventButton(), Mouse.getEventButtonState() ? 1 : 0));
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/FMLCommonHandler;fireMouseInput()V", remap = false), remap = true)
    private void onMouseInputEvent(CallbackInfo ci) {
        EventManager.INSTANCE.post(new MouseInputEvent(Mouse.getEventButton()));
    }
    //#else
    //$$ @Inject(method = "runTickKeyboard", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;debugCrashKeyPressTime:J", opcode = Opcodes.PUTFIELD))
    //$$ private void onKeyEvent(CallbackInfo ci) {
    //$$     int state = 0;
    //$$     if (Keyboard.getEventKeyState()) {
    //$$         if (Keyboard.isRepeatEvent()) {
    //$$             state = 2;
    //$$         } else {
    //$$             state = 1;
    //$$         }
    //$$     }
    //$$     EventManager.INSTANCE.post(new RawKeyEvent(Keyboard.getEventKey(), state));
    //$$ }
    //$$
    //$$ @Inject(method = "runTickKeyboard", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/FMLCommonHandler;fireKeyInput()V", remap = false), remap = true)
    //$$ private void onKeyInputEvent(CallbackInfo ci) {
    //$$     EventManager.INSTANCE.post(new KeyInputEvent());
    //$$ }
    //$$
    //$$ @Inject(method = "runTickMouse", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;postMouseEvent()Z", remap = false), remap = true)
    //$$ private void onMouseEvent(CallbackInfo ci) {
    //$$     EventManager.INSTANCE.post(new RawMouseEvent(Mouse.getEventButton(), Mouse.getEventButtonState() ? 1 : 0));
    //$$ }
    //$$
    //$$ @Inject(method = "runTickMouse", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/FMLCommonHandler;fireMouseInput()V", remap = false), remap = true)
    //$$ private void onMouseInputEvent(CallbackInfo ci) {
    //$$     EventManager.INSTANCE.post(new MouseInputEvent(Mouse.getEventButton()));
    //$$ }
    //#endif
    //#else
    //#if MC<=10809
    //$$ @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/options/KeyBinding;setKeyPressed(IZ)V", ordinal = 1))
    //$$ private void onKeyEvent(CallbackInfo ci) {
    //$$     int state = 0;
    //$$     if (Keyboard.getEventKeyState()) {
    //$$         if (Keyboard.isRepeatEvent()) {
    //$$             state = 2;
    //$$         } else {
    //$$             state = 1;
    //$$         }
    //$$     }
    //$$     EventManager.INSTANCE.post(new RawKeyEvent(Keyboard.getEventKey(), state));
    //$$ }
    //$$
    //$$ @Inject(method = "tick", at = @At(value = "JUMP", opcode = Opcodes.GOTO, ordinal = 22, by = 2, shift = At.Shift.BY))
    //$$ private void onKeyInputEvent(CallbackInfo ci) {
    //$$     EventManager.INSTANCE.post(new KeyInputEvent());
    //$$ }
    //$$
    //$$ @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventButton()I"))
    //$$ private void onMouseEvent(CallbackInfo ci) {
    //$$     EventManager.INSTANCE.post(new RawMouseEvent(Mouse.getEventButton(), Mouse.getEventButtonState() ? 1 : 0));
    //$$ }
    //$$
    //$$ @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;handleMouse()V", shift = At.Shift.BY, by = 2))
    //$$ private void onMouseInputEvent(CallbackInfo ci) {
    //$$     EventManager.INSTANCE.post(new MouseInputEvent(Mouse.getEventButton()));
    //$$ }
    //#else
    //$$ @Inject(method = "method_12145", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;f3CTime:J", opcode = Opcodes.PUTFIELD))
    //$$ private void onKeyEvent(CallbackInfo ci) {
    //$$     int state = 0;
    //$$     if (Keyboard.getEventKeyState()) {
    //$$         if (Keyboard.isRepeatEvent()) {
    //$$             state = 2;
    //$$         } else {
    //$$             state = 1;
    //$$         }
    //$$     }
    //$$     EventManager.INSTANCE.post(new RawKeyEvent(Keyboard.getEventKey(), state));
    //$$ }
    //$$
    //$$ @Inject(method = "method_12145", at = @At(value = "FIELD", target = "Lnet/minecraft/client/options/GameOptions;debugFpsEnabled:Z", opcode = Opcodes.PUTFIELD, shift = At.Shift.BY, by = 2))
    //$$ private void onKeyInputEvent(CallbackInfo ci) {
    //$$     EventManager.INSTANCE.post(new KeyInputEvent());
    //$$ }
    //$$
    //$$ @Inject(method = "method_12141", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/options/KeyBinding;setKeyPressed(IZ)V"))
    //$$ private void onMouseEvent(CallbackInfo ci) {
    //$$     EventManager.INSTANCE.post(new RawMouseEvent(Mouse.getEventButton(), Mouse.getEventButtonState() ? 1 : 0));
    //$$ }
    //$$
    //$$ @Inject(method = "method_12141", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;handleMouse()V", shift = At.Shift.AFTER))
    //$$ private void onMouseInputEvent(CallbackInfo ci) {
    //$$     EventManager.INSTANCE.post(new MouseInputEvent(Mouse.getEventButton()));
    //$$ }
    //#endif
    //#endif
    //#endif
}