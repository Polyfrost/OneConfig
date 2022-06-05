package cc.polyfrost.oneconfig.gui;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.gui.animations.Animation;
import cc.polyfrost.oneconfig.gui.animations.DummyAnimation;
import cc.polyfrost.oneconfig.gui.animations.EaseInOutQuart;
import cc.polyfrost.oneconfig.gui.elements.BasicButton;
import cc.polyfrost.oneconfig.gui.pages.CreditsPage;
import cc.polyfrost.oneconfig.gui.pages.ModsPage;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.SVGs;
import cc.polyfrost.oneconfig.test.ButtonTestPage;
import cc.polyfrost.oneconfig.utils.gui.GuiUtils;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;

import java.util.ArrayList;

import static cc.polyfrost.oneconfig.gui.elements.BasicButton.ALIGNMENT_LEFT;
import static cc.polyfrost.oneconfig.gui.elements.BasicButton.SIZE_36;

public class SideBar {
    private final ArrayList<BasicButton> buttons = new ArrayList<BasicButton>() {{
        add(new BasicButton(192, SIZE_36, "Credits", SVGs.COPYRIGHT_FILL, null, ALIGNMENT_LEFT, ColorPalette.TERTIARY));
        add(new BasicButton(192, SIZE_36, "Global Search", SVGs.MAGNIFYING_GLASS_BOLD, null, ALIGNMENT_LEFT, ColorPalette.TERTIARY));
        add(new BasicButton(192, SIZE_36, "Mods", SVGs.FADERS_HORIZONTAL_BOLD, null, ALIGNMENT_LEFT, ColorPalette.PRIMARY));
        add(new BasicButton(192, SIZE_36, "Profiles", SVGs.USER_SWITCH_FILL, null, ALIGNMENT_LEFT, ColorPalette.TERTIARY));
        add(new BasicButton(192, SIZE_36, "Performance", SVGs.GAUGE_FILL, null, ALIGNMENT_LEFT, ColorPalette.TERTIARY));
        add(new BasicButton(192, SIZE_36, "Updates", SVGs.ARROWS_CLOCKWISE_BOLD, null, ALIGNMENT_LEFT, ColorPalette.TERTIARY));
        add(new BasicButton(192, SIZE_36, "Themes", SVGs.PAINT_BRUSH_BROAD_FILL, null, ALIGNMENT_LEFT, ColorPalette.TERTIARY));
        add(new BasicButton(192, SIZE_36, "Screenshots", SVGs.APERTURE_FILL, null, ALIGNMENT_LEFT, ColorPalette.TERTIARY));
        add(new BasicButton(192, SIZE_36, "Preferences", SVGs.GEAR_SIX_FILL, null, ALIGNMENT_LEFT, ColorPalette.TERTIARY));
    }};
    private final BasicButton HUDButton = new BasicButton(192, SIZE_36, "Edit HUD", SVGs.NOTE_PENCIL_BOLD, null, ALIGNMENT_LEFT, ColorPalette.SECONDARY);
    private final BasicButton CloseButton = new BasicButton(192, SIZE_36, "Close", SVGs.X_CIRCLE_BOLD, null, ALIGNMENT_LEFT, ColorPalette.SECONDARY_DESTRUCTIVE);

    private int selected = 2;
    private Animation moveAnimation = null;
    private Animation sizeAnimation = null;

    public SideBar() {
        buttons.get(0).setClickAction(new CreditsPage());
        buttons.get(2).setClickAction(new ModsPage());
        buttons.get(8).setClickAction(new ButtonTestPage());
        HUDButton.setClickAction(() -> GuiUtils.displayScreen(new HudGui()));
        CloseButton.setClickAction(GuiUtils::closeScreen);
        for (BasicButton button : buttons) {
            if (button.hasClickAction()) continue;
            button.disable(true);
        }
    }

    public void draw(long vg, int x, int y) {
        for (BasicButton button : buttons) {
            if (!button.isClicked()) continue;
            if (button.equals(buttons.get(selected))) break;
            buttons.get(selected).setColorPalette(ColorPalette.TERTIARY);
            moveAnimation = new EaseInOutQuart(300, buttons.get(selected).y, button.y, false);
            sizeAnimation = new DummyAnimation(36);
            selected = buttons.indexOf(button);
        }
        if (moveAnimation != null) {
            RenderManager.drawRoundedRect(vg, x + 16, moveAnimation.get() - (sizeAnimation.get() - 36) / 2f, 192, sizeAnimation.get(0), OneConfigConfig.PRIMARY_600, 12);
            if (moveAnimation.isFinished() && sizeAnimation.isFinished()) {
                moveAnimation = null;
                sizeAnimation = null;
                buttons.get(selected).setColorPalette(ColorPalette.PRIMARY);
            }
        }

        buttons.get(0).draw(vg, x + 16, y + 80);
        buttons.get(1).draw(vg, x + 16, y + 116);
        RenderManager.drawText(vg, "MOD CONFIG", x + 16, y + 178, OneConfigConfig.WHITE, 12, Fonts.SEMIBOLD);
        buttons.get(2).draw(vg, x + 16, y + 192);
        buttons.get(3).draw(vg, x + 16, y + 228);
        buttons.get(4).draw(vg, x + 16, y + 264);
        buttons.get(5).draw(vg, x + 16, y + 300);
        RenderManager.drawText(vg, "PERSONALIZATION", x + 16, y + 362, OneConfigConfig.WHITE, 12, Fonts.SEMIBOLD);
        buttons.get(6).draw(vg, x + 16, y + 376);
        buttons.get(7).draw(vg, x + 16, y + 412);
        buttons.get(8).draw(vg, x + 16, y + 448);
        HUDButton.draw(vg, x + 16, y + 704);
        CloseButton.draw(vg, x + 16, y + 748);
    }
}
