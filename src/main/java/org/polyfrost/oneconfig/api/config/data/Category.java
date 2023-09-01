package org.polyfrost.oneconfig.api.config.data;

public enum Category {
	HUD("hud.svg", "oneconfig.hud"),
	COMBAT("console.svg", "oneconfig.combat"),
	QOL("spanner.svg", "oneconfig.qol"),
	HYPIXEL("hypixel.svg", "oneconfig.hypixel"),
	OTHER(null, "oneconfig.other");

	public final String name;
	public final String iconPath;
	Category(String iconPath, String name) {
		this.iconPath = iconPath;
		this.name = name;
	}
}
