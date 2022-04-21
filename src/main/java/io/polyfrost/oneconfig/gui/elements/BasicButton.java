package io.polyfrost.oneconfig.gui.elements;

import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;
import io.polyfrost.oneconfig.utils.ColorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.lwjgl.nanovg.NanoVG.nvgTextBounds;

public class BasicButton extends BasicElement {

    protected String text;
    protected String fileNameLeftIco, fileNameRightIco;

    public BasicButton(int width, int height, @NotNull String text, @Nullable String fileNameLeftIco, @Nullable String fileNameRightIco, int colorPalette, boolean hoverFx) {
        super(width, height, colorPalette, hoverFx);
        this.text = text;
        this.fileNameLeftIco = fileNameLeftIco;
        this.fileNameRightIco = fileNameRightIco;
    }


    @Override
    public void draw(long vg, int x, int y) {
        RenderManager.drawRectangle(vg, x, y, this.width, this.height, this.currentColor);
        final float fontSize;
        if(colorPalette == -1) {
            fontSize = 24f;
        } else fontSize = 14f;
        float width = RenderManager.getTextWidth(vg, text, fontSize);
        int middle = x + this.width / 2;
        RenderManager.drawString(vg, text,middle - width / 2, y + ((float) height / 2),-1, fontSize, Fonts.INTER_MEDIUM);
        if(fileNameLeftIco != null) {
            RenderManager.drawImage(vg, fileNameLeftIco, middle - width - 8, y + 8, 20, 20);
        }
        if(fileNameRightIco != null) {
            RenderManager.drawImage(vg, fileNameRightIco, middle + width - 8, y + 8, 20, 20);
        }
        this.update(x, y);
        if(hoverFx) {
            currentColor = ColorUtils.getColor(currentColor, 1, hovered, clicked);
        }
    }


}
