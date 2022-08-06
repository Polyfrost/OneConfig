package cc.polyfrost.oneconfig.platform;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.nanovg.NVGLUFramebuffer;

public interface NanoVGPlatform {
    int NVG_ANTIALIAS = 1;
    int NVG_STENCIL_STROKES = 2;
    int NVG_DEBUG = 4;
    int NVG_IMAGE_NODELETE = 65536;

    int nnvglCreateImageFromHandle(long var0, int var2, int var3, int var4, int var5);

    int nvglCreateImageFromHandle(long ctx, int textureId, int w, int h, int flags);

    int nnvglImageHandle(long var0, int var2);

    int nvglImageHandle(long ctx, int image);

    long nnvgCreate(int var0);

    long nvgCreate(int flags);

    void nnvgDelete(long var0);

    void nvgDelete(long ctx);

    long nnvgluCreateFramebuffer(long var0, int var2, int var3, int var4);

    @Nullable
    NVGLUFramebuffer nvgluCreateFramebuffer(long ctx, int w, int h, int imageFlags);

    void nnvgluBindFramebuffer(long var0, long var2);

    void nvgluBindFramebuffer(long ctx, @Nullable NVGLUFramebuffer fb);

    void nnvgluDeleteFramebuffer(long var0, long var2);

    void nvgluDeleteFramebuffer(long ctx, NVGLUFramebuffer fb);

    void triggerStaticInitialization();
}
