package org.polyfrost.oneconfig.internal.ui.themes

object BackdropSample {
    private const val INTERVAL_NANOS = 250_000_000L
    private const val WANTED_NANOS = 1_000_000_000L

    @Volatile
    private var requested = false

    @Volatile
    private var requestedAt = 0L

    @Volatile
    private var sampledAt = 0L

    @Volatile
    var pixels: IntArray = IntArray(0)
        private set

    @Volatile
    var version = 0
        private set

    fun request() {
        requestedAt = System.nanoTime()
        requested = true
    }

    fun due(): Boolean {
        if (!requested) return false
        val now = System.nanoTime()
        if (now - requestedAt > WANTED_NANOS) {
            requested = false
            return false
        }
        if (version != 0 && now - sampledAt < INTERVAL_NANOS) return false
        sampledAt = now
        return true
    }

    fun publish(argb: IntArray) {
        pixels = argb
        version++
    }
}
