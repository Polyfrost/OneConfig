package cc.polyfrost.oneconfig.lwjgl.image;

public enum Images {
    HUE_GRADIENT("/assets/oneconfig/colorui/huegradient.png"),
    COLOR_WHEEL("/assets/oneconfig/colorui/colorwheel.png"),
    ALPHA_GRID("/assets/oneconfig/colorui/alphagrid.png");

    public final String filePath;

    Images(String filePath) {
        this.filePath = filePath;
    }
}