package cc.polyfrost.oneconfig.gui.pages;

import cc.polyfrost.oneconfig.gui.animations.Animation;
import cc.polyfrost.oneconfig.gui.animations.ColorAnimation;
import cc.polyfrost.oneconfig.gui.animations.EaseOutQuad;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.scissor.Scissor;
import cc.polyfrost.oneconfig.renderer.scissor.ScissorManager;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;
import org.lwjgl.input.Mouse;

/**
 * A page is a 1056x728 rectangle of the GUI. It is the main content of the gui, and can be switched back and forwards easily. All the content of OneConfig is in a page.
 */
public abstract class Page {
    protected final String title;
    private Animation scrollAnimation;
    private final ColorAnimation colorAnimation = new ColorAnimation(new ColorPalette(Colors.TRANSPARENT, Colors.GRAY_400_60, Colors.GRAY_400_60));
    private float scrollTarget;
    private long scrollTime;
    private boolean mouseWasDown, dragging;
    private float yStart;

    public Page(String title) {
        colorAnimation.setSpeed(200);
        this.title = title;
    }

    public abstract void draw(long vg, int x, int y);

    /**
     * Use this method to draw elements that are static on the page (ignore the scrolling).
     *
     * @return the total height of the elements, so they are excluded from the scissor rectangle.
     */
    public int drawStatic(long vg, int x, int y) {
        return 0;
    }

    public void finishUpAndClose() {
    }

    public void scrollWithDraw(long vg, int x, int y) {
        int maxScroll = getMaxScrollHeight();
        int scissorOffset = drawStatic(vg, x, y);
        float scroll = scrollAnimation == null ? scrollTarget : scrollAnimation.get();
        final float scrollBarLength = (728f / maxScroll) * 728f;
        Scissor scissor = ScissorManager.scissor(vg, x, y + scissorOffset, x + 1056, y + 728 - scissorOffset);
        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            scrollTarget += dWheel;

            if (scrollTarget > 0f) scrollTarget = 0f;
            else if (scrollTarget < -maxScroll + 728) scrollTarget = -maxScroll + 728;

            scrollAnimation = new EaseOutQuad(150, scroll, scrollTarget, false);
            scrollTime = System.currentTimeMillis();
        } else if (scrollAnimation != null && scrollAnimation.isFinished()) scrollAnimation = null;
        if (maxScroll <= 728) {
            draw(vg, x, y);
            ScissorManager.resetScissor(vg, scissor);
            return;
        }
        draw(vg, x, (int) (y + scroll));
        if (dragging && InputUtils.isClicked(true)) {
            dragging = false;
        }

        ScissorManager.resetScissor(vg, scissor);
        if(!(scrollBarLength > 727f)) {
            final float scrollBarY = (scroll / maxScroll) * 720f;
            final boolean isMouseDown = Mouse.isButtonDown(0);
            final boolean scrollHover = InputUtils.isAreaHovered(x + 1042, (int) (y - scrollBarY), 12, (int) scrollBarLength) || (System.currentTimeMillis() - scrollTime < 1000);
            final boolean hovered = scrollHover && Mouse.isButtonDown(0);
            if (hovered && isMouseDown && !mouseWasDown) {
                yStart = InputUtils.mouseY();
                dragging = true;
            }
            mouseWasDown = isMouseDown;
            if(dragging) {
                scrollTarget = -(InputUtils.mouseY() - yStart) * maxScroll / 728f;
                if (scrollTarget > 0f) scrollTarget = 0f;
                else if (scrollTarget < -maxScroll + 728) scrollTarget = -maxScroll + 728;
                scrollAnimation = new EaseOutQuad(150, scroll, scrollTarget, false);
            }
            RenderManager.drawRoundedRect(vg, x + 1044, y - scrollBarY, 8, scrollBarLength, colorAnimation.getColor(scrollHover, dragging), 4f);
        }
    }

    public String getTitle() {
        return title;
    }

    public void keyTyped(char key, int keyCode) {
    }

    /**
     * Overwrite this method and make it return true if you want this to always be the base in breadcrumbs
     */
    public boolean isBase() {
        return false;
    }

    /**
     * Use this method to set the maximum scroll height of the page.
     */
    public int getMaxScrollHeight() {
        return 728;
    }
}
