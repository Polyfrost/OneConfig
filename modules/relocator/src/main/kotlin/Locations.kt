package org.polyfrost.oneconfig.relocator

import org.polyfrost.oneconfig.relocator.annotations.MoulConfig
import kotlin.reflect.KClass

internal object Locations {
    val relocations = mapOf<KClass<out Annotation>, Location>(
        MoulConfig::class to Location(
            "io.github.notenoughupdates.moulconfig",
            TargetLocation("skyhanni", "at.hannibal2.skyhanni.deps.moulconfig"),
            TargetLocation("dandelion","net.azureaaron.dandelion.deps.moulconfig"),
            //TargetLocation("firmament", "io.github.notenoughupdates.moulconfig")
        )
    )


    data class Location(
        val sourcePackage: String,
        val targets: List<TargetLocation>,
    ) {
        constructor(sourcePackage: String, vararg targets: TargetLocation) : this(sourcePackage, targets.toList())
    }

    data class TargetLocation(
        val modName: String,
        val targetPackage: String,
    )
}