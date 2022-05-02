package io.polyfrost.oneconfig.gui.pages;

import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;

/**
 * A page is a 1056x728 rectangle of the GUI. It is the main content of the gui, and can be switched back and forwards easily. All the content of OneConfig is in a page.
 */
public abstract class Page {
    protected final String title;

    Page(String title) {
        this.title = title;
    }

    public abstract void draw(long vg, int x, int y);

    public void finishUpAndClose() {
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
}
