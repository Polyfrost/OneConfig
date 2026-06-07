package org.polyfrost.oneconfig.relocator

import org.polyfrost.oneconfig.relocator.annotations.MoulConfig
import kotlin.reflect.KClass

internal object Locations {
    val relocations = mapOf<KClass<out Annotation>, Location>(
        MoulConfig::class to Location(
            "io.github.notenoughupdates.moulconfig",
            TargetLocation("skyhanni", "at.hannibal2.skyhanni.deps.moulconfig", Exact("1.8.9"), Newer("1.21.4")),
            TargetLocation("dandelion_bp", "net.azureaaron.dandelion_bp.deps.moulconfig", Newer("1.21.5")),
            TargetLocation("firmament", "moe.nea.firmament.deps.moulconfig", Newer("1.21.5")),
        )
    )

    data class Exact(val value: Int) : Condition {
        constructor(value: String) : this(convertStringToIntVersion(value))

        override fun test(other: Int): Boolean = value == other
    }

    data class Older(val value: Int, val inclusive: Boolean = false) : Condition {
        constructor(value: String, inclusive: Boolean = false) : this(convertStringToIntVersion(value), inclusive)

        override fun test(other: Int): Boolean = if (inclusive) other <= value else other < value
    }

    data class Newer(val value: Int, val inclusive: Boolean = true) : Condition {
        constructor(value: String, inclusive: Boolean = true) : this(convertStringToIntVersion(value), inclusive)

        override fun test(other: Int): Boolean = if (inclusive) other >= value else other > value
    }

    interface Condition {
        fun test(other: Int): Boolean
    }

    data class Location(
        val sourcePackage: String,
        val targets: List<TargetLocation>,
    ) {
        constructor(sourcePackage: String, vararg targets: TargetLocation) : this(sourcePackage, targets.toList())
    }

    data class TargetLocation(
        val modName: String,
        val targetPackage: String,
        val conditions: List<Condition>,
    ) {
        constructor(modName: String, targetPackage: String, vararg conditions: Condition) : this(
            modName,
            targetPackage,
            conditions.toList()
        )

        fun test(version: String?): Boolean {
            if (version == null) return true
            val version = convertStringToIntVersion(version)
            return conditions.any { it.test(version) }
        }
    }
}