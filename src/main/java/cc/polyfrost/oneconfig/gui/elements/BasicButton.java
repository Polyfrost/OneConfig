package cc.polyfrost.oneconfig.gui.elements;

import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.pages.Page;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;
import cc.polyfrost.oneconfig.utils.color.ColorUtils;
import org.jetbrains.annotations.NotNull;

public class BasicButton extends BasicElement {

    protected String text;
    protected SVGs icon1, icon2;
    private final int alignment;
    private final float fontSize, cornerRadius;
    private final float xSpacing, xPadding;
    private final int iconSize;
    public float x, y;
    public static final int ALIGNMENT_LEFT = 0;
    public static final int ALIGNMENT_CENTER = 2;
    public static final int ALIGNMENT_JUSTIFIED = 3;

    public static final int SIZE_32 = 32;
    public static final int SIZE_36 = 36;
    public static final int SIZE_40 = 40;
    public static final int SIZE_48 = 48;
    private boolean toggleable = false;
    private Page page;
    private Runnable runnable;

    public BasicButton(int width, int size, String text, SVGs icon1, SVGs icon2, int align, @NotNull ColorPalette colorPalette) {
        super(width, 32, colorPalette, true);
        if (text != null) this.text = text;
        if (icon1 != null) this.icon1 = icon1;
        if (icon2 != null) this.icon2 = icon2;
        this.colorPalette = colorPalette;
        this.alignment = align;
        this.cornerRadius = size == SIZE_48 ? 16f : 12f;
        this.xSpacing = size == SIZE_48 ? 12 : 8;
        if (size == SIZE_36 || size == SIZE_40) {
            this.xPadding = 16;
        } else this.xPadding = size == SIZE_48 ? 20 : 12;
        this.height = size;
        this.iconSize = this.height / 2;
        this.fontSize = size == SIZE_48 ? 20 : (float) (size / 2 - 4);
    }

    public BasicButton(int width, int size, SVGs icon, int align, @NotNull ColorPalette colorPalette) {
        this(width, size, null, icon, null, align, colorPalette);
    }

    public BasicButton(int width, int size, String text, int align, @NotNull ColorPalette colorPalette) {
        this(width, size, text, null, null, align, colorPalette);
    }

    @Override
    public void draw(long vg, float x, float y) {
        this.x = x;
        this.y = y;
        this.update(x, y);
        if (disabled) RenderManager.setAlpha(vg, 0.5f);
        float contentWidth = 0f;
        int color;
        if (colorPalette == ColorPalette.TERTIARY || colorPalette == ColorPalette.TERTIARY_DESTRUCTIVE) {
            color = currentColor;
        } else {
            RenderManager.drawRoundedRect(vg, x, y, this.width, this.height, currentColor, this.cornerRadius);
            color = ColorUtils.setAlpha(Colors.WHITE, (int) (colorAnimation.getAlpha() * 255));
        }
        final float middle = x + width / 2f;
        final float middleYIcon = y + height / 2f - iconSize / 2f;
        final float middleYText = y + height / 2f + fontSize / 8f;
        if (this.text != null) {
            contentWidth += RenderManager.getTextWidth(vg, text, fontSize, Fonts.MEDIUM);
        }
        if (alignment == ALIGNMENT_CENTER) {
            if (icon1 != null && icon2 == null && text == null) {
                RenderManager.drawSvg(vg, icon1, middle - iconSize / 2f, middleYIcon, iconSize, iconSize, color);
            } else {
                if (icon1 != null)
                    contentWidth += iconSize + xSpacing;
                if (icon2 != null)
                    contentWidth += iconSize + xSpacing;
                if (text != null)
                    RenderManager.drawText(vg, text, middle - contentWidth / 2 + (icon1 == null ? 0 : iconSize + xSpacing), middleYText, color, fontSize, Fonts.MEDIUM);
                if (icon1 != null)
                    RenderManager.drawSvg(vg, icon1, middle - contentWidth / 2, middleYIcon, iconSize, iconSize, color);
                if (icon2 != null)
                    RenderManager.drawSvg(vg, icon2, middle + contentWidth / 2 - iconSize, middleYIcon, iconSize, iconSize, color);
            }
        } else if (alignment == ALIGNMENT_JUSTIFIED) {
            if (text != null)
                RenderManager.drawText(vg, text, middle - contentWidth / 2, middleYText, color, fontSize, Fonts.MEDIUM);
            if (icon1 != null)
                RenderManager.drawSvg(vg, icon1, x + xPadding, middleYIcon, iconSize, iconSize, color);
            if (icon2 != null)
                RenderManager.drawSvg(vg, icon2, x + width - xPadding - iconSize, middleYIcon, iconSize, iconSize, color);
        } else if (alignment == ALIGNMENT_LEFT) {
            contentWidth = xPadding;
            if (icon1 != null) {
                RenderManager.drawSvg(vg, icon1, x + contentWidth, middleYIcon, iconSize, iconSize, color);
                contentWidth += iconSize + xSpacing;
            }
            if (text != null) {
                RenderManager.drawText(vg, text, x + contentWidth, middleYText, color, fontSize, Fonts.MEDIUM);
            }
            if (icon2 != null)
                RenderManager.drawSvg(vg, icon2, x + width - xPadding - iconSize, middleYIcon, iconSize, iconSize, color);
        }
        if (disabled) RenderManager.setAlpha(vg, 1f);
    }

    @Override
    public void onClick() {
        if (this.page != null && OneConfigGui.INSTANCE != null) {
            OneConfigGui.INSTANCE.openPage(page);
        } else if (this.runnable != null) {
            runnable.run();
        }
        if (toggleable && toggled) setColorPalette(ColorPalette.PRIMARY);
        else if (toggleable) setColorPalette(ColorPalette.SECONDARY);
    }

    @Override
    public void setToggled(boolean toggled) {
        this.toggled = toggled;
        if (toggled && toggleable) setColorPalette(ColorPalette.PRIMARY);
        else if (toggleable) setColorPalette(ColorPalette.SECONDARY);
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

    public Page getPage() {
        return page;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setLeftIcon(SVGs icon) {
        icon1 = icon;
    }

    public void setRightIcon(SVGs icon) {
        icon2 = icon;
    }

    public boolean hasClickAction() {
        return page != null || runnable != null;
    }
}
