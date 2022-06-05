package cc.polyfrost.oneconfig.mixin;


import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(FontRenderer.class)
public class FontRendererMixin {

    //@Inject(method = "renderDefaultChar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;bindTexture(Lnet/minecraft/util/ResourceLocation;)V", shift = At.Shift.AFTER))
    //public void whoAsked(int ch, boolean italic, CallbackInfoReturnable<Float> cir) {
    //    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    //    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
    //}
}
