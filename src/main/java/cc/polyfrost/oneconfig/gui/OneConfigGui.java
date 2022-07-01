package cc.polyfrost.oneconfig.gui;

import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.gui.animations.Animation;
import cc.polyfrost.oneconfig.gui.animations.EaseInOutQuad;
import cc.polyfrost.oneconfig.gui.elements.BasicElement;
import cc.polyfrost.oneconfig.gui.elements.ColorSelector;
import cc.polyfrost.oneconfig.gui.elements.text.TextInputField;
import cc.polyfrost.oneconfig.gui.pages.ModsPage;
import cc.polyfrost.oneconfig.gui.pages.Page;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.internal.config.OneConfigConfig;
import cc.polyfrost.oneconfig.internal.config.Preferences;
import cc.polyfrost.oneconfig.libs.universal.UKeyboard;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.libs.universal.UResolution;
import cc.polyfrost.oneconfig.libs.universal.UScreen;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.renderer.scissor.Scissor;
import cc.polyfrost.oneconfig.renderer.scissor.ScissorManager;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;
import cc.polyfrost.oneconfig.utils.gui.GuiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.nanovg.NanoVG;

import java.util.ArrayList;

public class OneConfigGui extends UScreen implements GuiPause {
    public static OneConfigGui INSTANCE;
    public static OneConfigGui instanceToRestore = null;
    private final SideBar sideBar = new SideBar();
    private final TextInputField textInputField = new TextInputField(248, 40, "Search...", false, false, SVGs.MAGNIFYING_GLASS_BOLD);
    private final ArrayList<Page> previousPages = new ArrayList<>();
    private final ArrayList<Page> nextPages = new ArrayList<>();
    private final BasicElement backArrow = new BasicElement(40, 40, new ColorPalette(Colors.GRAY_700, Colors.GRAY_500, Colors.GRAY_500_80), true);
    private final BasicElement forwardArrow = new BasicElement(40, 40, new ColorPalette(Colors.GRAY_700, Colors.GRAY_500, Colors.GRAY_500_80), true);
    public ColorSelector currentColorSelector;
    public boolean mouseDown;
    public boolean allowClose = true;
    protected Page currentPage;
    protected Page prevPage;
    private float scale = 1f;
    private Animation animation;

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
            if (instanceToRestore != null) INSTANCE = instanceToRestore;
        }
    }

    @Override
    public void onDrawScreen(@NotNull UMatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.onDrawScreen(matrixStack, mouseX, mouseY, partialTicks);
        long start = System.nanoTime();
        RenderManager.setupAndDraw((vg) -> {
            if (currentPage == null) {
                currentPage = new ModsPage();
                currentPage.parents.add(currentPage);
            }
            if (OneConfigConfig.australia) {
                NanoVG.nvgTranslate(vg, UResolution.getWindowWidth(), UResolution.getWindowHeight());
                NanoVG.nvgRotate(vg, (float) Math.toRadians(180));
            }
            scale = Preferences.enableCustomScale ? Preferences.customScale : Math.min(UResolution.getWindowWidth() / 1920f, UResolution.getWindowHeight() / 1080f);
            if (scale < 1 && !Preferences.enableCustomScale)
                scale = Math.min(Math.min(1f, UResolution.getWindowWidth() / 1280f), Math.min(1f, UResolution.getWindowHeight() / 800f));
            int x = (int) ((UResolution.getWindowWidth() - 1280 * scale) / 2f / scale);
            int y = (int) ((UResolution.getWindowHeight() - 800 * scale) / 2f / scale);
            RenderManager.scale(vg, scale, scale);
            if (Colors.ROUNDED_CORNERS) {
                RenderManager.drawDropShadow(vg, x, y, 1280, 800, 32, 0, 20);
                RenderManager.drawRoundedRect(vg, x + 224, y, 1056, 800, Colors.GRAY_800, Colors.CORNER_RADIUS_WIN);
                RenderManager.drawRoundedRect(vg, x, y, 244, 800, Colors.GRAY_800_95, Colors.CORNER_RADIUS_WIN);
                RenderManager.drawRect(vg, x + 224, y, 20, 800, Colors.GRAY_800);
                RenderManager.drawHollowRoundRect(vg, x - 1, y - 1, 1282, 802, 0x4DCCCCCC, 20, scale < 1 ? 1 / scale : 1);
            }
            RenderManager.drawLine(vg, x + 224, y + 72, x + 1280, y + 72, 1, Colors.GRAY_700);
            RenderManager.drawLine(vg, x + 224, y, x + 222, y + 800, 1, Colors.GRAY_700);

            RenderManager.drawSvg(vg, SVGs.ONECONFIG, x + 19, y + 19, 42, 42);
            RenderManager.drawText(vg, "OneConfig", x + 69, y + 32, -1, 18f, Fonts.BOLD);        // added half line height to center text
            RenderManager.drawText(vg, "By Polyfrost", x + 69, y + 51, -1, 12f, Fonts.REGULAR);

            textInputField.draw(vg, x + 1020, y + 16);
            sideBar.draw(vg, x, y);
            backArrow.draw(vg, x + 240, y + 16);
            forwardArrow.draw(vg, x + 288, y + 16);

            if (previousPages.size() == 0) {
                backArrow.disable(true);
                RenderManager.setAlpha(vg, 0.5f);
            } else {
                backArrow.disable(false);
                if (!backArrow.isHovered() || Platform.getMousePlatform().isButtonDown(0)) RenderManager.setAlpha(vg, 0.8f);
            }
            RenderManager.drawSvg(vg, SVGs.CARET_LEFT, x + 246, y + 22, 28, 28);
            RenderManager.setAlpha(vg, 1f);
            if (nextPages.size() == 0) {
                forwardArrow.disable(true);
                RenderManager.setAlpha(vg, 0.5f);
            } else {
                forwardArrow.disable(false);
                if (!forwardArrow.isHovered() || Platform.getMousePlatform().isButtonDown(0)) RenderManager.setAlpha(vg, 0.8f);
            }
            RenderManager.drawSvg(vg, SVGs.CARET_RIGHT, x + 294, y + 22, 28, 28);
            RenderManager.setAlpha(vg, 1f);

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
                    openPage(nextPages.get(0), new EaseInOutQuad(300, 224, 2128, true), false);
                    nextPages.remove(0);
                } catch (Exception ignored) {
                }
            }

            ScissorManager.scissor(vg, x + 224, y + 72, 1056, 728);
            Scissor blockedClicks = InputUtils.blockInputArea(x + 224, y, 1056,72);
            if (prevPage != null && animation != null) {
                float pageProgress = animation.get(GuiUtils.getDeltaTime());
                if (!animation.isReversed()) {
                    prevPage.scrollWithDraw(vg, (int) (x + pageProgress), y + 72);
                    currentPage.scrollWithDraw(vg, (int) (x - 1904 + pageProgress), y + 72);
                } else {
                    prevPage.scrollWithDraw(vg, (int) (x - 1904 + pageProgress), y + 72);
                    currentPage.scrollWithDraw(vg, (int) (x + pageProgress), y + 72);
                }
                if (animation.isFinished()) {
                    prevPage = null;
                }
            } else {
                currentPage.scrollWithDraw(vg, x + 224, y + 72);
            }
            ScissorManager.clearScissors(vg);
            InputUtils.stopBlock(blockedClicks);

            float breadcrumbX = x + 352;
            for (int i = 0; i < currentPage.parents.size(); i++) {
                String title = currentPage.parents.get(i).getTitle();
                float width = RenderManager.getTextWidth(vg, title, 24f, Fonts.SEMIBOLD);
                boolean hovered = InputUtils.isAreaHovered((int) breadcrumbX, y + 24, (int) width, 36);
                int color = Colors.WHITE_60;
                if (i == currentPage.parents.size() - 1) color = Colors.WHITE;
                else if (hovered && !Platform.getMousePlatform().isButtonDown(0)) color = Colors.WHITE_80;
                RenderManager.drawText(vg, title, breadcrumbX, y + 38, color, 24f, Fonts.SEMIBOLD);
                if (i != 0)
                    RenderManager.drawSvg(vg, SVGs.CARET_RIGHT, breadcrumbX - 28, y + 25, 24, 24, color);
                if (hovered && InputUtils.isClicked()) openPage(currentPage.parents.get(i));
                breadcrumbX += width + 32;
            }

            long end = System.nanoTime() - start;
            String s = (" draw: " + end / 1000000f + "ms");
            RenderManager.drawText(vg, s, x + 1170, y + 792, Colors.GRAY_300, 10f, Fonts.MEDIUM);
            if (currentColorSelector != null) {
                currentColorSelector.draw(vg);
            }
        });
        mouseDown = Platform.getMousePlatform().isButtonDown(0);
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
        }
    }

    public void openPage(@NotNull Page page) {
        openPage(page, true);
    }

    public void openPage(@NotNull Page page, boolean addToPrevious) {
        openPage(page, new EaseInOutQuad(300, 224, 2128, false), addToPrevious);
    }

    public void openPage(@NotNull Page page, Animation animation, boolean addToPrevious) {
        if (page == currentPage) return;
        currentPage.finishUpAndClose();
        textInputField.setInput("");
        if (page.parents.size() == 0) {
            page.parents.addAll(currentPage.parents);
            if (!page.isBase()) {
                boolean alreadyInParents = false;
                for (int i = 0; i < page.parents.size(); i++) {
                    Page parent = page.parents.get(i);
                    if (parent == page) {
                        alreadyInParents = true;
                        page.parents.subList(i + 1, page.parents.size()).clear();
                        break;
                    }
                }
                if (!alreadyInParents) page.parents.add(page);
            } else {
                page.parents.clear();
                page.parents.add(page);
            }
        }
        sideBar.pageOpened(page.parents.get(0).getTitle());
        if (addToPrevious) {
            previousPages.add(0, currentPage);
            nextPages.clear();
        }
        if (prevPage == null) {
            prevPage = currentPage;
        }
        currentPage = page;
        this.animation = animation;
    }

    /**
     * initialize a new ColorSelector and add it to the draw script. This method is used to make sure it is always rendered on top.
     * <p>
     * Correct usage: <code>OneConfigGui.INSTANCE.initColorSelector(new ColorSelector(color, InputUtils.mouseX(), InputUtils.mouseY()));</code>
     */
    public void initColorSelector(ColorSelector colorSelector) {
        if (currentColorSelector != null) closeColorSelector();
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

    public OneColor getColor() {
        if (currentColorSelector == null) return null;
        return currentColorSelector.getColor();
    }

    public float getScaleFactor() {
        return scale;
    }

    public String getSearchValue() {
        return textInputField.getInput();
    }

    @Override
    public void onScreenClose() {
        currentPage.finishUpAndClose();
        instanceToRestore = this;
        INSTANCE = null;
        super.onScreenClose();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
