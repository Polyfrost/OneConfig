package cc.polyfrost.oneconfig.gui;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.gui.elements.BasicElement;
import cc.polyfrost.oneconfig.gui.elements.ColorSelector;
import cc.polyfrost.oneconfig.gui.elements.text.TextInputField;
import cc.polyfrost.oneconfig.gui.pages.HomePage;
import cc.polyfrost.oneconfig.gui.pages.Page;
import cc.polyfrost.oneconfig.lwjgl.OneColor;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.Images;
import cc.polyfrost.oneconfig.lwjgl.image.SVGs;
import cc.polyfrost.oneconfig.lwjgl.scissor.Scissor;
import cc.polyfrost.oneconfig.lwjgl.scissor.ScissorManager;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.utils.MathUtils;
import gg.essential.universal.UKeyboard;
import gg.essential.universal.UMatrixStack;
import gg.essential.universal.UResolution;
import gg.essential.universal.UScreen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Mouse;
import org.lwjgl.nanovg.NanoVG;

import java.util.ArrayList;

public class OneConfigGui extends UScreen {
    public static OneConfigGui INSTANCE;
    public final int x = 320;
    public final int y = 140;
    private final SideBar sideBar = new SideBar();
    protected Page currentPage;
    protected Page prevPage;
    private float pageProgress = -224f;
    private final TextInputField textInputField = new TextInputField(248, 40, "Search...", false, false);
    private final ArrayList<Page> previousPages = new ArrayList<>();
    private final ArrayList<Page> nextPages = new ArrayList<>();
    private final BasicElement backArrow = new BasicElement(40, 40, -1, false);
    private final BasicElement forwardArrow = new BasicElement(40, 40, -1, false);
    private final ArrayList<Page> parents = new ArrayList<>();
    private ColorSelector currentColorSelector;
    public boolean mouseDown;
    private float scale = 1f;
    public static OneConfigGui instanceToRestore = null;
    public boolean allowClose = true;

    public OneConfigGui() {
        INSTANCE = this;
        instanceToRestore = null;
    }

    public OneConfigGui(Page page) {
        INSTANCE = this;
        instanceToRestore = null;
        currentPage = page;
    }

    public static OneConfigGui create() {
        try {
            return instanceToRestore == null ? new OneConfigGui() : instanceToRestore;
        } finally {
            instanceToRestore = null;
        }
    }

