package io.polyfrost.oneconfig.gui.elements;

import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.gui.OneConfigGui;
import io.polyfrost.oneconfig.gui.pages.Page;
import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;
import io.polyfrost.oneconfig.utils.ColorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BasicButton extends BasicElement {

    protected String text;
    protected String fileNameLeftIco, fileNameRightIco;
    private final int thisAlignment;
    private final float fontSize;
    private final int colorPalette;

    public int x, y;
    public static final int ALIGNMENT_LEFT = 0;
    public static final int ALIGNMENT_CENTER = 1;

    private boolean toggleable;

    private Page page;

    /**
     * Create a new basic button. Used mostly on the homepage and the sidebar. Note: The button will not be drawn until you call {@link #draw(long, int, int)}.
     * The button's content is centered on its total length, so the text is not always in the middle.
     * @param text Text to display on the button. Has to be there.
     * @param fileNameLeftIco file path of the icon to display on the left. Can be null if you don't want to display an icon on the left.
     * @param fileNameRightIco file path of the icon to display on the right. Can be null if you don't want to display an icon on the right.
     * @param colorPalette color palette to use. see {@link io.polyfrost.oneconfig.utils.ColorUtils} for more info. Can support color palette of -2, which is larger font and icons. Also supports -3, which is just the text changing color.
     * @param alignment alignment of the button. ALIGNMENT_LEFT or ALIGNMENT_CENTER.
     */
    public BasicButton(int width, int height, @NotNull String text, @Nullable String fileNameLeftIco, @Nullable String fileNameRightIco, int colorPalette, int alignment) {
        super(width, height, colorPalette, true);
        this.text = text;
        this.fileNameLeftIco = fileNameLeftIco;
        this.fileNameRightIco = fileNameRightIco;
        this.thisAlignment = alignment;
        if(colorPalette == -2) {
            fontSize = 24f;
            this.colorPalette = -1;
        } else {
            fontSize = 14f;
            this.colorPalette = colorPalette;
        }
    }

    public BasicButton(int width, int height, @NotNull String text, @Nullable String fileNameLeftIco, @Nullable String fileNameRightIco, int colorPalette, int alignment, Page page) {
        this(width, height, text, fileNameLeftIco, fileNameRightIco, colorPalette, alignment);
        this.page = page;
    }

    public BasicButton(int width, int height, @NotNull String text, @Nullable String fileNameLeftIco, @Nullable String fileNameRightIco, int colorPalette, int alignment, boolean toggleable) {
        this(width, height, text, fileNameLeftIco, fileNameRightIco, colorPalette, alignment);
        this.toggleable = toggleable;
    }



    @Override
    public void draw(long vg, int x, int y) {
        this.x = x;
        this.y = y;
        int textColor = -1;
        RenderManager.drawRectangle(vg, x, y, this.width, this.height, this.currentColor);
        float contentWidth = RenderManager.getTextWidth(vg, text, fontSize);
        if(fileNameLeftIco != null) {
            contentWidth += 28;
        }
        if(fileNameRightIco != null) {
            contentWidth += 28;
        }

        if(this.colorPalette == -3) {
            textColor = OneConfigConfig.WHITE_80;
            if(hovered) textColor = OneConfigConfig.WHITE;
            if(clicked) textColor = OneConfigConfig.WHITE_80;
        }

        if(thisAlignment == ALIGNMENT_CENTER) {
            int middle = x + this.width / 2;
            RenderManager.drawString(vg, text, middle - contentWidth / 2 + (fileNameLeftIco != null ? 28 : 0), y + ((float) height / 2) + 1, textColor, fontSize, Fonts.INTER_MEDIUM);
            if (fileNameLeftIco != null) {
                RenderManager.drawImage(vg, fileNameLeftIco, middle - contentWidth / 2, y + 8, 20, 20);
            }
            if (fileNameRightIco != null) {
                RenderManager.drawImage(vg, fileNameRightIco, middle + contentWidth / 2 - (fileNameLeftIco != null ? 20 : 24), y + 8, 20, 20);
            }
        }
        if(thisAlignment == ALIGNMENT_LEFT) {
            if(fileNameLeftIco != null) {
                RenderManager.drawImage(vg, fileNameLeftIco, x + 12, y + 8, 20, 20);
                RenderManager.drawString(vg, text, x + 40, y + ((float) height / 2) + 1, textColor, fontSize, Fonts.INTER_MEDIUM);
            } else {
                RenderManager.drawString(vg, text, x + 12, y + ((float) height / 2) + 1, textColor, fontSize, Fonts.INTER_MEDIUM);
            }
            if(fileNameRightIco != null) {
                RenderManager.drawImage(vg, fileNameRightIco, x + width - 28, y + 8, 20, 20);
            }
        }
        this.update(x, y);
        if(hoverFx) {
            if(colorPalette == -3) {
                currentColor = OneConfigConfig.TRANSPARENT;
                return;
            }
            if(!toggleable) {
                currentColor = ColorUtils.getColor(currentColor, colorPalette, hovered, clicked);
            } else {
                if (toggled) {
                    currentColor = ColorUtils.smoothColor(currentColor, OneConfigConfig.GRAY_500, OneConfigConfig.BLUE_600, true, 30f);
                } else currentColor = ColorUtils.getColor(currentColor, colorPalette, hovered, clicked);
            }
        }
    }


    @Override
    public void onClick() {
        if(this.page != null) {
            OneConfigGui.INSTANCE.openPage(page);
        }
    }
}
