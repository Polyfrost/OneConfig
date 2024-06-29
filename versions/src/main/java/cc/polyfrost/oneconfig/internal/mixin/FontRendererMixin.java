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
import cc.polyfrost.oneconfig.internal.renderer.BorderedTextHooks;
import cc.polyfrost.oneconfig.renderer.TextRenderer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FontRenderer.class)
public class FontRendererMixin {
    @Shadow
    protected float posX;

    @Shadow
    protected float posY;

    @ModifyVariable(method = "drawString(Ljava/lang/String;FFIZ)I", at = @At(value = "HEAD"))
    private boolean replaceShadow(boolean dropShadow) {
        if (Preferences.optimizedFontRenderer && dropShadow) {
            BorderedTextHooks.INSTANCE.setTextType(TextRenderer.TextType.SHADOW);
            return false;
        }
        return dropShadow;
    }

    @Inject(method = "drawString(Ljava/lang/String;FFIZ)I", at = @At(value = "RETURN"), cancellable = true)
    private void cachedShadow(String text, float x, float y, int color, boolean dropShadow, CallbackInfoReturnable<Integer> cir) {
        if (Preferences.optimizedFontRenderer && dropShadow) {
            BorderedTextHooks.INSTANCE.setTextType(TextRenderer.TextType.NONE);
            cir.setReturnValue(cir.getReturnValue());
        }
    }

    @ModifyConstant(method = "renderDefaultChar", constant = @Constant(floatValue = 128f))
    private float asciiTextureSize(float constant) {
        switch (BorderedTextHooks.INSTANCE.getTextType()) {
            case SHADOW:
                return 144f;
            case FULL:
                return 160f;
            default:
                return constant;
        }
    }

    @Inject(method = "renderDefaultChar", at = @At("HEAD"))
    private void asciiShift(int ch, boolean italic, CallbackInfoReturnable<Float> cir) {
        if (BorderedTextHooks.INSTANCE.getTextType() == TextRenderer.TextType.FULL) {
            posX -= 1;
            posY -= 1;
        }
    }

    @Inject(method = "renderDefaultChar", at = @At("RETURN"))
    private void asciiUnshift(CallbackInfoReturnable<Float> cir) {
        if (BorderedTextHooks.INSTANCE.getTextType() == TextRenderer.TextType.FULL) {
            posX += 1;
            posY += 1;
        }
    }

    @ModifyConstant(method = "renderDefaultChar", constant = @Constant(intValue = 8))
    private int asciiGlyphSize(int constant) {
        switch (BorderedTextHooks.INSTANCE.getTextType()) {
            case SHADOW:
                return 9;
            case FULL:
                return 10;
            default:
                return constant;
        }
    }

    @ModifyConstant(method = "renderDefaultChar", constant = @Constant(floatValue = 0.01f))
    private float asciiGlyphWidth(float constant) {
        switch (BorderedTextHooks.INSTANCE.getTextType()) {
            case SHADOW:
                return constant - 1.0f;
            case FULL:
                return constant - 4.0f;
            default:
                return constant;
        }
    }
    @ModifyConstant(method = "renderDefaultChar", constant = @Constant(floatValue = 7.99f))
    private float asciiPixelSize(float constant) {
        switch (BorderedTextHooks.INSTANCE.getTextType()) {
            case SHADOW:
                return 8.99f;
            case FULL:
                return 9.99f;
            default:
                return constant;
        }
    }

    @ModifyArg(method = "renderDefaultChar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;bindTexture(Lnet/minecraft/util/ResourceLocation;)V"))
    private ResourceLocation asciiTexture(ResourceLocation location) {
        switch (BorderedTextHooks.INSTANCE.getTextType()) {
            case SHADOW:
                return BorderedTextHooks.INSTANCE.getAsciiTexture().getShadowed().getLocation();
            case FULL:
                return BorderedTextHooks.INSTANCE.getAsciiTexture().getBordered().getLocation();
            default:
                return location;
        }
    }

    @ModifyConstant(method = "renderUnicodeChar", constant = @Constant(floatValue = 256f))
    private float unicodeTextureSize(float constant) {
        switch (BorderedTextHooks.INSTANCE.getTextType()) {
            case SHADOW:
                return 272f;
            case FULL:
                return 320f;
            default:
                return constant;
        }
    }

    @ModifyConstant(method = "renderUnicodeChar", constant = {@Constant(intValue = 16, ordinal = 1), @Constant(intValue = 16, ordinal = 3)})
    private int unicodeGlyphSize(int constant) {
        switch (BorderedTextHooks.INSTANCE.getTextType()) {
            case SHADOW:
                return 17;
            case FULL:
                return 20;
            default:
                return constant;
        }
    }

    @ModifyConstant(method = "renderUnicodeChar", constant = @Constant(floatValue = 0.02f))
    private float unicodeGlyphWidth(float constant) {
        switch (BorderedTextHooks.INSTANCE.getTextType()) {
            case SHADOW:
                return constant - 1.0f;
            case FULL:
                return constant - 4.0f;
            default:
                return constant;
        }
    }

    @ModifyConstant(method = "renderUnicodeChar", constant = @Constant(floatValue = 15.98F))
    private float unicodeGlyphHeight(float constant) {
        switch (BorderedTextHooks.INSTANCE.getTextType()) {
            case SHADOW:
                return 16.98f;
            case FULL:
                return 19.98f;
            default:
                return constant;
        }
    }

    @ModifyConstant(method = "renderUnicodeChar", constant = @Constant(floatValue = 7.99f))
    private float unicodePixelSize(float constant) {
        switch (BorderedTextHooks.INSTANCE.getTextType()) {
            case SHADOW:
                return 8.49f;
            case FULL:
                return 9.99f;
            default:
                return constant;
        }
    }

    @Inject(method = "getUnicodePageLocation", at = @At("HEAD"), cancellable = true)
    private void unicodeTexture(int page, CallbackInfoReturnable<ResourceLocation> cir) {
        switch (BorderedTextHooks.INSTANCE.getTextType()) {
            case SHADOW:
                cir.setReturnValue(BorderedTextHooks.INSTANCE.getUnicodeTexture()[page].getShadowed().getLocation());
                break;
            case FULL:
                cir.setReturnValue(BorderedTextHooks.INSTANCE.getUnicodeTexture()[page].getBordered().getLocation());
                break;
        }
    }


    @Inject(method = "renderUnicodeChar", at = @At("HEAD"))
    private void unicodeShift(char ch, boolean italic, CallbackInfoReturnable<Float> cir) {
        if (BorderedTextHooks.INSTANCE.getTextType() == TextRenderer.TextType.FULL) {
            posX -= 1f;
            posY -= 1f;
        }
    }

    @Inject(method = "renderUnicodeChar", at = @At("RETURN"))
    private void unicodeUnshift(CallbackInfoReturnable<Float> cir) {
        if (BorderedTextHooks.INSTANCE.getTextType() == TextRenderer.TextType.FULL) {
            posX += 1f;
            posY += 1f;
        }
    }
}
//#endif