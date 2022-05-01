package io.polyfrost.oneconfig.gui;

import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.gui.elements.BasicButton;
import io.polyfrost.oneconfig.gui.pages.HomePage;
import io.polyfrost.oneconfig.gui.pages.ModConfigPage;
import io.polyfrost.oneconfig.gui.pages.ModsPage;
import io.polyfrost.oneconfig.gui.pages.Page;
import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;
import io.polyfrost.oneconfig.utils.MathUtils;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class SideBar {
    private final List<BasicButton> btnList = new ArrayList<>();

    private float targetY = 0, currentY = 0;

    public SideBar() {
        btnList.add(new BasicButton(192, 36, "Dashboard", "/assets/oneconfig/textures/Dashboard.png", null, -3, BasicButton.ALIGNMENT_LEFT, new HomePage()));
        btnList.add(new BasicButton(192, 36, "Global Search", "/assets/oneconfig/textures/search.png", null, -3, BasicButton.ALIGNMENT_LEFT));
        btnList.add(new BasicButton(192, 36, "Mods", "/assets/oneconfig/textures/Mods.png", null, -3, BasicButton.ALIGNMENT_LEFT, new ModsPage()));
        btnList.add(new BasicButton(192, 36, "Performance", "/assets/oneconfig/textures/Performance.png", null, -3, BasicButton.ALIGNMENT_LEFT));
        btnList.add(new BasicButton(192, 36, "Profiles", "/assets/oneconfig/textures/Profiles.png", null, -3, BasicButton.ALIGNMENT_LEFT));
        btnList.add(new BasicButton(192, 36, "Updates", "/assets/oneconfig/textures/Update.png", null, -3, BasicButton.ALIGNMENT_LEFT));
        btnList.add(new BasicButton(192, 36, "Theme", "/assets/oneconfig/textures/Theme.png", null, -3, BasicButton.ALIGNMENT_LEFT));
        btnList.add(new BasicButton(192, 36, "Screenshots", "/assets/oneconfig/textures/Image.png", null, -3, BasicButton.ALIGNMENT_LEFT));
        btnList.add(new BasicButton(192, 36, "HUD Settings", "/assets/oneconfig/textures/HUDSettings.png", null, -3, BasicButton.ALIGNMENT_LEFT));
        btnList.add(new BasicButton(192, 36, "Preferences", "/assets/oneconfig/textures/Settings.png", null, -3, BasicButton.ALIGNMENT_LEFT));
        btnList.add(new BasicButton(192, 36, "Close", "/assets/oneconfig/textures/XCircle.png", null, -1, BasicButton.ALIGNMENT_LEFT, () -> Minecraft.getMinecraft().displayGuiScreen(null)));
        btnList.add(new BasicButton(192, 36, "Minimize", "/assets/oneconfig/textures/Minimise.png", null, -1, BasicButton.ALIGNMENT_LEFT));
        btnList.add(new BasicButton(192, 36, "Edit HUD", "/assets/oneconfig/textures/HUD.png", null, 0, BasicButton.ALIGNMENT_LEFT, () -> Minecraft.getMinecraft().displayGuiScreen(new HudGui())));
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