    @Override
    public void onDrawScreen(@NotNull UMatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.onDrawScreen(matrixStack, mouseX, mouseY, partialTicks);
        long start = System.nanoTime();
        RenderManager.setupAndDraw((vg) -> {
            if (currentPage == null) {
                currentPage = new HomePage();
                parents.add(currentPage);
            }
            scale = UResolution.getWindowWidth() / 1920f;
            NanoVG.nvgScale(vg, scale, scale);
            if (OneConfigConfig.ROUNDED_CORNERS) {
                RenderManager.drawRoundedRect(vg, x + 224, y, 1056, 800, OneConfigConfig.GRAY_800, OneConfigConfig.CORNER_RADIUS_WIN);
                RenderManager.drawRoundedRect(vg, x, y, 244, 800, OneConfigConfig.GRAY_900_80, OneConfigConfig.CORNER_RADIUS_WIN);
                RenderManager.drawRect(vg, x + 224, y, 20, 800, OneConfigConfig.GRAY_800);
                //RenderManager.drawDropShadow(vg, 544, 140, 1056, 800, 20f, 32f, OneConfigConfig.GRAY_800);
            }
            RenderManager.drawLine(vg, x + 224, y + 72, x + 1280, y + 72, 1, OneConfigConfig.GRAY_700);
            RenderManager.drawLine(vg, x + 224, y, x + 222, y + 800, 1, OneConfigConfig.GRAY_700);

            RenderManager.drawSvg(vg, SVGs.ONECONFIG, x + 19, y + 19, 42, 42);
            RenderManager.drawString(vg, "OneConfig", x + 69, y + 32, OneConfigConfig.WHITE, 18f, Fonts.BOLD);        // added half line height to center text
            RenderManager.drawString(vg, "ALPHA - By Polyfrost", x + 69, y + 51, OneConfigConfig.WHITE, 12f, Fonts.REGULAR);


            //RenderManager.drawRect(vg, x + 300, y + 500, 400, 12, OneConfigConfig.ERROR_700);
            //RenderManager.drawString(vg, "MoonTidez is Annoyinhg here is an f |||", x + 300, y + 500, OneConfigConfig.WHITE, 14f, 14,Fonts.INTER_REGULAR);

            textInputField.draw(vg, x + 1020, y + 16);
            sideBar.draw(vg, x, y);
            backArrow.draw(vg, x + 240, y + 16);
            forwardArrow.draw(vg, x + 280, y + 16);

            if (previousPages.size() == 0) {
                backArrow.disable(true);
                NanoVG.nvgGlobalAlpha(vg, 0.5f);
            } else {
                backArrow.disable(false);
                if (!backArrow.isHovered() || Mouse.isButtonDown(0)) NanoVG.nvgGlobalAlpha(vg, 0.8f);
            }
            RenderManager.drawSvg(vg, SVGs.ARROW_CIRCLE_LEFT, x + 249, y + 25, 22, 22);
            NanoVG.nvgGlobalAlpha(vg, 1f);
            if (nextPages.size() == 0) {
                forwardArrow.disable(true);
                NanoVG.nvgGlobalAlpha(vg, 0.5f);
            } else {
                forwardArrow.disable(false);
                if (!forwardArrow.isHovered() || Mouse.isButtonDown(0)) NanoVG.nvgGlobalAlpha(vg, 0.8f);
            }
            RenderManager.drawSvg(vg, SVGs.ARROW_CIRCLE_RIGHT, x + 289, y + 25, 22, 22);
            NanoVG.nvgGlobalAlpha(vg, 1f);

            if (backArrow.isClicked() && previousPages.size() > 0) {
                try {
                    nextPages.add(0, currentPage);
                    openPage(previousPages.get(0), false);
                    previousPages.remove(0);
                } catch (Exception ignored) {
                }
            } else if (forwardArrow.isClicked() && nextPages.size() > 0) {
                try {
                    previousPages.add(0, currentPage);
                    openPage(nextPages.get(0), false);
                    nextPages.remove(0);
                } catch (Exception ignored) {
                }
            }

            Scissor scissor = ScissorManager.scissor(vg, x + 224, y + 88, 1056, 698);
            if (prevPage != null) {
                pageProgress = MathUtils.easeInOutCirc(50, pageProgress, 832 - pageProgress, 220);
                prevPage.scrollWithDraw(vg, (int) (x - pageProgress), y + 72);
                RenderManager.drawLine(vg, (int) (x - pageProgress + 1055), y + 72, (int) (x - pageProgress + 1057), y + 800, 2, OneConfigConfig.GRAY_700);     // TODO might remove this
                currentPage.scrollWithDraw(vg, (int) (x - pageProgress + 1056), y + 72);
                if (pageProgress > 830f) {      // this number is the 'snap' point of the page
                    prevPage = null;
                    pageProgress = -224f;
                }
            } else {
                currentPage.scrollWithDraw(vg, (int) (x - pageProgress), y + 72);
            }
            ScissorManager.resetScissor(vg, scissor);

            float breadcrumbX = x + 336;
            for (int i = 0; i < parents.size(); i++) {
                String title = parents.get(i).getTitle();
                float width = RenderManager.getTextWidth(vg, title, 24f, Fonts.SEMIBOLD);
                boolean hovered = InputUtils.isAreaHovered((int) breadcrumbX, y + 24, (int) width, 36);
                int color = OneConfigConfig.WHITE_60;
                if (i == parents.size() - 1) color = OneConfigConfig.WHITE_95;
                else if (hovered && !Mouse.isButtonDown(0)) color = OneConfigConfig.WHITE_80;
                RenderManager.drawString(vg, title, breadcrumbX, y + 38, color, 24f, Fonts.SEMIBOLD);
                if (i != 0)
                    RenderManager.drawSvg(vg, SVGs.CHEVRON_RIGHT, breadcrumbX - 28, y + 25, 24, 24, color);
                if (hovered && InputUtils.isClicked()) openPage(parents.get(i));
                breadcrumbX += width + 32;
            }

            long end = System.nanoTime() - start;
            String s = (" draw: " + end / 1000000f + "ms");
            RenderManager.drawString(vg, s, x + 1170, y + 790, OneConfigConfig.GRAY_300, 10f, Fonts.MEDIUM);
            if (currentColorSelector != null) {
                currentColorSelector.draw(vg);
            }
        });
        mouseDown = Mouse.isButtonDown(0);
    }

    @Override
    public void onKeyPressed(int keyCode, char typedChar, @Nullable UKeyboard.Modifiers modifiers) {
        UKeyboard.allowRepeatEvents(true);
        try {
            if (allowClose) super.onKeyPressed(keyCode, typedChar, modifiers);
            textInputField.keyTyped(typedChar, keyCode);
            if (currentColorSelector != null) currentColorSelector.keyTyped(typedChar, keyCode);
            currentPage.keyTyped(typedChar, keyCode);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("this should literally never happen");
        }
    }

    public void openPage(@NotNull Page page) {
        openPage(page, true);
    }

    public void openPage(@NotNull Page page, boolean addToPrevious) {
        if (page == currentPage) return;
        currentPage.finishUpAndClose();
        if (!page.isBase()) {
            boolean alreadyInParents = false;
            for (int i = 0; i < parents.size(); i++) {
                Page parent = parents.get(i);
                if (parent == page) {
                    alreadyInParents = true;
                    parents.subList(i + 1, parents.size()).clear();
                    break;
                }
            }
            if (!alreadyInParents) parents.add(page);
        } else {
            parents.clear();
            parents.add(page);
        }
        if (addToPrevious) {
            previousPages.add(0, currentPage);
            nextPages.clear();
        }
        if (prevPage == null) {
            prevPage = currentPage;
        }
        currentPage = page;
    }

    /**
     * initialize a new ColorSelector and add it to the draw script. This method is used to make sure it is always rendered on top.
     *
     * @implNote Correct usage: <code>OneConfigGui.INSTANCE.initColorSelector(new ColorSelector(color, InputUtils.mouseX(), InputUtils.mouseY()));</code>
     */
    public void initColorSelector(ColorSelector colorSelector) {
        currentColorSelector = colorSelector;
    }

    /**
     * Close the current color selector and return the color it had when it closed.
     */
    public OneColor closeColorSelector() {
        currentColorSelector.onClose();
        OneColor color = currentColorSelector.getColor();
        currentColorSelector = null;
        return color;
    }

    public float getScaleFactor() {
        return scale;
    }

    public String getSearchValue() {
        return textInputField.getInput();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onScreenClose() {
        currentPage.finishUpAndClose();
        INSTANCE = null;
        super.onScreenClose();
    }
}
