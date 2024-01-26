package org.polyfrost.oneconfig.api.hud

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.MustBeInvokedByOverriders
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.impl.Block
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.seconds


/**
 * HUD (Heads Up Display) is a component that is rendered on top of the screen. They are used for displaying information to the user, such as the time, or the player's health.
 *
 * - You need to register your HUD with [HudManager.register] in order for it to be available to the user.
 * - The instance you pass to [HudManager.register] is the instance that is used for the HUD picker screen. When a HUD is added to the screen, a new instance is created using [clone].
 * - HUD config files are stored in `config/oneconfig/huds/{Class.simpleName}-<number>.json`. The number is incremented each time the user adds a new HUD of the same type.
 * - Try not to do really long operations in [update]. The method is called on the render thread and so may cause lag. If you need to do a long operation, consider using an asynchronous task.
 * - The parent of your HUD is a [Block] which is controlled by the user. You do not need to include a background in your HUD.
 * - HUDs which are wider than 450px may have issues when displayed on the HUD picker screen, and HUDs this large are not recommended anyway as they may be very distracting.
 *
 * In this system, multiple of the same HUD can exist at once. In order for this to work correctly, there are a few rules:
 * - Do not run any code inside an `init {}` block. [clone] does not run constructors.
 * - For fields that are mutable references to other objects, you must set them yourself in [clone] in order for each HUD to be independent of each other.
 * - The easiest way to ensure that your HUD works when placed multiple times is to just not use `static` fields, so you don't abuse them.
 */
@Suppress("EqualsOrHashCode")
abstract class Hud<T : Drawable> : Cloneable {
    private var it: T? = null

    /**
     * Return the instance of your HUD element, made by calling [create].
     * @see get
     */
    @Suppress("invisible_member", "invisible_reference")
    @kotlin.internal.InlineOnly
    inline val hud: T
        get() = get()

    /**
     * Hidden flag for this HUD.
     *
     * If `true`, the HUD will not be rendered, and, if this is in a HUD group, it will resize accordingly.
     */
    var hidden: Boolean
        get() = it?.enabled == false
        protected set(new) {
            val value = !new
            // useless null-safety checks, but I don't want to risk dumb errors
            val it = it ?: return
            val parent = it.parent ?: return
            val siblings = parent.children ?: return
            if (value == it.enabled) return

            it.enabled = value
            if (siblings.size == 1) {
                parent.enabled = value
            } else if (!value && siblings.allAre { !it.enabled }) {
                parent.enabled = false
            }
            parent.recalculateChildren()
        }

    /**
     * Return the instance of your HUD element, made by calling [create].
     * @see hud
     */
    fun get() = it ?: create().also { this.it = it }

    /**
     * Create a new instance of your HUD. This should be the complete unit of your hud, **excluding** a background.
     */
    protected abstract fun create(): T

    /**
     * Update your HUD element.
     * Get the instance of your HUD element by calling [get] or [hud].
     * @see updateFrequency
     * @return if you have performed an operation that has changed the size of your HUD element, return `true`
     *         so the system will automatically resize the drawable.
     */
    abstract fun update(): Boolean

    /**
     * Return, in *nanoseconds*, how often the [update] method should be called.
     * Note that small values may be slightly inaccurate. See [PolyUI.every][org.polyfrost.polyui.PolyUI.every] documentation for more information.
     *
     * PolyUI bundles time units for you, such as `0.8.`[seconds] or `50.milliseconds`.
     *
     * Any negative number means [update] will never be called. A value of `0` means that [update] will be called every frame. This is not recommended.
     *
     * This method is called once when the HUD is added to the screen.
     * @see seconds
     */
    abstract fun updateFrequency(): Long

    /**
     * Return the screen resolution you have designed this HUD for.
     *
     * The reason why this method exists is that not everyone has the same screen resolution as you may have,
     * and so the hud may appear by default larger or smaller on their screen.
     *
     * So, this method means that on smaller screens for example, PolyUI will automatically resize the drawable upon startup
     * so that it appears 1:1 to how you designed it.
     *
     * [Vec2]'s Companion has some fields for common screen resolutions. By default, this method returns `1920x1080`.
     *
     * **Marked as experimental because it is currently not used.**
     */
    @ApiStatus.Experimental
    open fun desiredScreenResolution(): Vec2 = Vec2.RES_1080P

    /**
     * Set a custom default background color for this HUD.
     *
     * A value of `null` (default) means that the standard component background color will be used.
     *
     * To remove the background, use [PolyColor.TRANSPARENT].
     */
    open fun backgroundColor(): PolyColor? = null

    /**
     * This method will create a new instance of the HUD. It is key to the functionality of the HUD system.
     *
     * When a HUD is registered, the instance that was passed is the instance that is used for the HUD picker screen.
     *
     * Whenever a HUD is added to the screen, this method is called to create a new instance of the HUD, therefore making them
     * independent of each other.
     *
     * This method is not final as you may need to set some field values manually. This is because the default JVM implementation of
     * [java.lang.Object.clone] performs a **shallow copy** of the object, meaning that any fields that are references to other objects
     * will be copied to the new instance, but the references will be the same. Primitives or immutable values are therefore safe.
     * **Constructors are also not called**. DON'T perform any logic in the constructor of your HUD.
     *
     * Calling this method yourself is not a good idea.
     */
    @MustBeInvokedByOverriders
    @Suppress("unchecked_cast")
    public override fun clone(): Hud<T> = (super.clone() as Hud<T>).apply { it = null }

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is Drawable) {
            return other == get()
        }
        return false
    }
}
