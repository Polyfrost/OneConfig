package cc.polyfrost.oneconfig.gui.pages;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.utils.MathUtils;
import org.lwjgl.input.Mouse;

/**
 * A page is a 1056x728 rectangle of the GUI. It is the main content of the gui, and can be switched back and forwards easily. All the content of OneConfig is in a page.
 */
public abstract class Page {
    private float scrollPercent = 0f;
    private float yDiff, scrollAmount;

    protected final String title;

    Page(String title) {
        this.title = title;
    }

    public abstract void draw(long vg, int x, int y);

    public void finishUpAndClose() {
    }

    public void scrollWithDraw(long vg, int x, int y) {
        int dWheel = Mouse.getDWheel();
        scrollAmount += dWheel / 120f;
        scrollPercent = MathUtils.easeOut(scrollPercent, scrollAmount, 20f);


        int currentScroll = (int) yDiff + (int) (scrollPercent * 100);
        if(currentScroll > 0) {
            currentScroll = 0;
            scrollPercent = 0;
            scrollAmount = 0;
        }
        draw(vg, x, y + currentScroll);
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
