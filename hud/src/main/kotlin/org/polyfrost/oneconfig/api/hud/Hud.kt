package org.polyfrost.oneconfig.api.hud

import org.jetbrains.annotations.MustBeInvokedByOverriders
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.component.namedId
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.milliseconds
import org.polyfrost.polyui.unit.minutes
import org.polyfrost.polyui.unit.seconds
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

abstract class Hud<T : Drawable> : Cloneable {
    var exists = false
        internal set

    /**
     * Create a new instance of your HUD. This should be the complete unit of your hud, **excluding** a background.
     *
     * **Note that** multiple of the HUD can exist at once.
     */
    abstract fun create(): T

    /**
     * Update your HUD element.
     * @param it an instance of your hud from the [create] method.
     * @see getUpdateFrequency
     * @return if you have performed an operation that has changed the size of your HUD element, return `true`
     *         so the system will automatically resize the drawable.
     */
    abstract fun update(it: T): Boolean

    /**
     * Return in *nanoseconds*, how often the [update] method is called.
     *
     * For time units, PolyUI bundles time units for you, such as `0.8.`[seconds] or `50.milliseconds`.
     *
     * Any negative number means [update] will never be called.
     * @see seconds
     */
    abstract fun getUpdateFrequency(): Long

    /**
     * Return the screen resolution you have designed this HUD for.
     *
     * The reason why this method exists is that not everyone has the same screen resolution as you may have,
     * and so the hud may appear by default larger or smaller on their screen.
     *
     * So, this method means that on smaller screens for example, PolyUI will automatically resize the drawable upon startup
     * so that it appears 1:1 to how you envisioned it.
     *
     * [Vec2]'s Companion has some fields for common screen resolutions. By default, this method returns `1920x1080`.
     */
    open fun desiredScreenResolution(): Vec2 = Vec2.RES_1080P

    @MustBeInvokedByOverriders
    @Suppress("unchecked_cast")
    public override fun clone() = super.clone() as Hud<T>
}

abstract class LegacyHud : Hud<Drawable>() {
    abstract var width: Float
    abstract var height: Float

    final override fun create(): Drawable {
        val size = object : Vec2(width, height) {
            override var x: Float
                get() = this@LegacyHud.width
                set(value) {
                    this@LegacyHud.width = value
                }
            override var y: Float
                get() = this@LegacyHud.height
                set(value) {
                    this@LegacyHud.height = value
                }
        }

        return object : Drawable(size = size) {
            override fun preRender() {}

            override fun render() {
                render(x, y)
            }

            override fun postRender() {}

            override fun setup(polyUI: PolyUI): Boolean {
                if (initialized) return false
                initialized = true
                return true
            }
        }.namedId("LegacyHud")
    }

    abstract fun render(x: Float, y: Float)
}


/**
 * Date and/or time HUD.
 * @param template the template to use for the time. See [DateTimeFormatter] for an explanation of the different keywords.
 */
class DateTimeHud(header: String, template: String, suffix: String = "") : TextHud(
    header,
    suffix,
    frequency =
    if (template.contains('S')) 100.milliseconds
    else if (template.contains('s')) 1.seconds
    else if (template.contains('m')) 1.minutes
    else 5.minutes // asm: updating every 5 minutes is reasonable
) {
    private var template = DateTimeFormatter.ofPattern(template)

    override fun get() = LocalDateTime.now().format(template)
}

class SupplierTextHud(prefix: String, suffix: String = "", frequency: Long, private val supplier: () -> String) : TextHud(prefix, suffix, frequency) {
    override fun get() = supplier()
}

abstract class TextHud(var prefix: String, var suffix: String = "", private val frequency: Long) : Hud<Text>() {
    override fun create() = Text("$prefix${get()}$suffix", fontSize = 16f, font = PolyUI.defaultFonts.medium)

    override fun getUpdateFrequency() = frequency

    override fun update(it: Text): Boolean {
        it.text = "$prefix${get()}$suffix"
        return true
    }

    protected abstract fun get(): String
}