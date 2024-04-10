package org.polyfrost.oneconfig.ui.screen;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.libs.universal.UResolution;
import org.polyfrost.oneconfig.libs.universal.UScreen;
import org.polyfrost.polyui.PolyUI;
import org.polyfrost.polyui.renderer.Window;
import org.polyfrost.polyui.renderer.data.Cursor;

//#if MC>=11300
//$$ import static org.lwjgl.glfw.GLFW.*;
//#endif

@ApiStatus.Internal
public class MCWindow extends Window {
    private final Minecraft mc;
    //#if MC>=11300
    //$$ private final long handle;
    //#endif

    public MCWindow(Minecraft minecraft) {
        super(UResolution.getViewportWidth(), UResolution.getViewportHeight(), 1f);
        this.mc = minecraft;
        //#if MC>=11300
        //$$ this.handle = mc
                //#if MC>=11700
                //$$ .getWindow()
                //#else
        //$$         .getMainWindow()
                //#endif
        //$$         .getHandle();
        //#endif
    }

    @Override
    public void close() {
        UScreen.displayScreen(null);
    }

    @NotNull
    @Override
    public Window open(@NotNull PolyUI polyUI) {
        throw new UnsupportedOperationException("Cannot be opened this way, see PolyUIScreen");
    }

    @Override
    public boolean supportsRenderPausing() {
        return false;
    }

    @Nullable
    @Override
    public String getClipboard() {
        //#if MC>=11300
        //$$ return glfwGetClipboardString(handle);
        //#else
        try {
            return java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().getData(java.awt.datatransfer.DataFlavor.stringFlavor).toString();
        } catch (Exception ignored) {
            return null;
        }
        //#endif
    }

    @Override
    public void setClipboard(@Nullable String s) {
        //#if MC>=11300
        //$$ glfwSetClipboardString(handle, s);
        //#else
        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(s), null);
        //#endif
    }

    @Override
    public void setCursor(@NotNull Cursor cursor) {
        //#if MC>=11300
        //$$ switch (cursor) {
        //$$     case Pointer:
        //$$         glfwSetCursor(handle, glfwCreateStandardCursor(GLFW_ARROW_CURSOR));
        //$$         return;
        //$$     case Clicker:
        //$$         glfwSetCursor(handle, glfwCreateStandardCursor(GLFW_HAND_CURSOR));
        //$$         return;
        //$$     case Text:
        //$$         glfwSetCursor(handle, glfwCreateStandardCursor(GLFW_IBEAM_CURSOR));
        //$$ }
        //#endif
    }

    @NotNull
    @Override
    public String getKeyName(int i) {
        String k =
                //#if MC>=11300
                //$$ glfwGetKeyName(i, 0);
                //#else
                org.lwjgl.input.Keyboard.getKeyName(i);
        //#endif
        return k == null ? "Unknown" : k;
    }
}
