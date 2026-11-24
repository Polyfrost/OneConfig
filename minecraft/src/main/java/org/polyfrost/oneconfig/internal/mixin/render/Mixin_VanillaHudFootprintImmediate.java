package org.polyfrost.oneconfig.internal.mixin.render;

//? if > 1.8.9 && < 1.21.8 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.polyfrost.oneconfig.internal.ui.hud.VanillaHudFootprint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//? if >= 1.21.4 {
import java.util.function.Function;
//?}

@Mixin(GuiGraphics.class)
public abstract class Mixin_VanillaHudFootprintImmediate {
    @Shadow
    public abstract PoseStack pose();

    @Unique
    private final Vector3f oneconfig$corner = new Vector3f();

    @Unique
    private boolean oneconfig$measure(float x1, float y1, float x2, float y2) {
        if (!VanillaHudFootprint.isCapturing()) return false;
        float left = Math.min(x1, x2);
        float right = Math.max(x1, x2);
        float top = Math.min(y1, y2);
        float bottom = Math.max(y1, y2);
        if (right > left && bottom > top) {
            Matrix4f matrix = pose().last().pose();
            float minX = Float.POSITIVE_INFINITY;
            float minY = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY;
            float maxY = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < 4; i++) {
                matrix.transformPosition((i & 1) == 0 ? left : right, (i & 2) == 0 ? top : bottom, 0f, oneconfig$corner);
                minX = Math.min(minX, oneconfig$corner.x);
                minY = Math.min(minY, oneconfig$corner.y);
                maxX = Math.max(maxX, oneconfig$corner.x);
                maxY = Math.max(maxY, oneconfig$corner.y);
            }
            VanillaHudFootprint.add(minX, minY, maxX, maxY);
        }
        return VanillaHudFootprint.isSampling();
    }

    //? if >= 1.21.4 {
    @Inject(method = "innerBlit(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIIIFFFFI)V", at = @At("HEAD"), cancellable = true)
    private void oneconfig$blit(Function<ResourceLocation, RenderType> renderType, ResourceLocation atlas, int x1, int x2, int y1, int y2, float minU, float maxU, float minV, float maxV, int color, CallbackInfo ci) {
        if (oneconfig$measure(x1, y1, x2, y2)) ci.cancel();
    }
    //?} else {
    /^@Inject(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFF)V", at = @At("HEAD"), cancellable = true)
    private void oneconfig$blit(ResourceLocation atlas, int x1, int x2, int y1, int y2, int z, float minU, float maxU, float minV, float maxV, CallbackInfo ci) {
        if (oneconfig$measure(x1, y1, x2, y2)) ci.cancel();
    }

    @Inject(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFFFFFF)V", at = @At("HEAD"), cancellable = true)
    private void oneconfig$tintedBlit(ResourceLocation atlas, int x1, int x2, int y1, int y2, int z, float minU, float maxU, float minV, float maxV, float red, float green, float blue, float alpha, CallbackInfo ci) {
        if (oneconfig$measure(x1, y1, x2, y2)) ci.cancel();
    }
    ^///?}

    @Inject(method = "fill(Lnet/minecraft/client/renderer/RenderType;IIIIII)V", at = @At("HEAD"), cancellable = true)
    private void oneconfig$fill(RenderType renderType, int x1, int y1, int x2, int y2, int z, int color, CallbackInfo ci) {
        if (oneconfig$measure(x1, y1, x2, y2)) ci.cancel();
    }

    @Inject(method = "fillGradient(Lcom/mojang/blaze3d/vertex/VertexConsumer;IIIIIII)V", at = @At("HEAD"), cancellable = true)
    private void oneconfig$fillGradient(VertexConsumer consumer, int x1, int y1, int x2, int y2, int z, int colorFrom, int colorTo, CallbackInfo ci) {
        if (oneconfig$measure(x1, y1, x2, y2)) ci.cancel();
    }

    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I", at = @At("HEAD"), cancellable = true)
    private void oneconfig$string(Font font, String text, int x, int y, int color, boolean shadow, CallbackInfoReturnable<Integer> cir) {
        if (text == null || !VanillaHudFootprint.isCapturing()) return;
        int width = font.width(text);
        if (oneconfig$measure(x, y, x + width, y + font.lineHeight)) cir.setReturnValue(x + width);
    }

    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I", at = @At("HEAD"), cancellable = true)
    private void oneconfig$sequence(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow, CallbackInfoReturnable<Integer> cir) {
        if (text == null || !VanillaHudFootprint.isCapturing()) return;
        int width = font.width(text);
        if (oneconfig$measure(x, y, x + width, y + font.lineHeight)) cir.setReturnValue(x + width);
    }

    @Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V", at = @At("HEAD"), cancellable = true)
    private void oneconfig$item(LivingEntity entity, Level level, ItemStack stack, int x, int y, int seed, int guiOffset, CallbackInfo ci) {
        if (stack.isEmpty()) return;
        if (oneconfig$measure(x, y, x + 16, y + 16)) ci.cancel();
    }
}
*///?}
