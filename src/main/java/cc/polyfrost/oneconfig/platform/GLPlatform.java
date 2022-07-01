package cc.polyfrost.oneconfig.platform;

import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;

public interface GLPlatform {
    void drawRect(float x, float y, float x2, float y2, int color);

    void enableStencil();

    default float drawText(String text, float x, float y, int color, boolean shadow) {
        return drawText(null, text, x, y, color, shadow);
    }

    float drawText(UMatrixStack matrixStack, String text, float x, float y, int color, boolean shadow);

    int getStringWidth(String text);
}
