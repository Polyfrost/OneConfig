package cc.polyfrost.oneconfig.gui.pages;

import cc.polyfrost.oneconfig.config.elements.OptionPage;
import cc.polyfrost.oneconfig.config.elements.OptionSubcategory;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.gui.animations.DummyAnimation;
import cc.polyfrost.oneconfig.gui.elements.BasicButton;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;

import java.util.ArrayList;

import static cc.polyfrost.oneconfig.gui.elements.BasicButton.SIZE_32;

public class ModConfigPage extends Page {
    private final OptionPage page;
    private final ArrayList<BasicButton> categories = new ArrayList<>();
    private String selectedCategory;
    private int totalSize = 724;
    private final boolean base;

    public ModConfigPage(OptionPage page, boolean base) {
        super(page.name);
        this.page = page;
        this.base = base;
        if (page.categories.size() == 0) return;
        for (String category : page.categories.keySet()) {
            selectedCategory = category;
            break;
        }
        if (page.categories.size() < 2) return;
        for (String category : page.categories.keySet()) {
            BasicButton button = new BasicButton(0, SIZE_32, category, BasicButton.ALIGNMENT_CENTER, ColorPalette.SECONDARY);
            button.setClickAction(() -> switchCategory(category));
            button.setToggleable(true);
            if (category.equals(selectedCategory)) button.setToggled(true);
            categories.add(button);
        }
    }

    public ModConfigPage(OptionPage page) {
        this(page, false);
    }

    @Override
    public void draw(long vg, int x, int y) {
        if (page.categories.size() == 0) return;
        int optionY = y + (page.categories.size() == 1 ? 16 : 64);
        for (OptionSubcategory subCategory : page.categories.get(selectedCategory).subcategories) {
            optionY += subCategory.draw(vg, x + 30, optionY);
        }
        for (OptionSubcategory subCategory : page.categories.get(selectedCategory).subcategories) {
            subCategory.drawLast(vg, x + 30);
        }
        totalSize = optionY - y;
    }

    @Override
    public int drawStatic(long vg, int x, int y) {
        // Category buttons
        if (categories.size() <= 1) return 0;
        int buttonX = x + 16;
        for (BasicButton button : categories) {
            if (button.getWidth() == 0)
                button.setWidth((int) (Math.ceil(RenderManager.getTextWidth(vg, button.getText(), 12f, Fonts.MEDIUM) / 8f) * 8 + 16));
            button.draw(vg, buttonX, y + 16);
            buttonX += button.getWidth() + 16;
        }
        return 60;
    }

    @Override
    public void finishUpAndClose() {
        page.mod.config.save();
    }

    @Override
    public void keyTyped(char key, int keyCode) {
        if (page.categories.size() == 0) return;
        for (OptionSubcategory subCategory : page.categories.get(selectedCategory).subcategories) {
            for (BasicOption option : subCategory.options) {
                option.keyTyped(key, keyCode);
            }
        }
    }

    public void switchCategory(String newCategory) {
        if (!page.categories.containsKey(newCategory)) return;
        selectedCategory = newCategory;
        for (BasicButton button : categories) {
            if (button.getText().equals(newCategory)) continue;
            button.setToggled(false);
            scrollTarget = 0;
            scrollAnimation = null;
        }
    }

    @Override
    public int getMaxScrollHeight() {
        return totalSize;
    }

    @Override
    public boolean isBase() {
        return base;
    }
}
