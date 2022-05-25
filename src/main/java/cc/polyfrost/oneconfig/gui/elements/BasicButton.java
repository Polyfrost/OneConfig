package cc.polyfrost.oneconfig.gui.elements;

import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.gui.pages.Page;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.lwjgl.image.SVGs;
import cc.polyfrost.oneconfig.utils.ColorUtils;


public class BasicButton extends BasicElement {

    protected String text;
    protected SVGs icon1, icon2;
    private final int alignment, colorPalette;
    private final float fontSize, cornerRadius;
    private final int xSpacing, xPadding;
    private final int iconSize;
    public int x, y;
    public static final int ALIGNMENT_LEFT = 0;
    @Deprecated
    public static final int ALIGNMENT_RIGHT = 1;
    public static final int ALIGNMENT_CENTER = 2;
    public static final int ALIGNMENT_JUSTIFIED = 3;

    public static final int SIZE_32 = 32;
    public static final int SIZE_36 = 36;
    public static final int SIZE_40 = 40;
    public static final int SIZE_48 = 48;
    private boolean toggleable = false;
    private Page page;
    private Runnable runnable;

    public BasicButton(int width, int size, String text, int align, int colorPalette) {
        this(width, size, text, null, null, align, colorPalette);
    }

    public BasicButton(int width, int size, String text, SVGs icon1, SVGs icon2, int align, int colorPalette) {
        super(width, 32, colorPalette, true);
        if(text != null) this.text = text;
        if (icon1 != null) this.icon1 = icon1;
        if (icon2 != null) this.icon2 = icon2;
        this.colorPalette = colorPalette;
        this.alignment = align;
        this.cornerRadius = size == SIZE_48 ? 16f : 12f;
        this.xSpacing = size == SIZE_48 ? 12 : 8;
        if(size == SIZE_36 || size == SIZE_40) {
            this.xPadding = 16;
        } else this.xPadding = size == SIZE_48 ? 20 : 12;
        this.height = size;
        this.iconSize = this.height / 2;
        this.fontSize = size == SIZE_48 ? 20 : (float) (size / 2 - 4);
    }

    public BasicButton(int width, int size, SVGs icon, int align, int colorPalette) {
        this(width, size, null, icon, null, align, colorPalette);
    }

    public void setToggleable(boolean state) {
        this.toggleable = state;
    }

    public void setClickAction(Page page) {
        this.page = page;
    }

    public void setClickAction(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void draw(long vg, int x, int y) {
        this.x = x;
        this.y = y;
        RenderManager.drawRoundedRect(vg, x, y, this.width, this.height, currentColor, this.cornerRadius);
        float contentWidth = 0f;
        int textColor = -1;
        final float middle = x + width / 2f;
        final float middleYIcon = y + height / 2f - iconSize / 2f;
        final float middleYText = y + height / 2f + fontSize / 8f;
        if(this.text != null) {
            if (this.colorPalette == -2) {
                textColor = OneConfigConfig.WHITE_80;
                if (hovered) textColor = OneConfigConfig.WHITE;
                if (clicked) textColor = OneConfigConfig.WHITE_80;
                if (page == null) textColor = OneConfigConfig.WHITE_50;
            }
            contentWidth += RenderManager.getTextWidth(vg, text, fontSize, Fonts.MEDIUM);
        }
        if(alignment == ALIGNMENT_CENTER) {
                if (icon1 != null) {
                    contentWidth += iconSize + xSpacing;
                }
                if (icon2 != null) {
                    contentWidth += iconSize + xSpacing;
                }
                if(text != null) {
                    RenderManager.drawString(vg, text, middle - contentWidth / 2 + (icon1 == null ? 0 : iconSize + xSpacing), middleYText, textColor, fontSize, Fonts.MEDIUM);
                }
                if(icon1 != null) {
                    RenderManager.drawSvg(vg, icon1, middle - contentWidth / 2, middleYIcon, iconSize, iconSize);
                }
                if(icon2 != null) {
                    RenderManager.drawSvg(vg, icon2, middle + contentWidth / 2 - iconSize, middleYIcon, iconSize, iconSize);
                }
            this.update(x, y);
            return;
        }
        if(alignment == ALIGNMENT_JUSTIFIED) {
            if(text != null) {
                RenderManager.drawString(vg, text, middle - contentWidth / 2, middleYText, textColor, fontSize, Fonts.MEDIUM);
            }
            if(icon1 != null) {
                RenderManager.drawSvg(vg, icon1, x + xSpacing, middleYIcon, iconSize, iconSize);
            }
            if(icon2 != null) {
                RenderManager.drawSvg(vg, icon2, x + width - xSpacing - iconSize, middleYIcon, iconSize, iconSize);
            }
            this.update(x, y);
            return;
        }
        if(alignment == ALIGNMENT_LEFT) {
            contentWidth = xSpacing;
            if(icon1 != null) {
                RenderManager.drawSvg(vg, icon1, x + contentWidth, middleYIcon, iconSize, iconSize);
                contentWidth += iconSize + xSpacing;
            }
            if(text != null) {
                RenderManager.drawString(vg, text, x + contentWidth, middleYText, textColor, fontSize, Fonts.MEDIUM);
                contentWidth += RenderManager.getTextWidth(vg, text, fontSize, Fonts.MEDIUM) + xSpacing;
            }
            if(icon2 != null) {
                RenderManager.drawSvg(vg, icon2, x + contentWidth, middleYIcon, iconSize, iconSize);
            }
            this.update(x, y);
            return;
        }
        if(alignment == ALIGNMENT_RIGHT) {
            contentWidth = width - xSpacing;
            if(icon2 != null) {
                contentWidth -= iconSize;
                RenderManager.drawSvg(vg, icon2, x + contentWidth, middleYIcon, iconSize, iconSize);
                contentWidth -= xSpacing;
            }
            if(text != null) {
                contentWidth -= RenderManager.getTextWidth(vg, text, fontSize, Fonts.MEDIUM);
                RenderManager.drawString(vg, text, x + contentWidth, middleYText, textColor, fontSize, Fonts.MEDIUM);
                contentWidth -= xSpacing;
            }
            if(icon1 != null) {
                contentWidth -= iconSize;
                RenderManager.drawSvg(vg, icon1, x + contentWidth, middleYIcon, iconSize, iconSize);
            }
            this.update(x, y);
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
        super.update(x, y);
        if (hoverFx) {
            if (colorPalette == -2) {
                currentColor = OneConfigConfig.TRANSPARENT;
                return;
            }
            if (!toggleable) {
                currentColor = ColorUtils.getColor(currentColor, colorPalette, hovered, clicked);
            } else {
                if (toggled) {
                    currentColor = ColorUtils.smoothColor(currentColor, OneConfigConfig.GRAY_500, OneConfigConfig.PRIMARY_600, true, 30f);
                } else currentColor = ColorUtils.getColor(currentColor, colorPalette, hovered, clicked);
            }
        }


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
}
