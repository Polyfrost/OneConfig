package cc.polyfrost.oneconfig.gui.pages;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.elements.BasicButton;
import cc.polyfrost.oneconfig.gui.elements.ColorSelector;
import cc.polyfrost.oneconfig.lwjgl.OneColor;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.Images;
import cc.polyfrost.oneconfig.utils.InputUtils;

import java.awt.*;

public class HomePage extends Page {
    private final BasicButton btn = new BasicButton(184, 36, "Socials", Images.SHARE, Images.LAUNCH, 1, BasicButton.ALIGNMENT_CENTER);

    public HomePage() {
        super("Home Dashboard");
    }

    public void draw(long vg, int x, int y) {
        RenderManager.drawRoundedRect(vg, x, y, 184, 36, -1, 12f);
        RenderManager.drawString(vg, "This is a cool string to test pages", x + 32, y + 72, -1, 36f, Fonts.BOLD);
        RenderManager.drawRoundedRect(vg, x + 350, y + 310, 300, 200, OneConfigConfig.BLUE_600, 14f);
        //RenderManager.drawRoundedRect(vg);
        btn.draw(vg, x + 432, y + 658);
        if(btn.isClicked()) {
            OneConfigGui.INSTANCE.initColorSelector(new ColorSelector(new OneColor(new Color(255, 0, 255, 127)), InputUtils.mouseX(), InputUtils.mouseY()));
        }
    }

    @Override
    public boolean isBase() {
        return true;
    }
}
