package org.polyfrost.oneconfig.internal.mixin.hidpi;

//#if MC < 1.13
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiScreen.class)
public abstract class Mixin_FixMousePositionHiDPI_Screen {

    @Redirect(method = "handleMouseInput", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;displayWidth:I", ordinal = 0))
    private int hiDpiFixMouseX(Minecraft mc) {
        return (int) (mc.displayWidth / Display.getPixelScaleFactor());
    }

    @Redirect(method = "handleMouseInput", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;displayHeight:I", ordinal = 0))
    private int hiDpiFixMouseY(Minecraft mc) {
        return (int) (mc.displayHeight / Display.getPixelScaleFactor());
    }

}
//#endif
