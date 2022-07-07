//#if MC<=11202
package cc.polyfrost.oneconfig.internal.mixin;

import cc.polyfrost.oneconfig.internal.eggs.TechnobladeCrownRenderer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPig;
import net.minecraft.entity.passive.EntityPig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderPig.class)
public abstract class RenderPigMixin extends RenderLiving<EntityPig> {
    public RenderPigMixin(RenderManager renderManager, ModelBase modelBase, float f) {
        super(renderManager, modelBase, f);
    }
    @Inject(method = "<init>", at = @At("TAIL"))
    private void addCrown(RenderManager renderManager, ModelBase modelBase, float f, CallbackInfo ci) {
        addLayer(new TechnobladeCrownRenderer<>((RenderPig) (Object) this));
    }
}
//#endif