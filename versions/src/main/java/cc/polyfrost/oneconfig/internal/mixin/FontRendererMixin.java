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

//#if FORGE==1 && MC<=11202
package cc.polyfrost.oneconfig.internal.mixin;

import cc.polyfrost.oneconfig.internal.config.Preferences;
import cc.polyfrost.oneconfig.internal.renderer.BorderedTextRenderer;
import cc.polyfrost.oneconfig.renderer.TextRenderer;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FontRenderer.class, priority = 2000)
public class FontRendererMixin {
    @Shadow
    protected int[] charWidth;

    @Shadow
    protected float posX;

    @Shadow
    protected float posY;

    @Shadow
    protected byte[] glyphWidth;

    @Inject(method = "drawString(Ljava/lang/String;FFIZ)I", at = @At(value = "HEAD"), cancellable = true)
    private void cachedShadow(String text, float x, float y, int color, boolean dropShadow, CallbackInfoReturnable<Integer> cir) {
        if (Preferences.optimizedFontRenderer && dropShadow) {
            cir.setReturnValue((int) BorderedTextRenderer.INSTANCE.drawString(text, x, y, color, TextRenderer.TextType.SHADOW));
        }
    }

    @Inject(method = "renderDefaultChar", at = @At(value = "HEAD"), cancellable = true)
    private void overrideDefault(int ch, boolean italic, CallbackInfoReturnable<Float> cir) {
        Float f = BorderedTextRenderer.INSTANCE.renderDefaultChar(ch, italic, charWidth, posX, posY);
        if (f == null) return;
        cir.setReturnValue(f);
    }

    @Inject(method = "renderUnicodeChar", at = @At(value = "HEAD"), cancellable = true)
    private void overrideUnicode(char ch, boolean italic, CallbackInfoReturnable<Float> cir) {
        Float f = BorderedTextRenderer.INSTANCE.renderUnicodeChar(ch, italic, glyphWidth, posX, posY);
        if (f == null) return;
        cir.setReturnValue(f);
    }
}
//#endif