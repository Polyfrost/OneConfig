package cc.polyfrost.oneconfig.gui.pages;

import cc.polyfrost.oneconfig.config.data.OptionPage;
import cc.polyfrost.oneconfig.config.data.OptionSubcategory;
import cc.polyfrost.oneconfig.config.interfaces.BasicOption;
import cc.polyfrost.oneconfig.gui.elements.BasicButton;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;

import java.util.ArrayList;

public class ModConfigPage extends Page {
    private final OptionPage page;
    private final ArrayList<BasicButton> categories = new ArrayList<>();
    private String selectedCategory;
    private int totalSize = 724;

    public ModConfigPage(OptionPage page) {
        super(page.name);
        this.page = page;
        if (page.categories.size() == 0) return;
        for (String category : page.categories.keySet()) {
            selectedCategory = category;
            break;
        }
        if (page.categories.size() < 2) return;
        for (String category : page.categories.keySet()) {
            BasicButton button = new BasicButton(0, 32, category, null, null, 0, BasicButton.ALIGNMENT_CENTER, true, () -> switchCategory(category));
            if (category.equals(selectedCategory)) button.setToggled(true);
            categories.add(button);
        }
    }

    @Override
    public void draw(long vg, int x, int y) {
        if (page.categories.size() == 0) return;
        int optionY = y + (page.categories.size() == 1 ? 16 : 64);
        for (OptionSubcategory subCategory : page.categories.get(selectedCategory).subcategories) {
            optionY += subCategory.draw(vg, x + 30, optionY);
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
        }
    }

    @Override
    public int getMaxScrollHeight() {
        return totalSize;
    }
}
