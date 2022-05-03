package cc.polyfrost.oneconfig.gui.pages;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.config.data.OptionPage;
import cc.polyfrost.oneconfig.config.interfaces.BasicOption;
import cc.polyfrost.oneconfig.gui.elements.BasicButton;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigPageButton;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;

import java.util.ArrayList;

public class ModConfigPage extends Page {
    private final OptionPage page;
    private final ArrayList<BasicButton> categories = new ArrayList<>();
    private String selectedCategory;

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
        int optionX = x + 30;
        int optionY = y + (page.categories.size() == 1 ? 16 : 64);

        // Category buttons
        int buttonX = x + 16;
        for (BasicButton button : categories) {
            if (button.getWidth() == 0)
                button.setWidth((int) (Math.ceil(RenderManager.getTextWidth(vg, button.getText(), 14f, Fonts.INTER_MEDIUM) / 8f) * 8 + 16));
            button.draw(vg, buttonX, y + 16);
            buttonX += button.getWidth() + 16;
        }

        // Top page buttons
        for (ConfigPageButton page : page.categories.get(selectedCategory).topPages) {
            page.draw(vg, optionX, optionY);
            optionY += page.getHeight() + 16;
        }

        // Background
        if (page.categories.get(selectedCategory).subcategories.keySet().size() > 0) {
            int backgroundSize = 16;
            for (String subCategory : page.categories.get(selectedCategory).subcategories.keySet()) {
                backgroundSize += 48;
                for (int i = 0; i < page.categories.get(selectedCategory).subcategories.get(subCategory).size(); i++) {
                    BasicOption option = page.categories.get(selectedCategory).subcategories.get(subCategory).get(i);
                    if (i + 1 < page.categories.get(selectedCategory).subcategories.get(subCategory).size()) {
                        BasicOption nextOption = page.categories.get(selectedCategory).subcategories.get(subCategory).get(i + 1);
                        if (option.size == 1 && option.hasHalfSize() && nextOption.size == 1 && nextOption.hasHalfSize()) {
                            backgroundSize += Math.max(option.getHeight(), nextOption.getHeight()) + 16;
                            i++;
                            continue;
                        }
                    }
                    backgroundSize += option.getHeight() + 16;
                }
            }
            RenderManager.drawRoundedRect(vg, x + 14, optionY, 1024, backgroundSize, OneConfigConfig.GRAY_900, 20);
        }

        // draw options
        int optionLastY = optionY + 16;
        if (page.categories.get(selectedCategory).subcategories.keySet().size() > 0) {
            optionY += 16;
            for (String subCategory : page.categories.get(selectedCategory).subcategories.keySet()) {
                RenderManager.drawString(vg, subCategory, optionX, optionY + 16, OneConfigConfig.WHITE_90, 24f, Fonts.INTER_MEDIUM);
                optionY += 48;
                for (int i = 0; i < page.categories.get(selectedCategory).subcategories.get(subCategory).size(); i++) {
                    BasicOption option = page.categories.get(selectedCategory).subcategories.get(subCategory).get(i);
                    option.draw(vg, optionX, optionY);
                    if (i + 1 < page.categories.get(selectedCategory).subcategories.get(subCategory).size()) {
                        BasicOption nextOption = page.categories.get(selectedCategory).subcategories.get(subCategory).get(i + 1);
                        if (option.size == 1 && option.hasHalfSize() && nextOption.size == 1 && nextOption.hasHalfSize()) {
                            nextOption.draw(vg, optionX + 512, optionY);
                            optionY += Math.max(option.getHeight(), nextOption.getHeight()) + 16;
                            i++;
                            continue;
                        }
                    }
                    optionY += option.getHeight() + 16;
                }
            }
            optionY += 16;
        }

        // Bottom page buttons
        for (ConfigPageButton page : page.categories.get(selectedCategory).bottomPages) {
            page.draw(vg, optionX, optionY);
            optionY += page.getHeight() + 16;
        }

        // Draw last options
        if (page.categories.get(selectedCategory).subcategories.keySet().size() > 0) {
            for (String subCategory : page.categories.get(selectedCategory).subcategories.keySet()) {
                optionLastY += 48;
                for (int i = 0; i < page.categories.get(selectedCategory).subcategories.get(subCategory).size(); i++) {
                    BasicOption option = page.categories.get(selectedCategory).subcategories.get(subCategory).get(i);
                    option.drawLast(vg, optionX, optionLastY);
                    if (i + 1 < page.categories.get(selectedCategory).subcategories.get(subCategory).size()) {
                        BasicOption nextOption = page.categories.get(selectedCategory).subcategories.get(subCategory).get(i + 1);
                        if (option.size == 1 && option.hasHalfSize() && nextOption.size == 1 && nextOption.hasHalfSize()) {
                            nextOption.drawLast(vg, optionX + 512, optionLastY);
                            optionLastY += Math.max(option.getHeight(), nextOption.getHeight()) + 16;
                            i++;
                            continue;
                        }
                    }
                    optionLastY += option.getHeight() + 16;
                }
            }
        }
    }

    @Override
    public void finishUpAndClose() {
        page.mod.config.save();
    }

    @Override
    public void keyTyped(char key, int keyCode) {
        if (page.categories.size() == 0) return;
        for (String subCategory : page.categories.get(selectedCategory).subcategories.keySet()) {
            for (int i = 0; i < page.categories.get(selectedCategory).subcategories.get(subCategory).size(); i++) {
                page.categories.get(selectedCategory).subcategories.get(subCategory).get(i).keyTyped(key, keyCode);
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
}
