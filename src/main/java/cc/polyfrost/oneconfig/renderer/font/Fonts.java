package cc.polyfrost.oneconfig.renderer.font;

public enum Fonts {
    BOLD(new Font("inter-bold", "/assets/oneconfig/font/Bold.otf")),
    SEMIBOLD(new Font("inter-semibold", "/assets/oneconfig/font/SemiBold.otf")),
    MEDIUM(new Font("inter-medium", "/assets/oneconfig/font/Medium.otf")),
    REGULAR(new Font("inter-regular", "/assets/oneconfig/font/Regular.otf")),
    MINECRAFT_REGULAR(new Font("mc-regular", "/assets/oneconfig/font/Minecraft-Regular.otf")),
    MINECRAFT_BOLD(new Font("mc-bold", "/assets/oneconfig/font/Minecraft-Bold.otf"));
    public final Font font;

    Fonts(Font font) {
        this.font = font;
    }
}
