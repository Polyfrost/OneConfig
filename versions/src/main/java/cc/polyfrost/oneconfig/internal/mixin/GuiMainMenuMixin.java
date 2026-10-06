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

package cc.polyfrost.oneconfig.internal.mixin;

import cc.polyfrost.oneconfig.internal.gui.OneClientPromo;
import cc.polyfrost.oneconfig.internal.hacks.ByeSkyClientHack;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMainMenu.class)
public class GuiMainMenuMixin extends GuiScreen {

    @Unique
    private static final String ONECLIENT_BANNER_1 = "\u00a7lOur Forge mods are no longer being updated;";
    @Unique
    private static final String ONECLIENT_BANNER_2 = "click here to switch to OneClient!";
    @Unique
    private static final int ONECLIENT_BANNER_COLOR = 0x55200000;

    @Shadow
    private String splashText;

    @Inject(method = "initGui", at = @At("HEAD"))
    private void setOneClientSplash(CallbackInfo ci) {
        splashText = "https://polyfrost.org/OneClient";
    }

    @Inject(method = "drawScreen", at = @At("HEAD"), cancellable = true)
    private void onDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (ByeSkyClientHack.INSTANCE.isSkyClient()) {
            ci.cancel();
            ByeSkyClientHack.INSTANCE.overwriteGui(mouseX, mouseY, partialTicks);
        }
    }

    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void drawOneClientPromo(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (OneClientPromo.INSTANCE.isActive()) {
            OneClientPromo.INSTANCE.draw();
        } else {
            int bannerWidth = oneconfig$getBannerWidth();
            int left = (width - bannerWidth) / 2;
            int top = oneconfig$getBannerTop();
            drawRect(left - 2, top - 2, left + bannerWidth + 2, top + 23, ONECLIENT_BANNER_COLOR);
            drawString(this.fontRendererObj, ONECLIENT_BANNER_1, left, top, -1);
            drawString(this.fontRendererObj, ONECLIENT_BANNER_2, (width - this.fontRendererObj.getStringWidth(ONECLIENT_BANNER_2)) / 2, top + 12, -1);
        }
    }

    @Unique
    private int oneconfig$getBannerWidth() {
        return Math.max(this.fontRendererObj.getStringWidth(ONECLIENT_BANNER_1), this.fontRendererObj.getStringWidth(ONECLIENT_BANNER_2));
    }

    @Unique
    private int oneconfig$getBannerTop() {
        return height / 4 + 24;
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void blockClicksUnderPromo(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        if (OneClientPromo.INSTANCE.isActive()) {
            ci.cancel();
            return;
        }
        int bannerWidth = oneconfig$getBannerWidth();
        int left = (width - bannerWidth) / 2;
        int top = oneconfig$getBannerTop();
        if (mouseX >= left && mouseX <= left + bannerWidth && mouseY >= top && mouseY <= top + 24) {
            OneClientPromo.INSTANCE.show();
            ci.cancel();
        }
    }
}
