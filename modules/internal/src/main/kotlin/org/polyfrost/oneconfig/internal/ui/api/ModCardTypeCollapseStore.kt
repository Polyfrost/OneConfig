package org.polyfrost.oneconfig.internal.ui.api

import org.polyfrost.oneconfig.api.ui.v1.ModCardTypes

object ModCardTypeCollapseStore : CollapseStore("mod-card-type-collapsed", "mod card types") {
    override fun defaultCollapsed(id: String) = ModCardTypes.byId(id)?.collapsedByDefault ?: false
}
