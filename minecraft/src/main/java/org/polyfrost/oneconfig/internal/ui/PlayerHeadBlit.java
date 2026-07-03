package org.polyfrost.oneconfig.internal.ui;

import com.mojang.blaze3d.platform.NativeImage;

public final class PlayerHeadBlit {
    private static final int HEAD_SIZE = 32;
    private static final int SKIN_FACE_SIZE = 8;
    private static final int SKIN_HEAD_U = 8;
    private static final int SKIN_HEAD_V = 8;
    private static final int SKIN_HAT_U = 40;
    private static final int SKIN_HAT_V = 8;

    private PlayerHeadBlit() {
    }

    public static NativeImage extractHead(NativeImage skin) {
        int scale = HEAD_SIZE / SKIN_FACE_SIZE;
        NativeImage head = new NativeImage(HEAD_SIZE, HEAD_SIZE, false);
        blitRegion(skin, head, SKIN_HEAD_U, SKIN_HEAD_V, SKIN_FACE_SIZE, SKIN_FACE_SIZE, scale);
        blitRegion(skin, head, SKIN_HAT_U, SKIN_HAT_V, SKIN_FACE_SIZE, SKIN_FACE_SIZE, scale);
        return head;
    }

    private static void blitRegion(
        NativeImage src,
        NativeImage dst,
        int srcU,
        int srcV,
        int width,
        int height,
        int scale
    ) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                //? if < 1.21.4 {
                /*int color = src.getPixelRGBA(srcU + x, srcV + y);
                if ((color >>> 24) == 0) {
                    continue;
                }
                for (int dy = 0; dy < scale; dy++) {
                    for (int dx = 0; dx < scale; dx++) {
                        int dx0 = x * scale + dx;
                        int dy0 = y * scale + dy;
                        dst.setPixelRGBA(dx0, dy0, composite(color, dst.getPixelRGBA(dx0, dy0)));
                    }
                }
                *///? } else {
                int color = src.getPixel(srcU + x, srcV + y);
                if ((color >>> 24) == 0) {
                    continue;
                }
                for (int dy = 0; dy < scale; dy++) {
                    for (int dx = 0; dx < scale; dx++) {
                        int dx0 = x * scale + dx;
                        int dy0 = y * scale + dy;
                        dst.setPixel(dx0, dy0, composite(color, dst.getPixel(dx0, dy0)));
                    }
                }
                //? }
            }
        }
    }

    private static int composite(int src, int dst) {
        int srcA = src >>> 24;
        if (srcA == 0xFF) {
            return src;
        }
        int dstA = dst >>> 24;
        if (dstA == 0) {
            return src;
        }
        int outA = srcA + dstA * (0xFF - srcA) / 0xFF;
        if (outA == 0) {
            return 0;
        }
        int outR = blendChannel(src & 0xFF, dst & 0xFF, srcA, dstA, outA);
        int outG = blendChannel((src >> 8) & 0xFF, (dst >> 8) & 0xFF, srcA, dstA, outA);
        int outB = blendChannel((src >> 16) & 0xFF, (dst >> 16) & 0xFF, srcA, dstA, outA);
        return (outA << 24) | (outB << 16) | (outG << 8) | outR;
    }

    private static int blendChannel(int srcC, int dstC, int srcA, int dstA, int outA) {
        return (srcC * srcA + dstC * dstA * (0xFF - srcA) / 0xFF) / outA;
    }
}
