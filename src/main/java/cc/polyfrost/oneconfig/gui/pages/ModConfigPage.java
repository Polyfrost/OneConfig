package cc.polyfrost.oneconfig.gui.pages;

import cc.polyfrost.oneconfig.config.OneConfigConfig;
import cc.polyfrost.oneconfig.config.data.OptionPage;
import cc.polyfrost.oneconfig.config.interfaces.BasicOption;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.elements.BasicButton;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigPageButton;
import cc.polyfrost.oneconfig.lwjgl.RenderManager;
import cc.polyfrost.oneconfig.lwjgl.font.Fonts;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

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
        String filter = OneConfigGui.INSTANCE == null ? "" : OneConfigGui.INSTANCE.getSearchValue().toLowerCase().trim();
        LinkedHashMap<String, ArrayList<BasicOption>> filteredSubcategories = new LinkedHashMap<>(page.categories.get(selectedCategory).subcategories);
        if (!filter.equals("")) {
            filteredSubcategories.clear();
            for (String subCategory : page.categories.get(selectedCategory).subcategories.keySet()) {
                if (subCategory.toLowerCase().contains(filter)) {
                    filteredSubcategories.put(subCategory, page.categories.get(selectedCategory).subcategories.get(subCategory));
                    continue;
                }
                for (BasicOption option : page.categories.get(selectedCategory).subcategories.get(subCategory)) {
                    if (!option.getName().toLowerCase().contains(filter)) continue;
                    if (!filteredSubcategories.containsKey(subCategory))
                        filteredSubcategories.put(subCategory, new ArrayList<>());
                    filteredSubcategories.get(subCategory).add(option);
                }
            }
        }
        int optionX = x + 30;
        int optionY = y + (page.categories.size() == 1 ? 16 : 64);

        // Top page buttons
        for (ConfigPageButton page : page.categories.get(selectedCategory).topPages) {
            if (!page.getName().toLowerCase().contains(filter) && !page.description.toLowerCase().contains(filter))
                continue;
            page.draw(vg, optionX, optionY);
            optionY += page.getHeight() + 16;
        }

        // Background
        if (filteredSubcategories.keySet().size() > 0) {
            int backgroundSize = 16;
            for (String subCategory : filteredSubcategories.keySet()) {
                backgroundSize += 48;
                for (int i = 0; i < filteredSubcategories.get(subCategory).size(); i++) {
                    BasicOption option = filteredSubcategories.get(subCategory).get(i);
                    if (i + 1 < filteredSubcategories.get(subCategory).size()) {
                        BasicOption nextOption = filteredSubcategories.get(subCategory).get(i + 1);
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
        if (filteredSubcategories.keySet().size() > 0) {
            optionY += 16;
            for (String subCategory : filteredSubcategories.keySet()) {
                RenderManager.drawString(vg, subCategory, optionX, optionY + 16, OneConfigConfig.WHITE_90, 24f, Fonts.MEDIUM);
                optionY += 48;
                for (int i = 0; i < filteredSubcategories.get(subCategory).size(); i++) {
                    BasicOption option = filteredSubcategories.get(subCategory).get(i);
                    option.draw(vg, optionX, optionY);
                    if (i + 1 < filteredSubcategories.get(subCategory).size()) {
                        BasicOption nextOption = filteredSubcategories.get(subCategory).get(i + 1);
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
            if (!page.getName().toLowerCase().contains(filter) && !page.description.toLowerCase().contains(filter))
                continue;
            page.draw(vg, optionX, optionY);
            optionY += page.getHeight() + 16;
        }
        totalSize = optionY - y;

        // Draw last options
        if (filteredSubcategories.keySet().size() > 0) {
            for (String subCategory : filteredSubcategories.keySet()) {
                optionLastY += 48;
                for (int i = 0; i < filteredSubcategories.get(subCategory).size(); i++) {
                    BasicOption option = filteredSubcategories.get(subCategory).get(i);
                    option.drawLast(vg, optionX, optionLastY);
                    if (i + 1 < filteredSubcategories.get(subCategory).size()) {
                        BasicOption nextOption = filteredSubcategories.get(subCategory).get(i + 1);
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

    @Override
    public int getMaxScrollHeight() {
        return totalSize;
    }
}
