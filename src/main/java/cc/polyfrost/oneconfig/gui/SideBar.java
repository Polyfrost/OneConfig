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
        btnList.add(new BasicButton(192, 36, "Screenshots", Images.SCREENSHOT, null, -3, BasicButton.ALIGNMENT_LEFT));
        btnList.add(new BasicButton(192, 36, "Preferences", Images.PREFERENCES, null, -3, BasicButton.ALIGNMENT_LEFT));
        btnList.add(new BasicButton(192, 36, "Mods", Images.MODS, null, -3, BasicButton.ALIGNMENT_LEFT, new ModsPage()));
        btnList.add(new BasicButton(192, 36, "Performance", Images.PERFORMANCE, null, -3, BasicButton.ALIGNMENT_LEFT));
        btnList.add(new BasicButton(192, 36, "Profiles", Images.PROFILES, null, -3, BasicButton.ALIGNMENT_LEFT));
        btnList.add(new BasicButton(192, 36, "Updates", Images.UPDATES, null, -3, BasicButton.ALIGNMENT_LEFT));
        btnList.add(new BasicButton(192, 36, "Themes Library", Images.THEMES, null, -3, BasicButton.ALIGNMENT_LEFT));
        btnList.add(new BasicButton(192, 36, "Themes Browser", Images.SEARCH, null, -3, BasicButton.ALIGNMENT_LEFT));
        btnList.add(new BasicButton(192, 36, "Packs Library", Images.MOD_BOX, null, -3, BasicButton.ALIGNMENT_LEFT));
        btnList.add(new BasicButton(192, 36, "Packs Browser", Images.SEARCH, null, -3, BasicButton.ALIGNMENT_LEFT));
        btnList.add(new BasicButton(192, 36, "Close", Images.CLOSE, null, -1, BasicButton.ALIGNMENT_LEFT, () -> Minecraft.getMinecraft().displayGuiScreen(null)));
        btnList.add(new BasicButton(192, 36, "Minimize", Images.MINIMIZE, null, -1, BasicButton.ALIGNMENT_LEFT, () -> {
            OneConfigGui.instanceToRestore = OneConfigGui.INSTANCE;
            Minecraft.getMinecraft().displayGuiScreen(null);
        }));
        btnList.add(new BasicButton(192, 36, "Edit HUD", Images.HUD, null, 0, BasicButton.ALIGNMENT_LEFT, () -> Minecraft.getMinecraft().displayGuiScreen(new HudGui())));
    }

    public void draw(long vg, int x, int y) {
        currentY = MathUtils.easeInOutCirc(50, currentY, targetY - currentY, 120);
        RenderManager.drawRoundedRect(vg, x + 16, currentY, 192, 36, OneConfigConfig.BLUE_600, OneConfigConfig.CORNER_RADIUS);
        int i = 0;
        if (targetY == 0) {
            targetY = y + 96;
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
                if (i < 520) targetY = btn.y;
            }
        }
    }
}
