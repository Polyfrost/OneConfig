package cc.polyfrost.oneconfig.config.elements;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigPageButton;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.font.Fonts;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OptionSubcategory {
    private final String name;
    public ArrayList<BasicOption> options = new ArrayList<>();
    public ArrayList<ConfigPageButton> topButtons = new ArrayList<>();
    public ArrayList<ConfigPageButton> bottomButtons = new ArrayList<>();
    private List<BasicOption> filteredOptions = new ArrayList<>();
    private int drawLastY;

    public OptionSubcategory(String name) {
        this.name = name;
    }

    public int draw(long vg, int x, int y) {
        String filter = OneConfigGui.INSTANCE == null ? "" : OneConfigGui.INSTANCE.getSearchValue().toLowerCase().trim();
        filteredOptions = options.stream().filter(option -> !option.isHidden() && (filter.equals("") || name.toLowerCase().contains(filter) || option.name.toLowerCase().contains(filter))).collect(Collectors.toList());
        List<ConfigPageButton> filteredTop = topButtons.stream().filter(page -> !page.isHidden() && (filter.equals("") || name.toLowerCase().contains(filter) || page.name.toLowerCase().contains(filter) || page.description.toLowerCase().contains(filter))).collect(Collectors.toList());
        List<ConfigPageButton> filteredBottom = bottomButtons.stream().filter(page -> !page.isHidden() && (filter.equals("") || name.toLowerCase().contains(filter) || page.name.toLowerCase().contains(filter) || page.description.toLowerCase().contains(filter))).collect(Collectors.toList());
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
                    if (option.size == 1 && nextOption.size == 1) {
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
                    if (option.size == 1 && nextOption.size == 1) {
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
                if (option.size == 1 && nextOption.size == 1) {
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

    public void reset(Config config) {
        for (BasicOption option : options) {
            options.remove(config);
        }
    }
}
