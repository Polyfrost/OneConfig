package org.polyfrost.oneconfig.internal.legacy.compat;

//? if = 1.8.9 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.platform.TextInputManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.opengl.Display;
import org.polyfrost.oneconfig.internal.legacy.User;
import org.polyfrost.oneconfig.internal.legacy.Window;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;
import java.util.function.Supplier;

public interface MinecraftCompat {
    default Window getWindow() {
        return new Window((Minecraft) (Object) this);
    }

    default boolean isWindowActive() {
        return Display.isActive();
    }

    default TextInputManager textInputManager() {
        return TextInputManager.getInstance();
    }

    default DebugOverlay getDebugOverlay() {
        Minecraft minecraft = (Minecraft) (Object) this;
        return () -> minecraft.options.debugEnabled;
    }

    default GameProfile getGameProfile() {
        return ((Minecraft) (Object) this).getSession().getProfile();
    }

    default User getUser() {
        return new User(((Minecraft) (Object) this).getSession().getProfile());
    }

    default boolean isLocalServer() {
        return ((Minecraft) (Object) this).isIntegratedServerRunning();
    }

    default int getFps() {
        return Minecraft.getCurrentFps();
    }

    default CompletableFuture<Void> reloadResourcePacks() {
        ((Minecraft) (Object) this).reloadResources();
        return CompletableFuture.completedFuture(null);
    }

    default VanillaPackResources getVanillaPackResources() {
        return path -> {
            String joined = String.join("/", path);
            ResourceManager resources = ResourceManager.client();
            return resources.hasResource(joined) ? () -> resources.getResource(joined) : null;
        };
    }

    default void tell(Runnable task) {
        Minecraft minecraft = (Minecraft) (Object) this;
        FutureTask<Void> future = new FutureTask<>(task, null);
        synchronized (minecraft.tasks) {
            minecraft.tasks.add(future);
        }
    }

    default <T> CompletableFuture<T> submit(Supplier<T> task) {
        Minecraft minecraft = (Minecraft) (Object) this;
        if (minecraft.isSameThread()) {
            return CompletableFuture.completedFuture(task.get());
        }

        CompletableFuture<T> future = new CompletableFuture<>();
        minecraft.executeTask(() -> {
            try {
                future.complete(task.get());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    interface VanillaPackResources {
        ResourceSupplier getRootResource(String... path);
    }

    interface DebugOverlay {
        boolean showDebugScreen();
    }

    interface ResourceSupplier {
        InputStream get() throws IOException;
    }
}
*///?}
