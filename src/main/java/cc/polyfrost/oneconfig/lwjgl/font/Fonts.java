package cc.polyfrost.oneconfig.lwjgl.font;

public enum Fonts {
    BOLD(new Font("inter-bold", "/assets/oneconfig/font/Bold.otf")),
    SEMIBOLD(new Font("inter-semibold", "/assets/oneconfig/font/SemiBold.otf")),
    MEDIUM(new Font("inter-medium", "/assets/oneconfig/font/Medium.otf")),
    REGULAR(new Font("inter-regular", "/assets/oneconfig/font/Regular.otf")),
    MINECRAFT(new Font("mc-regular", "/assets/oneconfig/font/Minecraft.otf"));
    public final Font font;

    Fonts(Font font) {
        this.font = font;
    }
}
