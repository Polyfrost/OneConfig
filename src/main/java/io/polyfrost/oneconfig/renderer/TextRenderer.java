package io.polyfrost.oneconfig.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;

public class TextRenderer {
    public static FontRenderer fontRenderer = new FontRenderer(Minecraft.getMinecraft().gameSettings, new ResourceLocation("oneconfig:font/ascii"),
            Minecraft.getMinecraft().getTextureManager(), false);
}
