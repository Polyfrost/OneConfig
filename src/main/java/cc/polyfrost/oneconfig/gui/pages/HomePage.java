package cc.polyfrost.oneconfig.gui.pages;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.SVGs;

public class HomePage extends Page {

    public HomePage() {
        super("Home Dashboard");
        /*socialsBtn.setClickAction(() -> NetworkUtils.browseLink("https://twitter.com/polyfrost"));
        discordBtn.setClickAction(() -> NetworkUtils.browseLink("https://discord.gg/4BdUuGpMdf"));
        webBtn.setClickAction(() -> NetworkUtils.browseLink("https://polyfrost.cc"));
        creditsBtn.setClickAction(new CreditsPage());
        guideBtn.setClickAction(() -> NetworkUtils.browseLink("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));*/

    }

    public void draw(long vg, int x, int y) {
        RenderManager.drawRoundedRect(vg, x, y, 184, 36, -1, 12f);
        RenderManager.drawText(vg, "This is a cool string to test pages", x + 32, y + 72, -1, 36f, Fonts.BOLD);
        RenderManager.drawRoundedRect(vg, x + 350, y + 310, 300, 200, OneConfigConfig.PRIMARY_600, 14f);
        RenderManager.drawSvg(vg, SVGs.INFO_CIRCLE, x + 20, y + 604, 24, 24);
        RenderManager.drawText(vg, "Info", x + 52, y + 618, OneConfigConfig.WHITE_90, 24f, Fonts.MEDIUM);
        RenderManager.drawRoundedRect(vg, x + 16, y + 644, 1024, 64, OneConfigConfig.GRAY_700, 20f);

        RenderManager.drawURL(vg, "https://www.youtube.com/watch?v=dQw4w9WgXcQ", x + 100, y + 205, 24, Fonts.MEDIUM);

        /*discordBtn.draw(vg, x + 32, y + 658);
        webBtn.draw(vg, x + 232, y + 658);
        socialsBtn.draw(vg, x + 432, y + 658);
        creditsBtn.draw(vg, x + 632, y + 658);
        guideBtn.draw(vg, x + 832, y + 658);
        if (socialsBtn.isClicked()) {
            OneConfigGui.INSTANCE.initColorSelector(new ColorSelector(new OneColor(new Color(255, 0, 255, 127)), InputUtils.mouseX(), InputUtils.mouseY()));
        }

        button1.draw(vg, x + 100, y + 100);
        button2.draw(vg, x + 100, y + 150);
        button3.draw(vg, x + 100, y + 250);
        button4.draw(vg, x + 100, y + 400);
        button5.draw(vg, x + 100, y + 600);
        button6.draw(vg, x + 350, y + 100);
        button7.draw(vg, x + 350, y + 300);
        button8.draw(vg, x + 350, y + 450);*/
    }

    @Override
    public boolean isBase() {
        return true;
    }
}
