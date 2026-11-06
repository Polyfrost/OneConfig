package org.polyfrost.oneconfig.internal.ui.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.Properties;
import org.polyfrost.oneconfig.api.config.v1.Tree;
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager;
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind;

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
    void clear() {
        ConfigRegistry.INSTANCE.unregister(ID);
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
    void aRebuiltTreeKeepsTheKeybindItShares() {
        OneConfigKeybind shared = bind();
        ConfigRegistry.INSTANCE.registerTree(tree(shared), ConfigSource.OC);

        ConfigRegistry.INSTANCE.registerTree(tree(shared), ConfigSource.OC);
        assertTrue(KeybindManager.isRegistered(shared));
        KeybindManager.unregister(shared);
    }
}
