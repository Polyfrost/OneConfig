package org.polyfrost.oneconfig.internal.ui.api.settings

import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.internal.ui.components.settings.BooleanOption
import org.polyfrost.oneconfig.internal.ui.components.settings.ButtonOption
import org.polyfrost.oneconfig.internal.ui.components.settings.ColorListOption
import org.polyfrost.oneconfig.internal.ui.components.settings.ColorOption
import org.polyfrost.oneconfig.internal.ui.components.settings.DraggableListOption
import org.polyfrost.oneconfig.internal.ui.components.settings.DropdownOption
import org.polyfrost.oneconfig.internal.ui.components.settings.EffectListOption
import org.polyfrost.oneconfig.internal.ui.components.settings.FileListOption
import org.polyfrost.oneconfig.internal.ui.components.settings.FileOption
import org.polyfrost.oneconfig.internal.ui.components.settings.InfoOption
import org.polyfrost.oneconfig.internal.ui.components.settings.InheritableSliderOption
import org.polyfrost.oneconfig.internal.ui.components.settings.ItemListOption
import org.polyfrost.oneconfig.internal.ui.components.settings.KeybindOption
import org.polyfrost.oneconfig.internal.ui.components.settings.LocalOptionEnabled
import org.polyfrost.oneconfig.internal.ui.components.settings.MultiSelectDropdownOption
import org.polyfrost.oneconfig.internal.ui.components.settings.NumberChainOption
import org.polyfrost.oneconfig.internal.ui.components.settings.NumberListOption
import org.polyfrost.oneconfig.internal.ui.components.settings.NumberOption
import org.polyfrost.oneconfig.internal.ui.components.settings.RadioButtonOption
import org.polyfrost.oneconfig.internal.ui.components.settings.RangeListOption
import org.polyfrost.oneconfig.internal.ui.components.settings.RangeOption
import org.polyfrost.oneconfig.internal.ui.components.settings.SliderOption
import org.polyfrost.oneconfig.internal.ui.components.settings.TextListOption
import org.polyfrost.oneconfig.internal.ui.components.settings.TextOption
import org.polyfrost.oneconfig.internal.ui.components.settings.enabledAwareVisualizers

internal object BuiltinVisualizers {
    fun register() {
        registerEnabledAware(Visualizer.SwitchVisualizer::class.java, Visualizer { prop ->
            BooleanOption(BooleanOptionData(prop, BooleanOptionData.Style.Switch), LocalOptionEnabled.current)
        })
        registerEnabledAware(Visualizer.CheckboxVisualizer::class.java, Visualizer { prop ->
            BooleanOption(BooleanOptionData(prop, BooleanOptionData.Style.Checkbox), LocalOptionEnabled.current)
        })
        registerEnabledAware(Visualizer.SliderVisualizer::class.java, Visualizer { prop ->
            SliderOption(SliderOptionData(prop), LocalOptionEnabled.current)
        })
        Visualizer.register(Visualizer.RangeSliderVisualizer::class.java, Visualizer { prop ->
            RangeOption(RangeOptionData(prop, RangeOptionData.Style.Slider))
        })
        Visualizer.register(Visualizer.RangeNumberVisualizer::class.java, Visualizer { prop ->
            RangeOption(RangeOptionData(prop, RangeOptionData.Style.Spinner))
        })
        Visualizer.register(Visualizer.InheritableSliderVisualizer::class.java, Visualizer { prop ->
            InheritableSliderOption(InheritableSliderOptionData(prop))
        })
        Visualizer.register(Visualizer.NumberChainVisualizer::class.java, Visualizer { prop ->
            NumberChainOption(NumberChainOptionData(prop))
        })
        registerEnabledAware(Visualizer.NumberVisualizer::class.java, Visualizer { prop ->
            NumberOption(NumberOptionData(prop), LocalOptionEnabled.current)
        })
        registerEnabledAware(Visualizer.TextVisualizer::class.java, Visualizer { prop ->
            TextOption(TextOptionData(prop), LocalOptionEnabled.current)
        })
        Visualizer.register(Visualizer.TextListVisualizer::class.java, Visualizer { prop ->
            TextListOption(TextListOptionData(prop))
        })
        registerEnabledAware(Visualizer.ItemListVisualizer::class.java, Visualizer { prop ->
            ItemListOption(ItemListOptionData(prop))
        })
        registerEnabledAware(Visualizer.EffectListVisualizer::class.java, Visualizer { prop ->
            EffectListOption(EffectListOptionData(prop))
        })
        registerEnabledAware(Visualizer.FileVisualizer::class.java, Visualizer { prop ->
            FileOption(FileOptionData(prop), LocalOptionEnabled.current)
        })
        Visualizer.register(Visualizer.FileListVisualizer::class.java, Visualizer { prop ->
            FileListOption(FileListOptionData(prop))
        })
        Visualizer.register(Visualizer.ColorListVisualizer::class.java, Visualizer { prop ->
            ColorListOption(ColorListOptionData(prop))
        })
        Visualizer.register(Visualizer.NumberListVisualizer::class.java, Visualizer { prop ->
            NumberListOption(NumberListOptionData(prop, NumberListOptionData.Style.Spinner))
        })
        Visualizer.register(Visualizer.SliderListVisualizer::class.java, Visualizer { prop ->
            NumberListOption(NumberListOptionData(prop, NumberListOptionData.Style.Slider))
        })
        Visualizer.register(Visualizer.RangeSliderListVisualizer::class.java, Visualizer { prop ->
            RangeListOption(RangeListOptionData(prop, RangeOptionData.Style.Slider))
        })
        Visualizer.register(Visualizer.RangeNumberListVisualizer::class.java, Visualizer { prop ->
            RangeListOption(RangeListOptionData(prop, RangeOptionData.Style.Spinner))
        })
        registerEnabledAware(Visualizer.DropdownVisualizer::class.java, Visualizer { prop ->
            DropdownOption(DropdownOptionData(prop), LocalOptionEnabled.current)
        })
        registerEnabledAware(Visualizer.RadioVisualizer::class.java, Visualizer { prop ->
            RadioButtonOption(RadioButtonOptionData(prop), LocalOptionEnabled.current)
        })
        registerEnabledAware(Visualizer.ColorVisualizer::class.java, Visualizer { prop ->
            ColorOption(ColorOptionData(prop), LocalOptionEnabled.current)
        })
        registerEnabledAware(Visualizer.KeybindVisualizer::class.java, Visualizer { prop ->
            KeybindOption(KeybindOptionData(prop), LocalOptionEnabled.current)
        })
        registerEnabledAware(Visualizer.ButtonVisualizer::class.java, Visualizer { prop ->
            ButtonOption(ButtonOptionData(prop), LocalOptionEnabled.current)
        })
        Visualizer.register(Visualizer.DraggableListVisualizer::class.java, Visualizer { prop ->
            DraggableListOption(DraggableListOptionData(prop))
        })
        registerEnabledAware(Visualizer.MultiSelectDropdownVisualizer::class.java, Visualizer { prop ->
            MultiSelectDropdownOption(MultiSelectDropdownOptionData(prop), LocalOptionEnabled.current)
        })
        Visualizer.register(Visualizer.InfoVisualizer::class.java, Visualizer { prop ->
            InfoOption(InfoOptionData(prop))
        })
    }

    private fun registerEnabledAware(type: Class<out Visualizer>, visualizer: Visualizer) {
        enabledAwareVisualizers += visualizer
        Visualizer.register(type, visualizer)
    }
}
