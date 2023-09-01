package org.polyfrost.oneconfig.api.config.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.config.Tree;
import org.polyfrost.polyui.input.Translator;
import org.polyfrost.polyui.renderer.data.PolyImage;

import java.util.ArrayList;

import static org.polyfrost.oneconfig.api.config.Tree.LOGGER;

public class Mod {
	public final PolyImage icon;
	public final Translator.Text name;
	public boolean enabled = true;
	public boolean favorite = false;
	public boolean hasUpdate = true;
	public ArrayList<PolyImage> data = new ArrayList<>(3);
	public final Category category;
	private Tree config;


	public Mod(@Nullable PolyImage icon, @NotNull Translator.Text name, Category category, Tree config) {
		this.icon = icon;
		this.name = name;
		this.category = category;
		this.config = config;
	}

    public Tree getConfig() {
        if(config == null) throw new IllegalStateException("Config not loaded!");
        return config;
    }

    public void setConfig(Tree c) {
        if(config != null) {
            LOGGER.warn("Config already loaded to mod " + name + " (" + config.id + ")!");
            return;
        }
        this.config = c;
    }
}
