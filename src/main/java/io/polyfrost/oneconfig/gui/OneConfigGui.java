package io.polyfrost.oneconfig.gui;

import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.gui.elements.BasicButton;
import io.polyfrost.oneconfig.gui.elements.BasicElement;
import io.polyfrost.oneconfig.gui.elements.TextInputField;
import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

public class OneConfigGui extends GuiScreen {
    public static OneConfigGui INSTANCE;
    private final BasicElement element = new BasicElement(200, 200, 1, true);

    private final TextInputField textInputField = new TextInputField(776, 32, "Search all of OneConfig...", false, false);
    private final BasicButton btn = new BasicButton(184, 36, "Socials", "/assets/oneconfig/textures/share.png", "/assets/oneconfig/textures/share2.png", 1, true);

    public OneConfigGui() {
        INSTANCE = this;
    }
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        RenderManager.setupAndDraw((vg) -> {
            if(OneConfigConfig.ROUNDED_CORNERS) {
                RenderManager.drawRoundedRect(vg, 544, 140, 1056, 800, OneConfigConfig.GRAY_800, OneConfigConfig.CORNER_RADIUS_WIN);
                RenderManager.drawRoundedRect(vg, 320, 140, 244, 800, OneConfigConfig.GRAY_900_80, OneConfigConfig.CORNER_RADIUS_WIN);
                RenderManager.drawRect(vg, 544, 140, 20, 800, OneConfigConfig.GRAY_800);
            } else {
                // L;
            }

            RenderManager.drawLine(vg, 544, 212, 1600, 212, 1,  OneConfigConfig.GRAY_700);
            RenderManager.drawLine(vg, 544, 140, 544, 940, 1, OneConfigConfig.GRAY_700);

            RenderManager.drawString(vg, "OneConfig", 389, 163, OneConfigConfig.WHITE, 18f, Fonts.INTER_BOLD);
            RenderManager.drawString(vg, "By Polyfrost", 389, 183, OneConfigConfig.WHITE, 12f, Fonts.INTER_REGULAR);
            //element.setColorPalette(0);
            try {
                //element.draw(vg, 0, 0);
                textInputField.draw(vg, 792, 548);
                btn.draw(vg, 976, 870);
            } catch (Exception e) {
                e.printStackTrace();
            }

            //RenderManager.drawGradientRoundedRect(vg, 100, 100, 500, 100, OneConfigConfig.BLUE_600, OneConfigConfig.BLUE_500, OneConfigConfig.CORNER_RADIUS_WIN);

        });
    }

    protected void keyTyped(char key, int keyCode) {
        Keyboard.enableRepeatEvents(true);
        try {
            super.keyTyped(key, keyCode);
            textInputField.keyTyped(key, keyCode);
        } catch (Exception e) {
            System.out.println("this should literally never happen");
        }
    }


    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

}
