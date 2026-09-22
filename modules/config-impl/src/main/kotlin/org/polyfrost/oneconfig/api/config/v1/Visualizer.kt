/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.api.config.v1

import androidx.compose.runtime.Composable

/**
 * Visualizers are procedures that take a [Property] and create a composable to represent it
 *
 * To create a custom visualizer implement this interface or pass a lambda
 * ```kotlin
 * val myVisualizer = Visualizer { prop -> Text(prop.get().toString()) }
 * ```
 *
 * The built-in visualizer token classes such as [SwitchVisualizer] delegate their rendering
 * to implementations registered via [register] which are provided by the UI layer
 *
 * There is no distinction between built-in and third-party visualizers because both use the same API
 */
@Suppress("UNCHECKED_CAST")
fun interface Visualizer {

    @Composable
    fun visualize(prop: Property<*>)

    class ButtonVisualizer : Visualizer {
        @Composable override fun visualize(prop: Property<*>) { Visualizer[ButtonVisualizer::class.java]?.visualize(prop) }
    }

    class ColorVisualizer : Visualizer {
        @Composable override fun visualize(prop: Property<*>) { Visualizer[ColorVisualizer::class.java]?.visualize(prop) }
    }

    class DropdownVisualizer : Visualizer {
        @Composable override fun visualize(prop: Property<*>) { Visualizer[DropdownVisualizer::class.java]?.visualize(prop) }
    }

    class KeybindVisualizer : Visualizer {
        @Composable override fun visualize(prop: Property<*>) { Visualizer[KeybindVisualizer::class.java]?.visualize(prop) }
    }

    class InfoVisualizer : Visualizer {
        @Composable override fun visualize(prop: Property<*>) { Visualizer[InfoVisualizer::class.java]?.visualize(prop) }
    }

    class DraggableListVisualizer : Visualizer {
        @Composable override fun visualize(prop: Property<*>) { Visualizer[DraggableListVisualizer::class.java]?.visualize(prop) }
    }

    class MultiSelectDropdownVisualizer : Visualizer {
        @Composable override fun visualize(prop: Property<*>) { Visualizer[MultiSelectDropdownVisualizer::class.java]?.visualize(prop) }
    }

    class NumberVisualizer : Visualizer {
        @Composable override fun visualize(prop: Property<*>) { Visualizer[NumberVisualizer::class.java]?.visualize(prop) }
    }

    class RadioVisualizer : Visualizer {
        @Composable override fun visualize(prop: Property<*>) { Visualizer[RadioVisualizer::class.java]?.visualize(prop) }
    }

    class SliderVisualizer : Visualizer {
        @Composable override fun visualize(prop: Property<*>) { Visualizer[SliderVisualizer::class.java]?.visualize(prop) }
    }

    /**
     * A slider with two thumbs backed by a single property holding a start and end pair as a two-element
     * numeric array or list
     *
     * Writes both ends at once keeping start below end
     */
    class RangeSliderVisualizer : Visualizer {
        @Composable override fun visualize(prop: Property<*>) { Visualizer[RangeSliderVisualizer::class.java]?.visualize(prop) }
    }

    /**
     * A slider with a third "inherited" state for overrides that fall back to a broader setting when unset
     *
     * The property stores a sentinel while inheriting which is either `null` or the value of the `inheritSentinel`
     * metadata
     *
     * The inherited value itself comes from the `inheritedValue` metadata and is either a number or a
     * `java.util.function.Supplier` of one so it can track a live global
     *
     * Prefer a numeric sentinel over `null` where the backing type allows it because [CompatSnapshots] cannot
     * record a null
     *
     * On a foreign-backed config a null-sentinel property must opt out of snapshots with
     * [CompatSnapshots.NO_SNAPSHOT_META] or an older value will resurrect itself and undo "inherit"
     */
    class InheritableSliderVisualizer : Visualizer {
        @Composable override fun visualize(prop: Property<*>) { Visualizer[InheritableSliderVisualizer::class.java]?.visualize(prop) }
    }

    /**
     * An ordered chain of numbers held in one property as a numeric array or list
     *
     * Entries can be reordered and edited in place and removed or appended
     *
     * `lockedLeading` metadata fixes that many entries at the front and `maxEntries` caps the length
     *
     * `entryLabel`/`nextValue` are both `java.util.function.Function`s that control how an entry reads and what "add" appends
     */
    class NumberChainVisualizer : Visualizer {
        @Composable override fun visualize(prop: Property<*>) { Visualizer[NumberChainVisualizer::class.java]?.visualize(prop) }
    }

    class SwitchVisualizer : Visualizer {
        @Composable override fun visualize(prop: Property<*>) { Visualizer[SwitchVisualizer::class.java]?.visualize(prop) }
    }

    class CheckboxVisualizer : Visualizer {
        @Composable override fun visualize(prop: Property<*>) { Visualizer[CheckboxVisualizer::class.java]?.visualize(prop) }
    }

    class TextVisualizer : Visualizer {
        @Composable override fun visualize(prop: Property<*>) { Visualizer[TextVisualizer::class.java]?.visualize(prop) }
    }

    class TextListVisualizer : Visualizer {
        @Composable override fun visualize(prop: Property<*>) { Visualizer[TextListVisualizer::class.java]?.visualize(prop) }
    }

    /**
     * A searchable Minecraft item selector backed by a `String[]` or `List<String>` of
     * namespaced registry IDs
     *
     * A `maxEntries` metadata value of one makes it a single selector
     */
    class ItemListVisualizer : Visualizer {
        @Composable override fun visualize(prop: Property<*>) { Visualizer[ItemListVisualizer::class.java]?.visualize(prop) }
    }

    class FileVisualizer : Visualizer {
        @Composable override fun visualize(prop: Property<*>) { Visualizer[FileVisualizer::class.java]?.visualize(prop) }
    }

    class FileListVisualizer : Visualizer {
        @Composable override fun visualize(prop: Property<*>) { Visualizer[FileListVisualizer::class.java]?.visualize(prop) }
    }

    class ColorListVisualizer : Visualizer {
        @Composable override fun visualize(prop: Property<*>) { Visualizer[ColorListVisualizer::class.java]?.visualize(prop) }
    }

    class NumberListVisualizer : Visualizer {
        @Composable override fun visualize(prop: Property<*>) { Visualizer[NumberListVisualizer::class.java]?.visualize(prop) }
    }

    class SliderListVisualizer : Visualizer {
        @Composable override fun visualize(prop: Property<*>) { Visualizer[SliderListVisualizer::class.java]?.visualize(prop) }
    }

    companion object {
        private val registry = HashMap<Class<out Visualizer>, Visualizer>()

        /**
         * Register a rendering implementation for a built-in [Visualizer] token class
         *
         * Called by the UI layer on startup to wire up the built-in visualizer types
         *
         * This will overwrite the existing visualizer if there is one already registered
         */
        fun register(cls: Class<out Visualizer>, impl: Visualizer) {
            registry[cls] = impl
        }

        operator fun get(cls: Class<out Visualizer>): Visualizer? = registry[cls]
    }
}
