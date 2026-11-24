package org.polyfrost.oneconfig.api.event.v1;

import net.kyori.adventure.text.Component;
import org.polyfrost.oneconfig.api.platform.v1.CompatibilityPlatform;
import org.polyfrost.oneconfig.api.platform.v1.Keys;
import org.polyfrost.oneconfig.api.platform.v1.ModInfo;
import org.polyfrost.oneconfig.api.platform.v1.Options;

import java.util.Collections;
import java.util.Set;

// Minimal ServiceLoader-provided platform so EventManager can initialize in unit tests.
public class TestCompatibilityPlatform implements CompatibilityPlatform {
    @Override
    public void displayChatMessage(Component text) {
    }

    @Override
    public Set<ModInfo> getMods() {
        return Collections.emptySet();
    }

    @Override
    public boolean isDevelopment() {
        return false;
    }

    @Override
    public String version() {
        return "test";
    }

    @Override
    public String loader() {
        return "test";
    }

    @Override
    public Options options() {
        throw new UnsupportedOperationException("not available in tests");
    }

    @Override
    public Keys keys() {
        throw new UnsupportedOperationException("not available in tests");
    }

    @Override
    public long windowHandle() {
        return 0L;
    }

    @Override
    public int fps() {
        return 0;
    }

    @Override
    public String resolveComponent(Component component) {
        return component.toString();
    }

    @Override
    public Object wrapPlatformComponent(Object component) {
        return component;
    }
}
