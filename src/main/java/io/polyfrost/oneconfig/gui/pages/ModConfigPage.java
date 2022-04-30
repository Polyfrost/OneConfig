package io.polyfrost.oneconfig.gui.pages;

import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.config.data.OptionPage;
import io.polyfrost.oneconfig.config.interfaces.BasicOption;
import io.polyfrost.oneconfig.gui.elements.config.ConfigPageButton;
import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;

public class ModConfigPage extends Page {
    private final OptionPage page;

    public ModConfigPage(OptionPage page) {
        super("Mod: " + page.mod.name);
        this.page = page;
    }

    @Override
    public void draw(long vg, int x, int y) {
        if (page.categories.size() == 0) return;
        String selectedCategory = page.categories.keySet().stream().findFirst().get();
        int optionX = x + 30;
        int optionY = y + (page.categories.size() == 1 ? 16 : 64);

        // Top page buttons
        for (ConfigPageButton page : page.categories.get(selectedCategory).topPages) {
            page.draw(vg, optionX, optionY);
            optionY += page.getHeight() + 16;
        }

        // Background
        int backgroundSize = 48;
        for (String subCategory : page.categories.get(selectedCategory).subcategories.keySet()) {
            backgroundSize += 32;
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

        // draw options
        optionY += 16;
        int optionLastY = optionX;
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

        // Bottom page buttons
        for (ConfigPageButton page : page.categories.get(selectedCategory).bottomPages) {
            page.draw(vg, optionX, optionY);
            optionY += page.getHeight() + 16;
        }

        // Draw last options
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

    @Override
    public void finishUpAndClose() {
        page.mod.config.save();
    }

    @Override
    public void keyTyped(char key, int keyCode) {
        if (page.categories.size() == 0) return;
        String selectedCategory = page.categories.keySet().stream().findFirst().get();
        for (String subCategory : page.categories.get(selectedCategory).subcategories.keySet()) {
            for (int i = 0; i < page.categories.get(selectedCategory).subcategories.get(subCategory).size(); i++) {
                page.categories.get(selectedCategory).subcategories.get(subCategory).get(i).keyTyped(key, keyCode);
            }
        }
    }
}
