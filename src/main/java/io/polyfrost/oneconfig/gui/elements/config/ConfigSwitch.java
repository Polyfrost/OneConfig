package io.polyfrost.oneconfig.gui.elements.config;

import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.config.interfaces.BasicOption;
import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;
import io.polyfrost.oneconfig.utils.ColorUtils;
import io.polyfrost.oneconfig.utils.InputUtils;
import io.polyfrost.oneconfig.utils.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;

import static org.lwjgl.nanovg.NanoVG.nvgResetScissor;
import static org.lwjgl.nanovg.NanoVG.nvgScissor;

public class ConfigSwitch extends BasicOption {
    private int color;
    private float percentOn = 0f;
    private boolean clicked = false;
    private boolean toggled ;

    public ConfigSwitch(Field field, String name, int size) {
        super(field, name,  size);
        try {
            toggled = (boolean) get();
        } catch (IllegalAccessException e) {
            System.err.println("failed to get config value: class=" + this + " fieldWatching=" + field);
        }
    }

    @Override
    public void draw(long vg, int x, int y) {
        nvgScissor(vg, x, y, size == 0 ? 480 : 992, 32);
        boolean hovered = InputUtils.isAreaHovered(x, y, size == 0 ? 480 : 992, 32);
        int x2 = x + 19 + (int) (percentOn * 18);
        color = ColorUtils.smoothColor(color, OneConfigConfig.GRAY_400, OneConfigConfig.BLUE_500, toggled, 20f);
        if(color == -15123643) {
            color = OneConfigConfig.GRAY_400;
        }
        RenderManager.drawRoundedRect(vg, x + 16, y + 4, 42, 24, color, 12f);
        RenderManager.drawRoundedRect(vg, x2, y + 7, 18, 18, OneConfigConfig.WHITE, 9f);
        RenderManager.drawString(vg, name, x + 66, y + 17, OneConfigConfig.WHITE, 18f, Fonts.INTER_MEDIUM);

        if (InputUtils.isClicked(x, y, size == 0 ? 480 : 992, 32) && !this.clicked && hovered)
        {
            toggled = !toggled;
            try {
                set(toggled);
            } catch (IllegalAccessException e) {
                System.err.println("failed to write config value: class=" + this + " fieldWatching=" + field + " valueWrite=" + toggled);
                e.printStackTrace();
            }
        }
        this.clicked = InputUtils.isClicked(x, y, size == 0 ? 480 : 992, 32) && hovered;
        percentOn = MathUtils.clamp(MathUtils.easeOut(percentOn, toggled ? 1f : 0f, 10));
        nvgResetScissor(vg);



    }

    @Override
    public int getHeight() {
        return 32;
    }
}
