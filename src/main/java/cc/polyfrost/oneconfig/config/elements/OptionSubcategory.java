package cc.polyfrost.oneconfig.config.elements;

import cc.polyfrost.oneconfig.gui.Colors;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigPageButton;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.font.Fonts;

import java.util.ArrayList;

public class OptionSubcategory {
    private final String name;
    public ArrayList<BasicOption> options = new ArrayList<>();
    public ArrayList<ConfigPageButton> topButtons = new ArrayList<>();
    public ArrayList<ConfigPageButton> bottomButtons = new ArrayList<>();
    private ArrayList<BasicOption> filteredOptions = new ArrayList<>();
    private int drawLastY;

    public OptionSubcategory(String name) {
        this.name = name;
    }

    public int draw(long vg, int x, int y) {
        String filter = OneConfigGui.INSTANCE == null ? "" : OneConfigGui.INSTANCE.getSearchValue().toLowerCase().trim();
        filteredOptions = new ArrayList<>(options);
        ArrayList<ConfigPageButton> filteredTop = new ArrayList<>(topButtons);
        ArrayList<ConfigPageButton> filteredBottom = new ArrayList<>(bottomButtons);
        if (!filter.equals("") && !name.toLowerCase().contains(filter)) {
            filteredOptions.clear();
            filteredTop.clear();
            filteredBottom.clear();
            for (BasicOption option : options) {
                if (option.getName().toLowerCase().contains(filter)) filteredOptions.add(option);
            }
            for (ConfigPageButton page : topButtons) {
                if (page.getName().toLowerCase().contains(filter) || page.description.toLowerCase().contains(filter))
                    filteredTop.add(page);
            }
            for (ConfigPageButton page : bottomButtons) {
                if (page.getName().toLowerCase().contains(filter) || page.description.toLowerCase().contains(filter))
                    filteredBottom.add(page);
            }
        }
        if (filteredOptions.size() == 0 && filteredTop.size() == 0 && filteredBottom.size() == 0) return 0;
        int optionY = y;
        if (!name.equals("")) {
            RenderManager.drawText(vg, name, x, y + 12, Colors.WHITE_90, 24, Fonts.MEDIUM);
            optionY += 36;
        }

        for (ConfigPageButton page : filteredTop) {
            page.draw(vg, x, optionY);
            optionY += page.getHeight() + 16;
        }

        if (filteredOptions.size() > 0) {
            int backgroundSize = 16;
            for (int i = 0; i < filteredOptions.size(); i++) {
                BasicOption option = filteredOptions.get(i);
                if (i + 1 < filteredOptions.size()) {
                    BasicOption nextOption = filteredOptions.get(i + 1);
                    if (option.size == 1 && option.hasHalfSize() && nextOption.size == 1 && nextOption.hasHalfSize()) {
                        backgroundSize += Math.max(option.getHeight(), nextOption.getHeight()) + 16;
                        i++;
                        continue;
                    }
                }
                backgroundSize += option.getHeight() + 16;
            }
            RenderManager.drawRoundedRect(vg, x - 16, optionY, 1024, backgroundSize, Colors.GRAY_900, 20);
            optionY += 16;
        }

        drawLastY = optionY;
        if (filteredOptions.size() > 0) {
            for (int i = 0; i < filteredOptions.size(); i++) {
                BasicOption option = filteredOptions.get(i);
                option.draw(vg, x, optionY);
                if (i + 1 < filteredOptions.size()) {
                    BasicOption nextOption = filteredOptions.get(i + 1);
                    if (option.size == 1 && option.hasHalfSize() && nextOption.size == 1 && nextOption.hasHalfSize()) {
                        nextOption.draw(vg, x + 512, optionY);
                        optionY += Math.max(option.getHeight(), nextOption.getHeight()) + 16;
                        i++;
                        continue;
                    }
                }
                optionY += option.getHeight() + 16;
            }
            optionY += 16;
        }

        for (ConfigPageButton page : filteredBottom) {
            page.draw(vg, x, optionY);
            optionY += page.getHeight() + 16;
        }

        return optionY - y;
    }

    public void drawLast(long vg, int x) {
        for (int i = 0; i < filteredOptions.size(); i++) {
            BasicOption option = filteredOptions.get(i);
            option.drawLast(vg, x, drawLastY);
            if (i + 1 < filteredOptions.size()) {
                BasicOption nextOption = filteredOptions.get(i + 1);
                if (option.size == 1 && option.hasHalfSize() && nextOption.size == 1 && nextOption.hasHalfSize()) {
                    nextOption.drawLast(vg, x + 512, drawLastY);
                    drawLastY += Math.max(option.getHeight(), nextOption.getHeight()) + 16;
                    i++;
                    continue;
                }
            }
            drawLastY += option.getHeight() + 16;
        }
    }

    public String getName() {
        return name;
    }
}
