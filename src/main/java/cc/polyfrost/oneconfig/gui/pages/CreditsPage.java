package cc.polyfrost.oneconfig.gui.pages;

import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.SVGs;

public class CreditsPage extends Page {
    CreditsPage() {
        super("Credits");
    }

    @Override
    public void draw(long vg, int x, int y) {
        RenderManager.drawSvg(vg, SVGs.ONECONFIG, x + 20f, y + 20f, 96, 96);
        RenderManager.drawString(vg, "OneConfig", x + 130, y + 46, -1, 42, Fonts.BOLD);
        RenderManager.drawString(vg, "ALPHA - By Polyfrost", x + 132, y + 76, -1, 18, Fonts.MEDIUM);
        RenderManager.drawString(vg, "v0.1", x + 132, y + 96, -1, 18, Fonts.MEDIUM);

        RenderManager.drawString(vg, "Development Team", x + 20, y + 180, -1, 24, Fonts.SEMIBOLD);
        RenderManager.drawString(vg, " - MoonTidez - Founder and lead designer", x + 20, y + 205, -1, 12, Fonts.REGULAR);
        RenderManager.drawString(vg, " - DeDiamondPro - Config backend, GUI frontend, HUD", x + 20, y + 220, -1, 12, Fonts.REGULAR);        // +15/line
        RenderManager.drawString(vg, " - nextdaydelivery - GUI frontend, Render Manager, Utilities", x + 20, y + 235, -1, 12, Fonts.REGULAR);
        RenderManager.drawString(vg, " - Wyvest - Gradle, Render Manager, VCAL, Utilities", x + 20, y + 250, -1, 12, Fonts.REGULAR);
        RenderManager.drawString(vg, " - Ethan - Utilities", x + 20, y + 265, -1, 12, Fonts.REGULAR);

        RenderManager.drawString(vg, "Libraries", x + 20, y + 318, -1, 24, Fonts.SEMIBOLD);
        RenderManager.drawString(vg, " - LWJGLTwoPointFive (DJTheRedstoner) - LWJGL3 loading hack", x + 20, y + 340, -1, 12, Fonts.REGULAR);
        RenderManager.drawString(vg, " - #getResourceAsStream (SpinyOwl) - IO Utility", x + 20, y + 355, -1, 12, Fonts.REGULAR);
        RenderManager.drawString(vg, " - NanoVG (memononen) - NanoVG Library", x + 20, y + 370, -1, 12, Fonts.REGULAR);
        RenderManager.drawString(vg, " - UniversalCraft (Sk1er LLC) - Multiversioning bindings", x + 20, y + 385, -1, 12, Fonts.REGULAR);
        RenderManager.drawString(vg, " - Easing Functions (jesusgollonet)", x + 20, y + 400, -1, 12, Fonts.REGULAR);
        RenderManager.drawString(vg, " - Quiltflower (W-OVERFLOW) - Gradle toolkit", x + 20, y + 415, -1, 12, Fonts.REGULAR);

        RenderManager.drawString(vg, "Terms Of Service & Licensing", x + 20, y + 517, -1, 24, Fonts.SEMIBOLD);
        RenderManager.drawString(vg, " - License Summary goes here", x + 20, y + 540, -1, 12, Fonts.REGULAR);

        RenderManager.drawImage(vg, "https://i.insider.com/602ee9ced3ad27001837f2ac?width=700", x + 600, y + 20, 350, 263);


    }
}
