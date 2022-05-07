package cc.polyfrost.oneconfig.lwjgl.font;

public enum Fonts {
    // https://opentype.js.org/font-inspector.html
    BOLD(new Font("inter-bold", "/assets/oneconfig/font/Bold.otf", 2816, 2728, -680)),
    SEMIBOLD(new Font("inter-semibold", "/assets/oneconfig/font/SemiBold.otf", 2816, 2728, -680)),
    MEDIUM(new Font("inter-medium", "/assets/oneconfig/font/Medium.otf", 2816, 2728, -680)),
    REGULAR(new Font("inter-regular", "/assets/oneconfig/font/Regular.otf", 2816, 2728, -680));

    public final Font font;

    Fonts(Font font) {
        this.font = font;
    }
}
