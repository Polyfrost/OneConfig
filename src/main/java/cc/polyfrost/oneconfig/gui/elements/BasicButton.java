package cc.polyfrost.oneconfig.gui.elements;

import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.gui.pages.Page;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.Images;
import cc.polyfrost.oneconfig.lwjgl.image.SVGs;
import cc.polyfrost.oneconfig.utils.ColorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BasicButton extends BasicElement {

    protected String text;
    protected SVGs fileNameLeftIco, fileNameRightIco;
    private final int thisAlignment;
    private final float fontSize;
    private final int colorPalette;
    public int x, y;
    public static final int ALIGNMENT_LEFT = 0;
    public static final int ALIGNMENT_CENTER = 1;
    private boolean toggleable;
    private Page page;
    private Runnable runnable;
    private boolean alignIconLeft = false;

    /**
     * Create a new basic button. Used mostly on the homepage and the sidebar. Note: The button will not be drawn until you call {@link #draw(long, int, int)}.
     * The button's content is centered on its total length, so the text is not always in the middle.
     *
     * @param text             Text to display on the button. Has to be there.
     * @param fileNameLeftIco  file path of the icon to display on the left. Can be null if you don't want to display an icon on the left.
     * @param fileNameRightIco file path of the icon to display on the right. Can be null if you don't want to display an icon on the right.
     * @param colorPalette     color palette to use. see {@link ColorUtils} for more info. Can support color palette of -2, which is larger font and icons. Also supports -3, which is just the text changing color.
     * @param alignment        alignment of the button. ALIGNMENT_LEFT or ALIGNMENT_CENTER.
     */
    public BasicButton(int width, int height, @NotNull String text, @Nullable SVGs fileNameLeftIco, @Nullable SVGs fileNameRightIco, int colorPalette, int alignment) {
        super(width, height, colorPalette, true);
        this.text = text;
        if (fileNameLeftIco != null) this.fileNameLeftIco = fileNameLeftIco;
        if (fileNameRightIco != null) this.fileNameRightIco = fileNameRightIco;
        this.thisAlignment = alignment;
        if (colorPalette == -2) {
            fontSize = 24f;
            this.colorPalette = -1;
        } else {
            if (colorPalette == 0) fontSize = 12;
            else fontSize = 14f;
            this.colorPalette = colorPalette;
        }
    }

    public BasicButton(int width, int height, @NotNull String text, @Nullable SVGs fileNameLeftIco, @Nullable SVGs fileNameRightIco, int colorPalette, int alignment, Page page) {
        this(width, height, text, fileNameLeftIco, fileNameRightIco, colorPalette, alignment);
        this.page = page;
    }

    public BasicButton(int width, int height, @NotNull String text, @Nullable SVGs fileNameLeftIco, @Nullable SVGs fileNameRightIco, int colorPalette, int alignment, boolean toggleable) {
        this(width, height, text, fileNameLeftIco, fileNameRightIco, colorPalette, alignment);
        this.toggleable = toggleable;
    }

    public BasicButton(int width, int height, @NotNull String text, @Nullable SVGs fileNameLeftIco, @Nullable SVGs fileNameRightIco, int colorPalette, int alignment, Runnable runnable) {
        this(width, height, text, fileNameLeftIco, fileNameRightIco, colorPalette, alignment);
        this.runnable = runnable;
    }

    public BasicButton(int width, int height, @NotNull String text, @Nullable SVGs fileNameLeftIco, @Nullable SVGs fileNameRightIco, int colorPalette, int alignment, boolean toggleable, Runnable runnable) {
        this(width, height, text, fileNameLeftIco, fileNameRightIco, colorPalette, alignment, runnable);
        this.toggleable = toggleable;
    }

    @Override
    public void draw(long vg, int x, int y) {
        this.x = x;
        this.y = y;
        int textColor = -1;
        RenderManager.drawRectangle(vg, x, y, this.width, this.height, this.currentColor);
        float contentWidth = RenderManager.getTextWidth(vg, text, fontSize, Fonts.MEDIUM);
        if (fileNameLeftIco != null && !alignIconLeft) {
            contentWidth += 28;
        }
        if (fileNameRightIco != null) {
            contentWidth += 28;
        }

        if (this.colorPalette == -3) {
            textColor = OneConfigConfig.WHITE_80;
            if (hovered) textColor = OneConfigConfig.WHITE;
            if (clicked) textColor = OneConfigConfig.WHITE_80;
            if (page == null) textColor = OneConfigConfig.WHITE_50;
        }

        if (thisAlignment == ALIGNMENT_CENTER) {
            int middle = x + this.width / 2;
            if (alignIconLeft)
                RenderManager.drawString(vg, text, middle - contentWidth / 2, y + ((float) height / 2) + 1, textColor, fontSize, Fonts.MEDIUM);
            else
                RenderManager.drawString(vg, text, middle - contentWidth / 2 + (fileNameLeftIco != null ? 28 : 0), y + ((float) height / 2) + 1, textColor, fontSize, Fonts.MEDIUM);
            if (fileNameLeftIco != null) {
                if (alignIconLeft) RenderManager.drawSvg(vg, fileNameLeftIco, x + 12, y + height / 2f - 10, 20, 20);
                else RenderManager.drawSvg(vg, fileNameLeftIco, middle - contentWidth / 2, y + 8, 20, 20);
            }
            if (fileNameRightIco != null) {
                RenderManager.drawSvg(vg, fileNameRightIco, middle + contentWidth / 2 - (fileNameLeftIco != null ? 20 : 24), y + 8, 20, 20);
            }
        }
        if (thisAlignment == ALIGNMENT_LEFT) {
            if (fileNameLeftIco != null) {
                RenderManager.drawSvg(vg, fileNameLeftIco, x + 12, y + 8, 20, 20, textColor);
                RenderManager.drawString(vg, text, x + 40, y + ((float) height / 2) + 1, textColor, fontSize, Fonts.MEDIUM);
            } else {
                RenderManager.drawString(vg, text, x + 12, y + ((float) height / 2) + 1, textColor, fontSize, Fonts.MEDIUM);
            }
            if (fileNameRightIco != null) {
                RenderManager.drawSvg(vg, fileNameRightIco, x + width - 28, y + 8, 20, 20);
            }
        }
        this.update(x, y);
        if (hoverFx) {
            if (colorPalette == -3) {
                currentColor = OneConfigConfig.TRANSPARENT;
                return;
            }
            if (!toggleable) {
                currentColor = ColorUtils.getColor(currentColor, colorPalette, hovered, clicked);
            } else {
                if (toggled) {
                    currentColor = ColorUtils.smoothColor(currentColor, OneConfigConfig.GRAY_500, OneConfigConfig.BLUE_600, true, 30f, OneConfigGui.INSTANCE.getDeltaTime());
                } else currentColor = ColorUtils.getColor(currentColor, colorPalette, hovered, clicked);
            }
        }
    }


    @Override
    public void onClick() {
        if (this.page != null) {
            OneConfigGui.INSTANCE.openPage(page);
        } else if (this.runnable != null) {
            runnable.run();
        }
    }

    @Override
    public void update(int x, int y) {
        if (toggleable && toggled) return;
        super.update(x, y);
    }

    public Page getPage() {
        return page;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void alignIconLeft(boolean value) {
        alignIconLeft = value;
    }
}
