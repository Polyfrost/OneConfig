/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost.
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

package cc.polyfrost.oneconfig.internal.mixin;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.ClientTickTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftMixin {
    @Final
    @Shadow
    private ClientTickTracker tricker; //lol

    @Inject(method = "stop", at = @At("HEAD"))
    private void onShutdown(CallbackInfo ci) {
        EventManager.INSTANCE.post(new PreShutdownEvent());
    }

    @Inject(method = "startGame", at = @At("HEAD"))
    private void onStart(CallbackInfo ci) {
        EventManager.INSTANCE.post(new StartEvent());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> EventManager.INSTANCE.post(new ShutdownEvent())));
    }

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;render(FJ)V"))
    private void onRenderTickStart(CallbackInfo ci) {
        EventManager.INSTANCE.post(new RenderEvent(Stage.START, tricker.tickDelta));
    }

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;render(FJ)V", shift = At.Shift.AFTER))
    private void onRenderTickEnd(CallbackInfo ci) {
        EventManager.INSTANCE.post(new RenderEvent(Stage.END, tricker.tickDelta));
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onClientTickStart(CallbackInfo ci) {
        EventManager.INSTANCE.post(new TickEvent(Stage.START));
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onClientTickEnd(CallbackInfo ci) {
        EventManager.INSTANCE.post(new TickEvent(Stage.END));
    }

    @Inject(method = "openScreen", at = @At("HEAD"), cancellable = true)
    private void onGuiOpenEvent(Screen screen, CallbackInfo ci) {
        ScreenOpenEvent event = new ScreenOpenEvent(screen);
        EventManager.INSTANCE.post(event);
        if (event.isCancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/ClientTickTracker;tick()V", shift = At.Shift.AFTER))
    private void onDeltaTickTimerUpdate(CallbackInfo ci) {
        EventManager.INSTANCE.post(new TimerUpdateEvent(tricker, true));
    }

    @Inject(method = "method_12145", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;hasAltDown()Z", shift = At.Shift.AFTER))
    private void onKeyEvent(CallbackInfo ci) {
        EventManager.INSTANCE.post(new KeyInputEvent());
    }

    @Inject(method = "method_12141", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;handleMouse()V", shift = At.Shift.AFTER))
    private void onMouseEvent(CallbackInfo ci) {
        EventManager.INSTANCE.post(new MouseInputEvent());
    }
}