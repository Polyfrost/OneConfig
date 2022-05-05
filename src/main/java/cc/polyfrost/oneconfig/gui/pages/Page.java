package cc.polyfrost.oneconfig.gui.pages;

import cc.polyfrost.oneconfig.utils.MathUtils;
import org.lwjgl.input.Mouse;

/**
 * A page is a 1056x728 rectangle of the GUI. It is the main content of the gui, and can be switched back and forwards easily. All the content of OneConfig is in a page.
 */
public abstract class Page {
    private float currentScrollf = 0f;
    private float scrollTarget;

    protected final String title;

    Page(String title) {
        this.title = title;
    }

    public abstract void draw(long vg, int x, int y);

    /** Use this method to draw elements that are static on the page (ignore the scrolling).
     * @return the total height of the elements, so they are excluded from the scissor rectangle. */
    public int drawStatic(long vg, int x, int y) {
        return 0;
    }

    public void finishUpAndClose() {
    }

    public void scrollWithDraw(long vg, int x, int y) {
        int currentScroll = (int) (currentScrollf * 100);
        draw(vg, x, y + currentScroll);
        int dWheel = Mouse.getDWheel();
        if(dWheel > 120) dWheel = 120;
        if(!(Math.abs((scrollTarget * 100) - 728) >= getMaxScrollHeight() && dWheel < 0)) {
            scrollTarget += dWheel / 120f;
        }
        if(scrollTarget > 0f) {     // fyi this is anti overscroll protection
            scrollTarget = 0;
        }

        currentScrollf = MathUtils.easeOut(currentScrollf, scrollTarget, 20f);


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

    /** Use this method to set the maximum scroll height of the page. */
    public int getMaxScrollHeight() {
        return 728;
    }
}
