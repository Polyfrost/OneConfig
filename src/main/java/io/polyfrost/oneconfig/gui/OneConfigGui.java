package io.polyfrost.oneconfig.gui;

import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.gui.elements.BasicElement;
import io.polyfrost.oneconfig.gui.elements.TextInputField;
import io.polyfrost.oneconfig.gui.pages.HomePage;
import io.polyfrost.oneconfig.gui.pages.Page;
import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;
import io.polyfrost.oneconfig.utils.MathUtils;
import net.minecraft.client.gui.GuiScreen;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;

import static org.lwjgl.nanovg.NanoVG.*;

public class OneConfigGui extends GuiScreen {
    public static OneConfigGui INSTANCE;
    private final BasicElement element = new BasicElement(200, 200, 1, true);

    public final int x = 320;
    public final int y = 140;

    private final SideBar sideBar = new SideBar();

    protected Page currentPage;
    protected Page prevPage;
    private float pageProgress = -224f;

    private final TextInputField textInputField = new TextInputField(776, 32, "Search all of OneConfig...", false, false);


    public OneConfigGui() {
        INSTANCE = this;
    }
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        RenderManager.setupAndDraw((vg) -> {
            if(currentPage == null) currentPage = new HomePage();
            //nvgScale(vg, 0.5f, 0.5f);
            if(OneConfigConfig.ROUNDED_CORNERS) {
                RenderManager.drawRoundedRect(vg, 544, 140, 1056, 800, OneConfigConfig.GRAY_800, OneConfigConfig.CORNER_RADIUS_WIN);
                RenderManager.drawRoundedRect(vg, 320, 140, 244, 800, OneConfigConfig.GRAY_900_80, OneConfigConfig.CORNER_RADIUS_WIN);
                RenderManager.drawRect(vg, 544, 140, 20, 800, OneConfigConfig.GRAY_800);
                //RenderManager.drawDropShadow(vg, 544, 140, 1056, 800, 20f, 32f, OneConfigConfig.GRAY_800);
            } else {
                // L;
            }

            RenderManager.drawLine(vg, 544, 212, 1600, 212, 1,  OneConfigConfig.GRAY_700);
            RenderManager.drawLine(vg, 544, 140, 544, 940, 1, OneConfigConfig.GRAY_700);

            RenderManager.drawImage(vg, "/assets/oneconfig/textures/icon.png", x + 19, y + 19, 42, 42);
            RenderManager.drawString(vg, "OneConfig", x + 69, y + 32, OneConfigConfig.WHITE, 18f, Fonts.INTER_BOLD);        // added half line height to center text
            RenderManager.drawString(vg, "By Polyfrost", x + 69, y + 51, OneConfigConfig.WHITE, 12f, Fonts.INTER_REGULAR);
            //element.setColorPalette(0);
            sideBar.draw(vg, x, y);
            nvgScissor(vg, x + 224, y + 72, 1056, 728);
            if(prevPage != null) {
                pageProgress = MathUtils.easeInOutCirc(50, pageProgress, 832 - pageProgress, 220);
                prevPage.draw(vg, (int) (x - pageProgress), y + 72);
                RenderManager.drawLine(vg, (int) (x - pageProgress + 1055), y + 72, (int) (x - pageProgress + 1057), y + 800, 2, OneConfigConfig.GRAY_700);     // TODO might remove this
                currentPage.draw(vg, (int) (x - pageProgress + 1056), y + 72);
                if(pageProgress > 828f) {
                    prevPage = null;
                    pageProgress = -224f;
                }
            } else {
                currentPage.draw(vg, (int) (x - pageProgress), y + 72);
            }
            nvgResetScissor(vg);
            RenderManager.drawString(vg, currentPage.getTitle(), x + 336, y + 36, OneConfigConfig.WHITE_90, 32f, Fonts.INTER_SEMIBOLD);

                //textInputField.draw(vg, 792, 548);
                //btn.draw(vg, 976, 870);

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

    public void openPage(@NotNull Page page) {
        if(prevPage == null) {
            prevPage = currentPage;
        }
        currentPage = page;
    }


    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

}
