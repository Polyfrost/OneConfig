package org.polyfrost.oneconfig.internal.ui.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;
import org.polyfrost.oneconfig.api.config.v1.Properties;
import org.polyfrost.oneconfig.api.config.v1.Tree;
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager;
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind;
import org.polyfrost.oneconfig.internal.ui.keybind.MinecraftKeybindRegistrar;

class ConfigRegistryKeybindsTest {
    private static final String ID = "config-registry-keybinds-test.json";

    private static OneConfigKeybind bind() {
        return KeybindManager.builder().build();
    }

    private static Tree tree(OneConfigKeybind bind) {
        Tree tree = Tree.tree(ID);
        tree.setTitle("Keybinds");
        tree.put(Tree.tree("nested").put(Properties.simple("bind", "Bind", "", bind)));
        return tree;
    }

    @AfterEach
    void clear() throws IOException {
        ConfigRegistry.INSTANCE.setOff(Set.of());
        ConfigRegistry.INSTANCE.unregister(ID);
        ConfigManager.active().unregister(ID);
        Files.deleteIfExists(ConfigManager.active().getFolder().resolve(ID));
    }

    @Test
    void aKeybindStopsWhileItsModIsOffAndReturnsWithIt() {
        OneConfigKeybind bind = bind();
        Tree tree = tree(bind);
        ConfigManager.active().register(tree);
        ConfigRegistry.INSTANCE.registerTree(tree, ConfigSource.OC);
        assertTrue(KeybindManager.isRegistered(bind));

        ConfigRegistry.INSTANCE.setOff(Set.of(ID));
        assertFalse(KeybindManager.isRegistered(bind));
        // as a registry reload after a profile switch does
        ConfigRegistry.INSTANCE.registerTree(tree, ConfigSource.OC);
        assertFalse(KeybindManager.isRegistered(bind));

        ConfigRegistry.INSTANCE.setOff(Set.of());
        assertTrue(KeybindManager.isRegistered(bind));
        KeybindManager.unregister(bind);
    }

    @Test
    void aKeybindBuiltOnEveryReadStillStopsWhileItsModIsOff() {
        List<OneConfigKeybind> built = new ArrayList<>();
        Tree tree = Tree.tree(ID);
        tree.setTitle("Keybinds");
        tree.put(Properties.functional(() -> {
            OneConfigKeybind bind = bind();
            built.add(bind);
            return bind;
        }, bind -> {
        }, "bind", "Bind", "", OneConfigKeybind.class));

        MinecraftKeybindRegistrar.scan(tree);
        assertTrue(built.stream().anyMatch(KeybindManager::isRegistered));

        MinecraftKeybindRegistrar.unregister(tree);
        assertTrue(built.stream().noneMatch(KeybindManager::isRegistered));
    }

    @Test
    void aReplacedConfigTakesItsKeybindsWithIt() {
        OneConfigKeybind old = bind();
        OneConfigKeybind replacement = bind();
        ConfigRegistry.INSTANCE.registerTree(tree(old), ConfigSource.OC);
        assertTrue(KeybindManager.isRegistered(old));

        ConfigRegistry.INSTANCE.registerTree(tree(replacement), ConfigSource.OC);
        assertFalse(KeybindManager.isRegistered(old));
        assertTrue(KeybindManager.isRegistered(replacement));
        KeybindManager.unregister(replacement);
    }

    @Test
    void aReplacedConfigWhoseCardHotwireTookOutTakesItsKeybindsWithIt() {
        OneConfigKeybind old = bind();
        OneConfigKeybind replacement = bind();
        ConfigRegistry.INSTANCE.registerTree(tree(old), ConfigSource.OC);
        ConfigRegistry.INSTANCE.unregister(ID);

        ConfigRegistry.INSTANCE.registerTree(tree(replacement), ConfigSource.OC);
        assertFalse(KeybindManager.isRegistered(old));
        assertTrue(KeybindManager.isRegistered(replacement));
        KeybindManager.unregister(replacement);
    }

    @Test
    void aRebuiltTreeKeepsTheKeybindItShares() {
        OneConfigKeybind shared = bind();
        ConfigRegistry.INSTANCE.registerTree(tree(shared), ConfigSource.OC);

        ConfigRegistry.INSTANCE.registerTree(tree(shared), ConfigSource.OC);
        assertTrue(KeybindManager.isRegistered(shared));
        KeybindManager.unregister(shared);
    }
}
