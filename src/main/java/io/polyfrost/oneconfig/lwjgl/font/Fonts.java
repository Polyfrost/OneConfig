package io.polyfrost.oneconfig.lwjgl.font;

public enum Fonts {
    INTER_BOLD(new Font("inter-bold", "/assets/oneconfig/font/Inter-Bold.otf")),
    INTER_REGULAR(new Font("inter-regular", "/assets/oneconfig/font/Inter-Regular.otf")),
    INTER_SEMIBOLD(new Font("inter-semibold", "/assets/oneconfig/font/Inter-SemiBold.otf")),
    INTER_MEDIUM(new Font("inter-medium", "/assets/oneconfig/font/Inter-Medium.otf")),
    MC_REGULAR(new Font("mc-regular", "/assets/oneconfig/font/Minecraft-Regular.otf"));

    public final Font font;

    Fonts(Font font) {
        this.font = font;
    }
}
