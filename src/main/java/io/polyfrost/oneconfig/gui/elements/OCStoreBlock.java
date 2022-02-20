package io.polyfrost.oneconfig.gui.elements;

import io.polyfrost.oneconfig.renderer.Renderer;
import net.minecraft.util.ResourceLocation;

import java.awt.*;

public class OCStoreBlock extends OCBlock {
    private ResourceLocation image;
    private String description, title;
    private Color color;

    public OCStoreBlock(String title, String description, ResourceLocation image, int color) {
        super(color, 200, 400);
        this.color = Renderer.getColorFromInt(color);
        this.description = description;
        this.title = title;
        this.image = image;
    }

    public void draw(int x, int y) {
        super.draw(x, y);
        Renderer.drawScaledImage(image, x, y, 200, 100);
        super.theme.getFont().drawSplitString("i like fish", x + 2, y + 102, 200, -1);


        super.update();
    }

    public void draw(int x, int y, int width, int height) {
        draw(x,y);
    }
}
