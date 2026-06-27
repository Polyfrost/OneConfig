package org.polyfrost.oneconfig.test

import net.minecraft.core.Direction
import org.polyfrost.oneconfig.api.config.v1.KtConfig

object TestKtConfig : KtConfig("test-2", "Test Kt Config", Category.QOL) {

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
}