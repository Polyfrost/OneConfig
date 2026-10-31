package org.polyfrost.oneconfig.api.config.v1;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LateInitializationTest {
    static class LateConfig extends Config {
        LateConfig(String id) {
            super(id, "Late", Category.QOL);
        }
    }

    @AfterEach
    void reopenStartup() throws ReflectiveOperationException {
        Field initialized = ConfigManager.class.getDeclaredField("initialized");
        initialized.setAccessible(true);
        initialized.setBoolean(null, false);
    }

    @Test
    void configsCreatedLateAreInitializedOnceTheCreatorReturns() {
        AtomicReference<Config> created = new AtomicReference<>();
        ConfigManager.initializeLate(() -> {
            created.set(new LateConfig("late-initialization-test.json"));
            assertNull(created.get().getTree());
            assertTrue(ConfigManager.createdConfigs().contains(created.get()));
        });
        assertNotNull(created.get().getTree());
        assertTrue(ConfigManager.createdConfigs().contains(created.get()));
    }
}
