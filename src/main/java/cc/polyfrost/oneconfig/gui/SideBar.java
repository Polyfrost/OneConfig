package cc.polyfrost.oneconfig.gui;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.gui.elements.BasicButton;
import cc.polyfrost.oneconfig.gui.pages.HomePage;
import cc.polyfrost.oneconfig.gui.pages.ModsPage;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.Images;
import cc.polyfrost.oneconfig.utils.MathUtils;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class SideBar {
    private final List<BasicButton> btnList = new ArrayList<>();

    private float targetY = 0, currentY = 0;

    public SideBar() {
        btnList.add(new BasicButton(192, 36, "Dashboard", Images.DASHBOARD, null, -3, BasicButton.ALIGNMENT_LEFT, new HomePage()));
        btnList.add(new BasicButton(192, 36, "Global Search", Images.SEARCH, null, -3, BasicButton.ALIGNMENT_LEFT));
        btnList.add(new BasicButton(192, 36, "Mods", Images.MODS, null, -3, BasicButton.ALIGNMENT_LEFT, new ModsPage()));
        btnList.add(new BasicButton(192, 36, "Performance", Images.PERFORMANCE, null, -3, BasicButton.ALIGNMENT_LEFT));
        btnList.add(new BasicButton(192, 36, "Profiles", Images.PROFILES, null, -3, BasicButton.ALIGNMENT_LEFT));
        btnList.add(new BasicButton(192, 36, "Updates", Images.UPDATES, null, -3, BasicButton.ALIGNMENT_LEFT));
        btnList.add(new BasicButton(192, 36, "Theme", Images.THEMES, null, -3, BasicButton.ALIGNMENT_LEFT));
        btnList.add(new BasicButton(192, 36, "Screenshots", Images.SCREENSHOT, null, -3, BasicButton.ALIGNMENT_LEFT));
        btnList.add(new BasicButton(192, 36, "HUD Settings", Images.HUD_SETTINGS, null, -3, BasicButton.ALIGNMENT_LEFT));
        btnList.add(new BasicButton(192, 36, "Preferences", Images.PREFERENCES, null, -3, BasicButton.ALIGNMENT_LEFT));
        btnList.add(new BasicButton(192, 36, "Close", Images.CLOSE, null, -1, BasicButton.ALIGNMENT_LEFT, () -> Minecraft.getMinecraft().displayGuiScreen(null)));
        btnList.add(new BasicButton(192, 36, "Minimize", Images.MINIMIZE, null, -1, BasicButton.ALIGNMENT_LEFT));
        btnList.add(new BasicButton(192, 36, "Edit HUD", Images.HUD, null, 0, BasicButton.ALIGNMENT_LEFT, () -> Minecraft.getMinecraft().displayGuiScreen(new HudGui())));
    }

    public void draw(long vg, int x, int y) {
        //percentMove = 36f;

        currentY = MathUtils.easeInOutCirc(50, currentY, targetY - currentY, 120);
        RenderManager.drawRoundedRect(vg, x + 16, currentY, 192, 36, OneConfigConfig.BLUE_600, OneConfigConfig.CORNER_RADIUS);
        int i = 0;
        if (targetY == 0) {
            targetY = y + 96;
            currentY = targetY;
        }
        for (BasicButton btn : btnList) {
            btn.draw(vg, x + 16, y + 96 + i);
            i += 44;
            if (i == 88) { // +88
                RenderManager.drawString(vg, "MOD CONFIG", x + 16, y + 200, OneConfigConfig.WHITE_90, 12f, Fonts.INTER_SEMIBOLD);
                i = 122;
            }
            if (i == 298) {
                RenderManager.drawString(vg, "PERSONALIZATION", x + 16, y + 420, OneConfigConfig.WHITE_90, 12f, Fonts.INTER_SEMIBOLD);
                i = 342;
            }
            if (i == 518) {
                i = 562;
            }

            if (btn.isClicked() && btn.getPage() != null) {
                if (i < 520) targetY = btn.y;
            }
        }
    }
}
