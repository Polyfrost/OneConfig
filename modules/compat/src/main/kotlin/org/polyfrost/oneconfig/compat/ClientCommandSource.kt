package org.polyfrost.oneconfig.compat

import net.kyori.adventure.text.Component

interface ClientCommandSource {

    fun sendMessage(text: Component)

}