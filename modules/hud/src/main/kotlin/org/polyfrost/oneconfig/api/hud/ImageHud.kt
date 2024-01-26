package org.polyfrost.oneconfig.api.hud

import org.polyfrost.polyui.component.impl.Image
import org.polyfrost.polyui.utils.image

class ImageHud(val address: String) : Hud<Image>() {

    override fun create() = Image(address.image())

    override fun update() = false

    override fun updateFrequency() = -1L

}
