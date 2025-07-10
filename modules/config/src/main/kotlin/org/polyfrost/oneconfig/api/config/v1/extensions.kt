package org.polyfrost.oneconfig.api.config.v1

inline fun <reified T> Tree.getProp(id: String): Property<T>? {
    return this.getProp(id) as Property<T>?
}