package org.polyfrost.oneconfig.api.hud

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.unit.milliseconds
import org.polyfrost.polyui.unit.minutes
import org.polyfrost.polyui.unit.seconds
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Basic HUD element which displays text.
 * @see TextHud.Supplier
 * @see TextHud.DateTime
 */
abstract class TextHud(var prefix: String, var suffix: String = "", private val frequency: Long) : Hud<Text>() {
    override fun create() = Text("$prefix${getText()}$suffix", fontSize = 16f, font = PolyUI.defaultFonts.medium)

    override fun updateFrequency() = frequency

    override fun update(): Boolean {
        get().text = "$prefix${getText()}$suffix"
        return true
    }

    protected abstract fun getText(): String


    /**
     * [TextHud] which uses the given [supplier] to get the text.
     */
    class Supplier(prefix: String, suffix: String = "", frequency: Long, private val supplier: () -> String) : TextHud(prefix, suffix, frequency) {
        constructor(prefix: String, suffix: String = "", frequency: Long, supplier: java.util.function.Supplier<String>) : this(prefix, suffix, frequency, supplier::get)

        override fun getText() = supplier()
    }

    /**
     * [TextHud] which displays the date/time information.
     * @param template the template to use for the time. See [DateTimeFormatter] for an explanation of the different keywords.
     */
    class DateTime(header: String, template: String, suffix: String = "") : TextHud(
        header,
        suffix,
        frequency =
        if (template.contains('S')) 100.milliseconds
        else if (template.contains('s')) 1.seconds
        else if (template.contains('m')) 1.minutes
        else 5.minutes // asm: updating every 5 minutes is reasonable
    ) {
        var string = template
            set(value) {
                field = value
                template = DateTimeFormatter.ofPattern(value)
            }

        private var template = DateTimeFormatter.ofPattern(template)

        override fun getText() = LocalDateTime.now().format(template)
    }
}

