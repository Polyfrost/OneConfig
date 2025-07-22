package org.polyfrost.oneconfig.relocator.annotations

/**
 * Denotes that the annotated object contains moulconfig imports and should be relocated/duplicated for all known moulconfig locations.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class MoulConfig()
