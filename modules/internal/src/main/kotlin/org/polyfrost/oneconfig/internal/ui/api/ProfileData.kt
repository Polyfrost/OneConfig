package org.polyfrost.oneconfig.internal.ui.api

interface ProfileData {
    val name: String
    val icon: String?

    /** UI expects this to be backed by a MutableState */
    var liked: Boolean
    /** UI expects this to be backed by a MutableState */
    var selected: Boolean
}