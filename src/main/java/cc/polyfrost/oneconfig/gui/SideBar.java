package cc.polyfrost.oneconfig.gui;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.gui.elements.BasicButton;
import cc.polyfrost.oneconfig.gui.pages.HomePage;
import cc.polyfrost.oneconfig.gui.pages.ModsPage;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.SVGs;
import cc.polyfrost.oneconfig.utils.MathUtils;
import cc.polyfrost.oneconfig.libs.universal.UScreen;

import java.util.ArrayList;
import java.util.List;

import static cc.polyfrost.oneconfig.gui.elements.BasicButton.*;

public class SideBar {
    private final List<BasicButton> btnList = new ArrayList<>();

    private float targetY = 0, currentY = 0;

    public SideBar() {
        btnList.add(new BasicButton(192, SIZE_36, "Dashboard", SVGs.DASHBOARD, null, ALIGNMENT_LEFT, -2));
        btnList.get(0).setClickAction(new HomePage());
        btnList.add(new BasicButton(192, SIZE_36, "Global Search", SVGs.SEARCH, null, ALIGNMENT_LEFT, -2));
        btnList.add(new BasicButton(192, SIZE_36, "Screenshots", SVGs.IMAGE, null, ALIGNMENT_LEFT, -2));
        btnList.add(new BasicButton(192, SIZE_36, "Preferences", SVGs.SETTINGS, null, ALIGNMENT_LEFT, -2));
        btnList.add(new BasicButton(192, 36, "Mods", SVGs.MODS, null, ALIGNMENT_LEFT, -2));
        btnList.get(4).setClickAction(new ModsPage());
        btnList.add(new BasicButton(192, SIZE_36, "Performance", SVGs.PERFORMANCE, null, ALIGNMENT_LEFT, -2));
        btnList.add(new BasicButton(192, SIZE_36, "Profiles", SVGs.PROFILES, null, ALIGNMENT_LEFT, -2));
        btnList.add(new BasicButton(192, SIZE_36, "Updates", SVGs.UPDATE, null, ALIGNMENT_LEFT, -2));
        btnList.add(new BasicButton(192, SIZE_36, "Themes Library", SVGs.THEME, null, ALIGNMENT_LEFT, -2));
        btnList.add(new BasicButton(192, SIZE_36, "Themes Browser", SVGs.SEARCH, null, ALIGNMENT_LEFT, -2));
        btnList.add(new BasicButton(192, SIZE_36, "Packs Library", SVGs.BOX, null, ALIGNMENT_LEFT, -2));
        btnList.add(new BasicButton(192, SIZE_36, "Packs Browser", SVGs.SEARCH, null, ALIGNMENT_LEFT, -2));
        btnList.add(new BasicButton(192, SIZE_36, "Close", SVGs.X_CIRCLE, null, ALIGNMENT_LEFT, -1));
        btnList.get(12).setClickAction(() -> UScreen.displayScreen(null));
        btnList.add(new BasicButton(192, SIZE_36, "Minimize", SVGs.MINIMISE, null, ALIGNMENT_LEFT, -1));
        btnList.get(13).setClickAction(() -> UScreen.displayScreen(null));
        btnList.add(new BasicButton(192, SIZE_36, "Edit HUD", SVGs.HUD, null, ALIGNMENT_LEFT, -1));
        btnList.get(14).setClickAction(() -> UScreen.displayScreen(new HudGui()));
    }

    public void draw(long vg, int x, int y) {
        currentY = MathUtils.easeInOutCirc(50, currentY, targetY - currentY, 400);
        RenderManager.drawRoundedRect(vg, x + 16, y + currentY, 192, 36, OneConfigConfig.PRIMARY_600, OneConfigConfig.CORNER_RADIUS);
        int i = 0;
        if (targetY == 0) {
            targetY = 96;
            currentY = targetY;
        }
        for (BasicButton btn : btnList) {
            btn.draw(vg, x + 16, y + 96 + i);
            if (i >= 562) i += 44;
            else i += 36;
            if (i == 144) {
                RenderManager.drawString(vg, "MOD CONFIG", x + 16, y + 266, OneConfigConfig.WHITE_80, 12f, Fonts.SEMIBOLD);
                i = 180;
            }
            if (i == 324) {
                RenderManager.drawString(vg, "PERSONALIZATION", x + 16, y + 446, OneConfigConfig.WHITE_80, 12f, Fonts.SEMIBOLD);
                i = 360;
            }
            if (i == 504) {
                i = 562;
            }

            if (btn.isClicked() && btn.getPage() != null) {
                if (i < 520) targetY = btn.y - y;
            }
        }
    }
}
