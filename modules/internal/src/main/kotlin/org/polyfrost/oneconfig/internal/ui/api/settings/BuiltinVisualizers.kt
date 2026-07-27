package org.polyfrost.oneconfig.internal.ui.api.settings

import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.internal.ui.components.settings.BooleanOption
import org.polyfrost.oneconfig.internal.ui.components.settings.ButtonOption
import org.polyfrost.oneconfig.internal.ui.components.settings.ColorListOption
import org.polyfrost.oneconfig.internal.ui.components.settings.ColorOption
import org.polyfrost.oneconfig.internal.ui.components.settings.DraggableListOption
import org.polyfrost.oneconfig.internal.ui.components.settings.DropdownOption
import org.polyfrost.oneconfig.internal.ui.components.settings.FileListOption
import org.polyfrost.oneconfig.internal.ui.components.settings.FileOption
import org.polyfrost.oneconfig.internal.ui.components.settings.InfoOption
import org.polyfrost.oneconfig.internal.ui.components.settings.KeybindOption
import org.polyfrost.oneconfig.internal.ui.components.settings.MultiSelectDropdownOption
import org.polyfrost.oneconfig.internal.ui.components.settings.NumberListOption
import org.polyfrost.oneconfig.internal.ui.components.settings.NumberOption
import org.polyfrost.oneconfig.internal.ui.components.settings.RadioButtonOption
import org.polyfrost.oneconfig.internal.ui.components.settings.SliderOption
import org.polyfrost.oneconfig.internal.ui.components.settings.TextListOption
import org.polyfrost.oneconfig.internal.ui.components.settings.TextOption

internal object BuiltinVisualizers {
    fun register() {
        Visualizer.register(Visualizer.SwitchVisualizer::class.java, Visualizer { prop ->
            BooleanOption(BooleanOptionData(prop, BooleanOptionData.Style.Switch))
        })
        Visualizer.register(Visualizer.CheckboxVisualizer::class.java, Visualizer { prop ->
            BooleanOption(BooleanOptionData(prop, BooleanOptionData.Style.Checkbox))
        })
        Visualizer.register(Visualizer.SliderVisualizer::class.java, Visualizer { prop ->
            SliderOption(SliderOptionData(prop))
        })
        Visualizer.register(Visualizer.NumberVisualizer::class.java, Visualizer { prop ->
            NumberOption(NumberOptionData(prop))
        })
        Visualizer.register(Visualizer.TextVisualizer::class.java, Visualizer { prop ->
            TextOption(TextOptionData(prop))
        })
        Visualizer.register(Visualizer.TextListVisualizer::class.java, Visualizer { prop ->
            TextListOption(TextListOptionData(prop))
        })
        Visualizer.register(Visualizer.FileVisualizer::class.java, Visualizer { prop ->
            FileOption(FileOptionData(prop))
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
        Visualizer.register(Visualizer.DropdownVisualizer::class.java, Visualizer { prop ->
            DropdownOption(DropdownOptionData(prop))
        })
        Visualizer.register(Visualizer.RadioVisualizer::class.java, Visualizer { prop ->
            RadioButtonOption(RadioButtonOptionData(prop))
        })
        Visualizer.register(Visualizer.ColorVisualizer::class.java, Visualizer { prop ->
            ColorOption(ColorOptionData(prop))
        })
        Visualizer.register(Visualizer.KeybindVisualizer::class.java, Visualizer { prop ->
            KeybindOption(KeybindOptionData(prop))
        })
        Visualizer.register(Visualizer.ButtonVisualizer::class.java, Visualizer { prop ->
            ButtonOption(ButtonOptionData(prop))
        })
        Visualizer.register(Visualizer.DraggableListVisualizer::class.java, Visualizer { prop ->
            DraggableListOption(DraggableListOptionData(prop))
        })
        Visualizer.register(Visualizer.MultiSelectDropdownVisualizer::class.java, Visualizer { prop ->
            MultiSelectDropdownOption(MultiSelectDropdownOptionData(prop))
        })
        Visualizer.register(Visualizer.InfoVisualizer::class.java, Visualizer { prop ->
            InfoOption(InfoOptionData(prop))
        })
    }
}
