package io.polyfrost.oneconfig.gui;

import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.gui.elements.BasicElement;
import io.polyfrost.oneconfig.gui.elements.ColorSelector;
import io.polyfrost.oneconfig.gui.elements.TextInputField;
import io.polyfrost.oneconfig.gui.pages.HomePage;
import io.polyfrost.oneconfig.gui.pages.Page;
import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;
import io.polyfrost.oneconfig.lwjgl.image.Images;
import io.polyfrost.oneconfig.utils.MathUtils;
import net.minecraft.client.gui.GuiScreen;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.nanovg.NanoVG;

import java.awt.*;
import java.util.ArrayList;

import static org.lwjgl.nanovg.NanoVG.nvgResetScissor;
import static org.lwjgl.nanovg.NanoVG.nvgScissor;

public class OneConfigGui extends GuiScreen {
    public static OneConfigGui INSTANCE;

    public final int x = 320;
    public final int y = 140;

    private final SideBar sideBar = new SideBar();

    protected Page currentPage;
    protected Page prevPage;
    private float pageProgress = -224f;

    private final TextInputField textInputField = new TextInputField(248, 40, "Search all of OneConfig...", false, false);
    private final ArrayList<Page> pageHistory = new ArrayList<>();
    private int currentPageIndex = 0;
    private final BasicElement backArrow = new BasicElement(40, 40, -1, true);
    private final BasicElement forwardArrow = new BasicElement(40, 40, -1, true);

    private ColorSelector currentColorSelector;

    public boolean mouseDown;

    public OneConfigGui() {
        INSTANCE = this;
    }

    public OneConfigGui(Page page) {
        INSTANCE = this;
        currentPage = page;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        long start = System.nanoTime();
        RenderManager.setupAndDraw((vg) -> {
            if (currentPage == null) currentPage = new HomePage();
            //nvgScale(vg, 0.5f, 0.5f);
            if (OneConfigConfig.ROUNDED_CORNERS) {
                RenderManager.drawRoundedRect(vg, 544, 140, 1056, 800, OneConfigConfig.GRAY_800, OneConfigConfig.CORNER_RADIUS_WIN);
                RenderManager.drawRoundedRect(vg, 320, 140, 244, 800, OneConfigConfig.GRAY_900_80, OneConfigConfig.CORNER_RADIUS_WIN);
                RenderManager.drawRect(vg, 544, 140, 20, 800, OneConfigConfig.GRAY_800);
                //RenderManager.drawDropShadow(vg, 544, 140, 1056, 800, 20f, 32f, OneConfigConfig.GRAY_800);
            }

            RenderManager.drawLine(vg, 544, 212, 1600, 212, 1, OneConfigConfig.GRAY_700);
            RenderManager.drawLine(vg, 544, 140, 544, 940, 1, OneConfigConfig.GRAY_700);

            RenderManager.drawImage(vg, Images.LOGO, x + 19, y + 19, 42, 42);
            RenderManager.drawString(vg, "OneConfig", x + 69, y + 32, OneConfigConfig.WHITE, 18f, Fonts.INTER_BOLD);        // added half line height to center text
            RenderManager.drawString(vg, "By Polyfrost", x + 69, y + 51, OneConfigConfig.WHITE, 12f, Fonts.INTER_REGULAR);
            textInputField.draw(vg, x + 1020, y + 16);
            sideBar.draw(vg, x, y);
            backArrow.draw(vg, x + 240, y + 16);
            forwardArrow.draw(vg, x + 280, y + 16);

            if (currentPageIndex <= 0) {
                backArrow.disable(true);
                NanoVG.nvgGlobalAlpha(vg, 0.5f);
            } else backArrow.disable(false);
            RenderManager.drawImage(vg, Images.ARROW_LEFT, x + 250, y + 26, 20, 20);
            NanoVG.nvgGlobalAlpha(vg, 1f);
            if (currentPageIndex > pageHistory.size() - 1) {
                forwardArrow.disable(true);
                NanoVG.nvgGlobalAlpha(vg, 0.5f);
            } else forwardArrow.disable(false);
            RenderManager.drawImage(vg, Images.ARROW_RIGHT, x + 290, y + 26, 20, 20);
            NanoVG.nvgGlobalAlpha(vg, 1f);

            /*if (backArrow.isClicked()) {      // TODO
                try {
                    openPage(pageHistory.get(currentPageIndex--));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (forwardArrow.isClicked()) {
                try {
                    pageHistory.add(currentPage);
                    openPage(pageHistory.get(currentPageIndex++));
                } catch (Exception ignored) {
                }
            }*/


            nvgScissor(vg, x + 224, y + 72, 1056, 728);
            if (prevPage != null) {
                pageProgress = MathUtils.easeInOutCirc(50, pageProgress, 832 - pageProgress, 220);
                prevPage.draw(vg, (int) (x - pageProgress), y + 72);
                RenderManager.drawLine(vg, (int) (x - pageProgress + 1055), y + 72, (int) (x - pageProgress + 1057), y + 800, 2, OneConfigConfig.GRAY_700);     // TODO might remove this
                currentPage.draw(vg, (int) (x - pageProgress + 1056), y + 72);
                if (pageProgress > 830f) {      // this number is the 'snap' point of the page
                    prevPage = null;
                    pageProgress = -224f;
                }
            } else {
                currentPage.draw(vg, (int) (x - pageProgress), y + 72);
            }
            nvgResetScissor(vg);
            if(currentColorSelector != null) {
                currentColorSelector.draw(vg);
            }
            long end = System.nanoTime() - start;
            String s = (" draw: " + end / 1000000f + "ms");
            RenderManager.drawString(vg, currentPage.getTitle(), x + 336, y + 36, OneConfigConfig.WHITE_90, 32f, Fonts.INTER_SEMIBOLD);
            RenderManager.drawString(vg, s, x + 1170, y + 790, OneConfigConfig.GRAY_300, 10f, Fonts.INTER_MEDIUM);
        });
        mouseDown = Mouse.isButtonDown(0);
    }

    protected void keyTyped(char key, int keyCode) {
        Keyboard.enableRepeatEvents(true);
        try {
            super.keyTyped(key, keyCode);
            textInputField.keyTyped(key, keyCode);
            currentPage.keyTyped(key, keyCode);
        } catch (Exception e) {
            System.out.println("this should literally never happen");
        }
    }

    public void openPage(@NotNull Page page) {
        if (page == currentPage) return;
        currentPage.finishUpAndClose();
        if (prevPage == null) {
            prevPage = currentPage;
        }
        currentPage = page;
    }

    /**
     * initialize a new ColorSelector and add it to the draw script. This method is used to make sure it is always rendered on top.
     * @implNote Correct usage: <code>OneConfigGui.INSTANCE.initColorSelector(new ColorSelector(color, InputUtils.mouseX(), InputUtils.mouseY()));</code>
     */
    public void initColorSelector(ColorSelector colorSelector) {
        currentColorSelector = colorSelector;
    }

    /** Close the current color selector and return the color it had when it closed.
     */
    public Color closeColorSelector() {
        Color color = currentColorSelector.getColor();
        currentColorSelector = null;
        return color;
    }


    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onGuiClosed() {
        currentPage.finishUpAndClose();
    }
}
