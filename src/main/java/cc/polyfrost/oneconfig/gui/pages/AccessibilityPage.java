package cc.polyfrost.oneconfig.gui.pages;

import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.SVGs;

public class AccessibilityPage extends Page {

    public AccessibilityPage() { super("Accessibility"); }

    @Override
    public void draw(long vg, int x, int y) {
        RenderManager.drawSvg(vg, SVGs.ONECONFIG, x + 20f, y + 20f, 96, 96);
        RenderManager.drawText(vg, "OneConfig", x + 130, y + 46, -1, 42, Fonts.BOLD);
        RenderManager.drawText(vg, "ALPHA - By Polyfrost", x + 132, y + 76, -1, 18, Fonts.MEDIUM);
        RenderManager.drawText(vg, "v0.1", x + 132, y + 96, -1, 18, Fonts.MEDIUM);

        /* accessibility options */
    }
}
