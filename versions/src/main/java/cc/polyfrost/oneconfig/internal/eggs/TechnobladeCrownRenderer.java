//#if MC<=11202
package cc.polyfrost.oneconfig.internal.eggs;

import net.minecraft.client.model.ModelPig;
import net.minecraft.client.renderer.entity.RenderPig;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.util.ResourceLocation;

/**
 * Adapted from technomodel under MIT
 * <a href="https://github.com/thecolonel63/technomodel/blob/master/LICENSE">...</a>
 */
public class TechnobladeCrownRenderer<T extends EntityPig> implements LayerRenderer<T> {

    private static final ResourceLocation CROWN_TEXTURE = new ResourceLocation("oneconfig", "textures/entity/pig/technocrown.png");
    private final RenderPig renderer;
    private final ModelPig pigModel = new ModelPig(0.5F);

    public TechnobladeCrownRenderer(RenderPig renderPig) {
        renderer = renderPig;
    }

    @Override
    public void doRenderLayer(T entitylivingbaseIn, float f, float g, float partialTicks, float h, float i, float j, float scale) {
        if (entitylivingbaseIn.hasCustomName() && entitylivingbaseIn.getCustomNameTag().equals("Technoblade")) {
            this.renderer.bindTexture(CROWN_TEXTURE);
            this.pigModel.setModelAttributes(renderer.getMainModel());
            this.pigModel.render(entitylivingbaseIn, f, g, h, i, j, scale);
        }
    }

    @Override
    public boolean shouldCombineTextures() {
        return true;
    }
}
//#endif