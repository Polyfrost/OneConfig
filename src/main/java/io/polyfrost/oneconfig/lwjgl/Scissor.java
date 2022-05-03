package io.polyfrost.oneconfig.lwjgl;

public class Scissor {
    public int x;
    public int y;
    public int width;
    public int height;

    public Scissor(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Scissor(Scissor scissor) {
        this.x = scissor.x;
        this.y = scissor.y;
        this.width = scissor.width;
        this.height = scissor.height;
    }
}
