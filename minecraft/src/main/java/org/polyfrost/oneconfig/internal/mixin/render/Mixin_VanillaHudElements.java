//~ gui_graphics
package org.polyfrost.oneconfig.internal.mixin.render;

//? if > 1.8.9 {
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
//? if >= 26.2 {
import net.minecraft.client.gui.Hud;
//?} else {
/*import net.minecraft.client.gui.Gui;
*///?}
//? if >= 26.3 {
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
//?} else if >= 1.21.6 {
/*import com.mojang.blaze3d.pipeline.RenderPipeline;
*///?} else if >= 1.21.4 {
/*import java.util.function.Function;
*///?}
//? if >= 1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;
*///?}
import org.polyfrost.oneconfig.api.ui.v1.platform.VanillaHudElement;
import org.polyfrost.oneconfig.api.ui.v1.platform.VanillaHudHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 26.2 {
@Mixin(Hud.class)
//?} else {
/*@Mixin(Gui.class)
*///?}
public abstract class Mixin_VanillaHudElements {
    @Unique
    private static final String CROSSHAIR =
        //~ if >= 26.1 'renderCrosshair' -> 'extractCrosshair'
        "extractCrosshair";

    @Unique
    private static final String BLIT_SPRITE =
        //? if >= 26.3 {
        "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V";
        //?} else if >= 1.21.11 {
        /*"Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V";
        *///?} else if >= 1.21.6 {
        /*"Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIII)V";
        *///?} else if >= 1.21.4 {
        /*"Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIII)V";
        *///?} else {
        /*"Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V";
        *///?}

    @Unique
    private static final String BLIT_SPRITE_REGION =
        //? if >= 26.3 {
        "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIIIII)V";
        //?} else if >= 1.21.11 {
        /*"Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIIIII)V";
        *///?} else if >= 1.21.6 {
        /*"Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIIIIIII)V";
        *///?} else if >= 1.21.4 {
        /*"Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIIIIIII)V";
        *///?} else {
        /*"Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIIIIIII)V";
        *///?}

    @WrapWithCondition(method = CROSSHAIR, at = @At(value = "INVOKE", target = BLIT_SPRITE, ordinal = 0))
    private boolean oneconfig$crosshair(
        GuiGraphicsExtractor graphics,
        //? if >= 1.21.6 {
        RenderPipeline pipeline,
        //?} else if >= 1.21.4 {
        /*Function<?, ?> renderType,
        *///?}
        //? if >= 1.21.11 {
        Identifier sprite,
        //?} else {
        /*ResourceLocation sprite,
        *///?}
        int x, int y, int width, int height
    ) {
        return !VanillaHudHooks.intercept(VanillaHudElement.CROSSHAIR, graphics);
    }

    @WrapWithCondition(method = CROSSHAIR, at = {
        @At(value = "INVOKE", target = BLIT_SPRITE, ordinal = 1),
        @At(value = "INVOKE", target = BLIT_SPRITE, ordinal = 2),
    })
    private boolean oneconfig$attackIndicator(
        GuiGraphicsExtractor graphics,
        //? if >= 1.21.6 {
        RenderPipeline pipeline,
        //?} else if >= 1.21.4 {
        /*Function<?, ?> renderType,
        *///?}
        //? if >= 1.21.11 {
        Identifier sprite,
        //?} else {
        /*ResourceLocation sprite,
        *///?}
        int x, int y, int width, int height
    ) {
        return !VanillaHudHooks.intercept(VanillaHudElement.ATTACK_INDICATOR, graphics);
    }

    @WrapWithCondition(method = CROSSHAIR, at = @At(value = "INVOKE", target = BLIT_SPRITE_REGION))
    private boolean oneconfig$attackIndicatorProgress(
        GuiGraphicsExtractor graphics,
        //? if >= 1.21.6 {
        RenderPipeline pipeline,
        //?} else if >= 1.21.4 {
        /*Function<?, ?> renderType,
        *///?}
        //? if >= 1.21.11 {
        Identifier sprite,
        //?} else {
        /*ResourceLocation sprite,
        *///?}
        int spriteWidth, int spriteHeight, int u, int v, int x, int y, int width, int height
    ) {
        return VanillaHudHooks.vanillaDraws(VanillaHudElement.ATTACK_INDICATOR);
    }

    //~ if >= 26.1 'renderEffects' -> 'extractEffects'
    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void oneconfig$statusEffects(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (VanillaHudHooks.intercept(VanillaHudElement.STATUS_EFFECTS, graphics)) ci.cancel();
    }

    //~ if >= 26.1 'renderScoreboardSidebar' -> 'extractScoreboardSidebar'
    @Inject(method = "extractScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void oneconfig$scoreboard(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (VanillaHudHooks.intercept(VanillaHudElement.SCOREBOARD, graphics)) ci.cancel();
    }

    //~ if >= 26.1 'renderOverlayMessage' -> 'extractOverlayMessage'
    @Inject(method = "extractOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void oneconfig$actionBar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (VanillaHudHooks.intercept(VanillaHudElement.ACTION_BAR, graphics)) ci.cancel();
    }

    //~ if >= 26.1 'renderTitle' -> 'extractTitle'
    @Inject(method = "extractTitle", at = @At("HEAD"), cancellable = true)
    private void oneconfig$title(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (VanillaHudHooks.intercept(VanillaHudElement.TITLE, graphics)) ci.cancel();
    }

    //~ if >= 26.1 'renderSelectedItemName' -> 'extractSelectedItemName'
    @Inject(method = "extractSelectedItemName", at = @At("HEAD"), cancellable = true)
    private void oneconfig$heldItemName(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (VanillaHudHooks.intercept(VanillaHudElement.HELD_ITEM_NAME, graphics)) ci.cancel();
    }

    //~ if >= 26.1 'renderVignette' -> 'extractVignette'
    @Inject(method = "extractVignette", at = @At("HEAD"), cancellable = true)
    private void oneconfig$vignette(GuiGraphicsExtractor graphics, Entity entity, CallbackInfo ci) {
        if (VanillaHudHooks.intercept(VanillaHudElement.VIGNETTE, graphics)) ci.cancel();
    }
}
//?} else {
/*import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.GameGui;
import org.polyfrost.oneconfig.api.ui.v1.platform.VanillaHudElement;
import org.polyfrost.oneconfig.api.ui.v1.platform.VanillaHudHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameGui.class)
public abstract class Mixin_VanillaHudElements {
    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GameGui;hasCrosshair()Z"))
    private boolean oneconfig$crosshair(boolean original) {
        if (!original) return false;
        return !VanillaHudHooks.intercept(VanillaHudElement.CROSSHAIR, null);
    }
}
*///?}
