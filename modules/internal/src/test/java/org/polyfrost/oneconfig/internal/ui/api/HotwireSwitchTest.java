package org.polyfrost.oneconfig.internal.ui.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.moon.hotwire.api.HotwireApi;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.CompatSnapshots;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;
import org.polyfrost.oneconfig.api.config.v1.Properties;
import org.polyfrost.oneconfig.api.config.v1.Property;
import org.polyfrost.oneconfig.api.config.v1.Tree;

class HotwireSwitchTest {
    private static final String ID = "hotwire-switch-test";

    @AfterEach
    void clear() {
        HotwireApi.CONTROLS.clear();
        CompatSnapshots.untrack(ID);
        ConfigManager.active().unregister(ID);
    }

    @Test
    void aModWritesItsUnsavedChangesBeforeItIsTurnedOff() throws ReflectiveOperationException {
        Property<Boolean> property = Properties.simple("enabled", "Enabled", "", true);
        Tree tree = Tree.tree(ID).put(property);
        tree.addMetadata("custom_save", (Runnable) () -> HotwireApi.CONTROLS.add("save"));
        CompatSnapshots.register(tree);
        HotwireApi.CONTROLS.clear();
        // the snapshot of an earlier run can already have set the other value
        property.setAs(!property.get());

        // "switch" can't be called by name from Java
        Method turn = HotwireBridge.class.getMethod("switch", Collection.class, boolean.class);
        turn.invoke(HotwireBridge.INSTANCE, List.of("mod"), false);

        assertEquals(List.of("save", "off [mod]"), HotwireApi.CONTROLS);
    }
}
