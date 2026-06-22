package org.polyfrost.oneconfig.internal.mixin.compat.firmament;

//? if > 1.8.9 {
//~ gui_graphics
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.polyfrost.oneconfig.internal.compat.FirmamentHudCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? >= 1.21.8 {
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
//? }

@Mixin(GuiGraphicsExtractor.class)
public class Mixin_FirmamentContentCapture {

    @Inject(
        //? if >= 26.1 {
        method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
        //?} else {
        /*method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
        *///?}
        at = @At("HEAD"),
        require = 0
    )
    private void oneconfig$captureFirmamentText(Font font, Component text, int x, int y, int color, boolean shadow, CallbackInfo ci) {
        //? >= 1.21.8 {
        if (text == null || !FirmamentHudCompat.isCapturing()) return;
        oneconfig$noteTextBounds(font, x, y, font.width(text));
        //? }
    }

    @Inject(
        //? if >= 26.1 {
        method = "text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V",
        //?} else {
        /*method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V",
        *///?}
        at = @At("HEAD"),
        require = 0
    )
    private void oneconfig$captureFirmamentString(Font font, String text, int x, int y, int color, boolean shadow, CallbackInfo ci) {
        //? >= 1.21.8 {
        if (text == null || !FirmamentHudCompat.isCapturing()) return;
        oneconfig$noteTextBounds(font, x, y, font.width(text));
        //? }
    }

    //? >= 1.21.8 {
    @Unique
    private void oneconfig$noteTextBounds(Font font, int x, int y, int width) {
        Matrix3x2f pose = ((GuiGraphicsExtractor) (Object) this).pose();
        int height = font.lineHeight;
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        int[][] corners = {{x, y}, {x + width, y}, {x, y + height}, {x + width, y + height}};
        for (int[] c : corners) {
            Vector2f p = pose.transformPosition(new Vector2f(c[0], c[1]));
            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);
            maxX = Math.max(maxX, p.x);
            maxY = Math.max(maxY, p.y);
        }
        FirmamentHudCompat.noteContentDrawn(minX, minY, maxX, maxY);
    }
    //? }
}
//?}
