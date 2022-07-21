//#if FORGE==1
package cc.polyfrost.oneconfig.internal.gui.impl;

import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.GuiModList;
import net.minecraftforge.fml.client.IModGuiFactory;

import java.util.Set;

public class ConfigButtonImpl implements IModGuiFactory {

    @Override
    public void initialize(Minecraft minecraftInstance) {
    }

    //#if MC<=11200
    //$$ @Override
    //$$ public boolean hasConfigGui() {
    //$$     return true;
    //$$ }

    //$$ @Override
    //$$ public GuiScreen createConfigGui(GuiScreen parentScreen) {
    //$$     return OneConfigGui.create();
    //$$ }
    //#else
    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return ConfigGuiWrapper.class;
    }

    @Override
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
        return null;
    }

    public static class ConfigGuiWrapper extends GuiScreen {
        private static boolean hasEverOpened = false;

        public ConfigGuiWrapper(GuiScreen parent) {
        }

        @Override
        public void initGui() {
            OneConfigGui.create();
            OneConfigGui.INSTANCE.sideBar.HUDButton.disable(true);
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            OneConfigGui.INSTANCE.onDrawScreen(UMatrixStack.Compat.INSTANCE.get(), mouseX, mouseY, partialTicks);
        }

        @Override
        public void onGuiClosed() {
            if (!hasEverOpened) hasEverOpened = true;
            OneConfigGui.INSTANCE.onScreenClose();
        }

        @Override
        protected void keyTyped(char typedChar, int keyCode) {
            if (OneConfigGui.INSTANCE.allowClose) {
                // allow ESC pressing to the right menu!
                Minecraft.getMinecraft().displayGuiScreen(new GuiModList(new GuiMainMenu()));
                return;
            }
            OneConfigGui.INSTANCE.onKeyPressed(keyCode, typedChar, null);
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }
    }
    //#endif

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }
}


//#endif