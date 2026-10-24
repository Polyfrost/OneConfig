package org.polyfrost.oneconfig.internal.utils;

import java.awt.image.BufferedImage;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.utils.v1.SdlClipboard;

//? if sdl {
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDL_ClipboardCleanupCallback;
import org.lwjgl.sdl.SDL_ClipboardDataCallback;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.sdl.SDLClipboard.SDL_ClearClipboardData;
import static org.lwjgl.sdl.SDLClipboard.SDL_GetClipboardText;
import static org.lwjgl.sdl.SDLClipboard.SDL_SetClipboardData;
import static org.lwjgl.sdl.SDLClipboard.SDL_SetClipboardText;
import static org.lwjgl.system.MemoryUtil.memByteBuffer;
import static org.lwjgl.system.MemoryUtil.memGetAddress;
import static org.lwjgl.system.MemoryUtil.memPutAddress;
import static org.lwjgl.system.MemoryUtil.nmemAllocChecked;
import static org.lwjgl.system.Pointer.POINTER_SIZE;
//?}

public final class SdlClipboardImpl implements SdlClipboard {
    //? if sdl {
    @Override
    public boolean isAvailable() {
        return true;
    }

    @Nullable
    @Override
    public String getString() {
        String text = onRenderThread(() -> SDL_GetClipboardText());
        return text == null || text.isEmpty() ? null : text;
    }

    @Override
    public boolean setString(String value) {
        return onRenderThread(() -> SDL_SetClipboardText(value));
    }

    @Override
    public boolean setImage(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", out)) return false;
        byte[] png = out.toByteArray();
        return onRenderThread(() -> {
            // the length followed by the PNG, owned by SDL until it calls the cleanup callback
            long data = nmemAllocChecked(POINTER_SIZE + png.length);
            memPutAddress(data, png.length);
            memByteBuffer(data + POINTER_SIZE, png.length).put(png);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                return SDL_SetClipboardData(ImageCallbacks.DATA, ImageCallbacks.CLEANUP, data, stack.pointers(stack.UTF8("image/png")));
            }
        });
    }

    @Override
    public boolean clear() {
        return onRenderThread(() -> SDL_ClearClipboardData());
    }

    // SDL only allows clipboard calls on its main thread which is the render thread
    private static <T> T onRenderThread(Supplier<T> task) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isSameThread()) return task.get();
        return CompletableFuture.supplyAsync(task, minecraft::execute).join();
    }

    // shared by every image and never freed as SDL can ask for the data whenever something pastes it
    private static final class ImageCallbacks {
        private static final SDL_ClipboardDataCallback DATA = SDL_ClipboardDataCallback.create((userdata, mimeType, size) -> {
            memPutAddress(size, memGetAddress(userdata));
            return userdata + POINTER_SIZE;
        });
        private static final SDL_ClipboardCleanupCallback CLEANUP = SDL_ClipboardCleanupCallback.create(MemoryUtil::nmemFree);
    }
    //?} else {
    /*@Override
    public boolean isAvailable() {
        return false;
    }

    @Nullable
    @Override
    public String getString() {
        return null;
    }

    @Override
    public boolean setString(String value) {
        return false;
    }

    @Override
    public boolean setImage(BufferedImage image) {
        return false;
    }

    @Override
    public boolean clear() {
        return false;
    }
    *///?}
}
