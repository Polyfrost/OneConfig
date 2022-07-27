package cc.polyfrost.oneconfig.gui.pages;

import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.internal.assets.SVGs;

public class CreditsPage extends Page {
    public CreditsPage() {
        super("Credits");
    }

    @Override
    public void draw(long vg, int x, int y) {
        RenderManager.drawSvg(vg, SVGs.ONECONFIG, x + 20f, y + 20f, 96, 96);
        RenderManager.drawText(vg, "OneConfig", x + 130, y + 46, -1, 42, Fonts.BOLD);
        RenderManager.drawText(vg, "ALPHA - By Polyfrost", x + 132, y + 76, -1, 18, Fonts.MEDIUM);
        RenderManager.drawText(vg, "v0.1", x + 132, y + 96, -1, 18, Fonts.MEDIUM);

        RenderManager.drawText(vg, "Development Team", x + 20, y + 180, -1, 24, Fonts.SEMIBOLD);
        RenderManager.drawText(vg, " - MoonTidez - Founder and lead designer", x + 20, y + 205, -1, 12, Fonts.REGULAR);
        RenderManager.drawText(vg, " - DeDiamondPro - Founder, Config backend, GUI frontend, HUD", x + 20, y + 220, -1, 12, Fonts.REGULAR);        // +15/line
        RenderManager.drawText(vg, " - nextdaydelivery - GUI frontend, Render Manager, Utilities", x + 20, y + 235, -1, 12, Fonts.REGULAR);
        RenderManager.drawText(vg, " - Wyvest - Gradle, Render Manager, VCAL, Utilities", x + 20, y + 250, -1, 12, Fonts.REGULAR);
        RenderManager.drawText(vg, " - Ethan - Utilities", x + 20, y + 265, -1, 12, Fonts.REGULAR);

        RenderManager.drawText(vg, "Libraries", x + 20, y + 318, -1, 24, Fonts.SEMIBOLD);
        RenderManager.drawText(vg, " - LWJGLTwoPointFive (DJTheRedstoner) - LWJGL3 loading hack", x + 20, y + 340, -1, 12, Fonts.REGULAR);
        RenderManager.drawText(vg, " - #getResourceAsStream (SpinyOwl) - IO Utility and shadow", x + 20, y + 355, -1, 12, Fonts.REGULAR);
        RenderManager.drawText(vg, " - NanoVG (memononen) - NanoVG Library", x + 20, y + 370, -1, 12, Fonts.REGULAR);
        RenderManager.drawText(vg, " - UniversalCraft (Sk1er LLC) - Multiversioning bindings", x + 20, y + 385, -1, 12, Fonts.REGULAR);
        RenderManager.drawText(vg, " - https://easings.net/ - Easing functions", x + 20, y + 400, -1, 12, Fonts.REGULAR);
        RenderManager.drawText(vg, " - Quiltflower (Quilt Team) - Gradle decompiler", x + 20, y + 415, -1, 12, Fonts.REGULAR);
        RenderManager.drawText(vg, " - Seraph (Scherso) - Locraw and Multithreading utilities", x + 20, y + 430, -1, 12, Fonts.REGULAR);
        RenderManager.drawText(vg, " - Android - Cubic Bezier curve", x + 20, y + 445, -1, 12, Fonts.REGULAR);
        RenderManager.drawText(vg, " - Fabric API (FabricMC) - Client-side Commands", x + 20, y + 460, -1, 12, Fonts.REGULAR);
    }

    @Override
    public boolean isBase() {
        return true;
    }
}
