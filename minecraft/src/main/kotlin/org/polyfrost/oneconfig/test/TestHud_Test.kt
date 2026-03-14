package org.polyfrost.oneconfig.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.polyfrost.compose.composables.PolyBox
import org.polyfrost.compose.composables.PolyMcText
import org.polyfrost.compose.composables.PolyModifier
import org.polyfrost.compose.composables.align
import org.polyfrost.compose.composables.background
import org.polyfrost.compose.composables.padding
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.annotations.Color
import org.polyfrost.oneconfig.api.config.v1.annotations.Slider
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch
import org.polyfrost.oneconfig.api.config.v1.annotations.Text
import org.polyfrost.oneconfig.api.hud.v1.Hud

class TestHud_Test : Hud("test_hud", "Test HUD", Category.INFO) {

    @Switch(title = "Show Background")
    var showBackground = true

    @Color(title = "Background Color")
    var bgColor: Int = 0xAA000000.toInt()

    @Color(title = "Text Color")
    var textColor: Int = 0xFFFFFFFF.toInt()

    @Slider(title = "Scale", min = 0.5f, max = 3f)
    var scale: Float = 1f

    @Text(title = "Label")
    var label: String = "Hello, HUD!"

    private var displayText by mutableStateOf("Hello, HUD!")

    @Composable
    override fun Content() {
        val bg = if (showBackground) PolyColor(bgColor) else PolyColor.TRANSPARENT
        PolyBox(modifier = PolyModifier.background(bg).padding(6f)) {
            PolyMcText(text = displayText, color = textColor, scale = scale)
        }
    }

    override fun update(): Boolean {
        displayText = label
        return true
    }

    override fun setup() {
        super.setup()
        if (isReal) {
            updateWhenChanged("label")
            updateWhenChanged("showBackground")
            updateWhenChanged("bgColor")
            updateWhenChanged("textColor")
            updateWhenChanged("scale")
        }
        update()
    }

    override fun defaultPosition() = 10f to 10f
    override fun minimumSize() = 80f to 24f
}
