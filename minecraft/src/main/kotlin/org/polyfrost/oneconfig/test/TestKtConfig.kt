package org.polyfrost.oneconfig.test

import net.minecraft.core.Direction
import org.polyfrost.oneconfig.api.config.v1.KtConfig

object TestKtConfig : KtConfig("test-2", "Test Kt Config", Category.QOL) {

    var enabled by masterToggle()

    var test by transformed(
        radiobutton(
            "test",
            Direction.EAST,
            Direction.entries.toTypedArray()
        ) { "$it.mrow" }.onChange {
            println("Enum $it")
        },
        Enum<*>::ordinal,
        Direction.entries::get
    ).onChange {
        println("Transformed to ordinal $it")
    }

    val audio = accordion("audio", title = "Audio", description = "Accordion built from KtConfig delegates", subcategory = "Accordions")
    var audioEnabled by switch("Enabled", def = true).primary().inside(audio)
    var audioVolume by slider("Volume", def = 50f, min = 0f, max = 100f).inside(audio)
    var audioAdvanced by switch("Show advanced", def = false).inside(audio)
    var audioPitch by slider("Pitch", def = 1f, min = 0.5f, max = 2f, step = 0.1f).inside(audio)
    val audioTest by button("Test", text = "Play") { println("Audio test at volume $audioVolume") }.inside(audio)

    val extras = submenu("extras", title = "Extras", subcategory = "Accordions")
    val extrasSound = accordion("sound", title = "Sound", parent = extras)
    var extrasSoundEnabled by switch("Enabled", def = false).inside(extrasSound).primary()
    var extrasSoundName by text("Sound", def = "minecraft:block.note_block.pling").inside(extrasSound)

    init {
        hideIf(::audioPitch) { !audioAdvanced }
    }
}