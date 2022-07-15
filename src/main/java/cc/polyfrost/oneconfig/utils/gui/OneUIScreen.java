package cc.polyfrost.oneconfig.utils.gui;

import cc.polyfrost.oneconfig.gui.GuiPause;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.libs.universal.UScreen;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.utils.InputUtils;
import org.jetbrains.annotations.NotNull;

/**
 * <h1>OneUIScreen</h1>
 * OneUIScreen is a GUI that can be used to render things on the client's screen.
 * It contains many handy methods for rendering, including {@link #draw(long, float)} for drawing using OneConfig's {@link RenderManager}.
 * <p> It also contains methods for mouse input. (see {@link InputUtils} for more utils).
 * <p></p>
 * Use GuiUtils to display a screen; and GuiUtils.closeScreen to close it.
 */
public abstract class OneUIScreen extends UScreen implements GuiPause {
    private boolean mouseDown;
    private boolean blockClicks;

    /**
     * Create a new OneUIScreen.
     *
     * @param restoreGuiOnClose use this to declare weather or not to open the Gui that was open before it when this screen is closed.
     */
    public OneUIScreen(boolean restoreGuiOnClose) {
        super(restoreGuiOnClose);
    }

    /**
     * Create a new OneUIScreen.
     */
    public OneUIScreen() {
        super(false);
    }

    @Override
    public final void onDrawScreen(@NotNull UMatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.onDrawScreen(matrixStack, mouseX, mouseY, partialTicks);
        RenderManager.setupAndDraw(ignoreMinecraftScale(), vg -> draw(vg, partialTicks));
        mouseDown = Platform.getMousePlatform().isButtonDown(0);
    }


    /**
     * Use this method to draw things on the screen. It is called every render tick, and has a handy <code>vg</code> (NanoVG context) that can be used with the {@link RenderManager} to draw things.
     * <p></p>
     * For example: <d> <code>{@link RenderManager#drawRoundedRect(long, float, float, float, float, int, float)} </code>
     *
     * @param vg           the NanoVG context you can use to render things with
     * @param partialTicks the time between ticks (You can use this as a deltaTime equivalent)
     */
    public abstract void draw(long vg, float partialTicks);

    /**
     * Use this method to set whether to use the Minecraft scale on the GUI. Its default is true, and that is recommended for the NanoVG rendering.
     */
    public boolean ignoreMinecraftScale() {
        return true;
    }

    /**
     * Get the current x position of the mouse.
     */
    protected float getMouseX() {
        return InputUtils.mouseX();
    }

    /**
     * Get the current y position of the mouse.
     */
    protected float getMouseY() {
        return InputUtils.mouseY();
    }

    /**
     * Retrieve the click status of the mouse. This method uses a boolean to store the status of the mouse, so it will only return true once per click. (very useful)
     *
     * @param ignoreBlockClicks whether to ignore the current click blocker.
     */
    protected boolean isClicked(boolean ignoreBlockClicks) {
        return mouseDown && !Platform.getMousePlatform().isButtonDown(0) && (!blockClicks || ignoreBlockClicks);
    }

    /**
     * Retrieve the click status of the mouse. This method uses a boolean to store the status of the mouse, so it will only return true once per click. (very useful)
     */
    protected boolean isClicked() {
        return isClicked(false);
    }

    /**
     * Retrieve weather or not the mouse is currently down. Will constantly return true if its clicked. See {@link #isClicked()} for a method that only executes once per tick.
     */
    protected boolean isMouseDown() {
        return Platform.getMousePlatform().isButtonDown(0);
    }

    /**
     * Click blocking can be useful when you are drawing buttons for example over the top of other elements, so a click blocker can be used to ensure that the mouse doesn't click through things.
     */
    public void shouldBlockClicks(boolean state) {
        blockClicks = state;
    }

    /**
     * Click blocking can be useful when you are drawing buttons for example over the top of other elements, so a click blocker can be used to ensure that the mouse doesn't click through things.
     */
    public boolean isBlockingClicks() {
        return blockClicks;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
