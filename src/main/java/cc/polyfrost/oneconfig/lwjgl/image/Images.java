package cc.polyfrost.oneconfig.lwjgl.image;

public enum Images {
    HUE_GRADIENT("/assets/oneconfig/colorui/huegradient.png"),
    COLOR_WHEEL("/assets/oneconfig/colorui/colorwheel.png"),
    HSB_GRADIENT("/assets/oneconfig/colorui/hsbgradient.png"),
    ALPHA_GRID("/assets/oneconfig/colorui/alphagrid.png");

    public final String filePath;

    Images(String filePath) {
        this.filePath = filePath;
    }
}