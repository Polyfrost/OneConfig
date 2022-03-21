package io.polyfrost.oneconfig.gui.elements;

import io.polyfrost.oneconfig.renderer.Renderer;
import net.minecraft.util.ResourceLocation;

import java.awt.*;

public class OCStoreBlock extends OCBlock {
    private final ResourceLocation image;
    private final String description;
    private final String title;
    private final Color color;

    public OCStoreBlock(String title, String description, ResourceLocation image, int color) {
        super(color, 300, 400);
        this.color = Renderer.getColorFromInt(color);
        this.description = description;
        this.title = title;
        this.image = image;
    }

    public void draw(int x, int y) {
        //super.draw(x, y);
        Renderer.drawScaledImage(image, x, y, 300, 150);
        //Gui.drawRect(x,y,x + 300, y + 150, -1);
        theme.getBoldFont().drawSplitString(title, x + 2, y + 152, 200, -1);


        //super.update();
    }

    public void draw(int x, int y, int width, int height) {
        draw(x, y);
    }
}
