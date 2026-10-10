package org.polyfrost.oneconfig.internal.ui.api;

import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.Properties;
import org.polyfrost.oneconfig.api.config.v1.Property;
import org.polyfrost.oneconfig.api.config.v1.Tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModToggleSupportTest {
    private static Property<Boolean> bool(String id, boolean value) {
        return Properties.simple(id, id, null, value, Boolean.class);
    }

    @Test
    void findsTheEnabledOptionAtTheRoot() {
        Property<Boolean> enabled = bool("enabled", true);
        Tree tree = Tree.tree("toggle-plain.json").put(bool("showInInventory", true), enabled);

        assertSame(enabled, ModToggleSupportKt.findMasterSwitch(tree));
    }

    @Test
    void toleratesKotlinStyleAndDifferentlyCasedNames() {
        Property<Boolean> kotlin = bool("isEnabled", false);
        assertSame(kotlin, ModToggleSupportKt.findMasterSwitch(Tree.tree("toggle-kotlin.json").put(kotlin)));

        Property<Boolean> spaced = bool("mod_enabled", false);
        assertSame(spaced, ModToggleSupportKt.findMasterSwitch(Tree.tree("toggle-spaced.json").put(spaced)));
    }

    @Test
    void ignoresFeatureSwitchesAndNonBooleans() {
        Tree feature = Tree.tree("animation").put(bool("enabled", true));
        Tree tree = Tree.tree("toggle-nested.json").put(
            feature,
            bool("enableSounds", true),
            bool("confirmEnabled", true),
            Properties.simple("enabled", "Enabled", null, "yes", String.class)
        );

        assertNull(ModToggleSupportKt.findMasterSwitch(tree));
    }

    @Test
    void theToggleWritesThroughToTheOption() {
        Property<Boolean> enabled = bool("enabled", true);
        PropertyModToggle toggle = new PropertyModToggle(enabled);

        assertTrue(toggle.isEnabled());
        toggle.setEnabled(false);
        assertFalse(toggle.isEnabled());
        assertEquals(Boolean.FALSE, enabled.get());
    }
}
