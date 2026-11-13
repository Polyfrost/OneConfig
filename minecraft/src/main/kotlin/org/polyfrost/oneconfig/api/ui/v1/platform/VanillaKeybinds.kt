@file:OptIn(ExperimentalOneConfigUiApi::class)
@file:JvmName("VanillaKeybinds")

package org.polyfrost.oneconfig.api.ui.v1.platform

import net.minecraft.client.KeyMapping
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindHelper
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindMetadata
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.internal.ui.keybind.VanillaKeyMappingProperties

@ExperimentalOneConfigUiApi
fun KeybindHelper.Companion.vanillaProperty(keyMapping: KeyMapping): Property<OneConfigKeybind?> =
    VanillaKeyMappingProperties.propertyFor(keyMapping)
