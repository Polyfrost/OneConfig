package org.polyfrost.oneconfig.api.config.v1;

import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConstructorSetupDeferralTest {

    private static final String ID = "ctor_setup_deferral.json";

    @SuppressWarnings("unused")
    public static class EagerInstanceConfig extends Config {
        public static final EagerInstanceConfig INSTANCE = new EagerInstanceConfig();

        @Switch(title = "Disable")
        public static boolean disable = false;
        @Switch(title = "Cleaner")
        public static boolean cleaner = true;

        public EagerInstanceConfig() {
            super(ID, "Constructor Setup Deferral", Category.QOL);
            addDependency("cleaner", null, () -> disable ? Property.Display.DISABLED : Property.Display.SHOWN);
        }
    }

    @Test
    void constructorSetupKeepsCodeDefaultsAndStoredValues() throws Exception {
        Path dir = ConfigManager.active().getFolder();
        Files.createDirectories(dir);
        Path file = dir.resolve(ID);
        Files.writeString(file, "{ \"disable\": true, \"cleaner\": false }");

        try {
            EagerInstanceConfig config = EagerInstanceConfig.INSTANCE;
            assertNull(config.getTree());
            config.preload();

            Property<?> cleaner = config.getProperty("cleaner");
            assertEquals(Boolean.TRUE, cleaner.getMetadata("default"), "code default must survive the constructor");
            assertEquals(Boolean.FALSE, config.getProperty("disable").getMetadata("default"));

            assertTrue(EagerInstanceConfig.disable);
            assertFalse(EagerInstanceConfig.cleaner);

            assertEquals(Property.Display.DISABLED, cleaner.getDisplay());
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
