package cc.polyfrost.oneconfig.test;

import cc.polyfrost.oneconfig.hud.BasicHud;
import cc.polyfrost.oneconfig.internal.assets.Images;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.renderer.RenderManager;

public class TestBasicHud_Test extends BasicHud {
    @Override
    protected void draw(UMatrixStack matrices, float x, float y, float scale, boolean example) {
        RenderManager.setupAndDraw(true, vg -> RenderManager.drawImage(vg, Images.HUE_GRADIENT, x, y, 50 * scale, 50f * scale));
    }

    @Override
    protected float getWidth(float scale, boolean example) {
        return 50 * scale;
    }

    @Override
    protected float getHeight(float scale, boolean example) {
        return 50 * scale;
    }
}
