package cc.polyfrost.oneconfig.gui.pages;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.elements.BasicButton;
import cc.polyfrost.oneconfig.gui.elements.ColorSelector;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.SVGs;
import cc.polyfrost.oneconfig.utils.IOUtils;
import cc.polyfrost.oneconfig.utils.InputUtils;

import java.awt.*;

public class HomePage extends Page {
    private final BasicButton socialsBtn = new BasicButton(184, 36, "Socials", SVGs.SHARE, SVGs.POP_OUT, 1, BasicButton.ALIGNMENT_CENTER, () -> IOUtils.browseLink("https://twitter.com/polyfrost"));
    private final BasicButton discordBtn = new BasicButton(184, 36, "Discord", SVGs.WEBSITE, SVGs.LINK_DIAGONAL, 1, BasicButton.ALIGNMENT_CENTER, () -> IOUtils.browseLink("https://discord.gg/4BdUuGpMdf"));
    private final BasicButton webBtn = new BasicButton(184, 36, "Website", SVGs.WEBSITE, null, 1, BasicButton.ALIGNMENT_CENTER, () -> IOUtils.browseLink("https://polyfrost.cc"));
    private final BasicButton creditsBtn = new BasicButton(184, 36, "Credits", SVGs.AUDIO_PLAY, SVGs.LINK_DIAGONAL, 0, BasicButton.ALIGNMENT_CENTER, () -> OneConfigGui.INSTANCE.openPage(new CreditsPage()));
    private final BasicButton guideBtn = new BasicButton(184, 36, "Online Guide", SVGs.HELP_CIRCLE, null, 0, BasicButton.ALIGNMENT_CENTER, () -> IOUtils.browseLink("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));


    public HomePage() {
        super("Home Dashboard");
    }

    public void draw(long vg, int x, int y) {
        RenderManager.drawRoundedRect(vg, x, y, 184, 36, -1, 12f);
        RenderManager.drawString(vg, "This is a cool string to test pages", x + 32, y + 72, -1, 36f, Fonts.BOLD);
        RenderManager.drawRoundedRect(vg, x + 350, y + 310, 300, 200, OneConfigConfig.PRIMARY_600, 14f);
        RenderManager.drawSvg(vg, SVGs.INFO_CIRCLE, x + 20, y + 604, 24, 24);
        RenderManager.drawString(vg, "Info", x + 52, y + 618, OneConfigConfig.WHITE_90, 24f, Fonts.MEDIUM);
        RenderManager.drawRoundedRect(vg, x + 16, y + 644, 1024, 64, OneConfigConfig.GRAY_700, 20f);

        discordBtn.draw(vg, x + 32, y + 658);
        webBtn.draw(vg, x + 232, y + 658);
        socialsBtn.draw(vg, x + 432, y + 658);
        creditsBtn.draw(vg, x + 632, y + 658);
        guideBtn.draw(vg, x + 832, y + 658);
        if(socialsBtn.isClicked()) {
            OneConfigGui.INSTANCE.initColorSelector(new ColorSelector(new OneColor(new Color(255, 0, 255, 127)), InputUtils.mouseX(), InputUtils.mouseY()));
        }
    }

    @Override
    public boolean isBase() {
        return true;
    }
}
