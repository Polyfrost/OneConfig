package org.polyfrost.oneconfig.internal.ui.api

interface ProfileData {
    val name: String
    val icon: String?

    var liked: Boolean
    var selected: Boolean
}