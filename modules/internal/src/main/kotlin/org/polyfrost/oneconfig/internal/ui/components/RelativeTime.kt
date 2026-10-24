package org.polyfrost.oneconfig.internal.ui.components



internal fun relativeTime(createdAt: Long, now: Long = System.currentTimeMillis()): String {
    val seconds = ((now - createdAt) / 1000L).coerceAtLeast(0L)
    return when {
        seconds < 5 -> "Just now"
        seconds < 60 -> "${seconds}s ago"
        seconds < 3600 -> "${seconds / 60}m ago"
        seconds < 86400 -> "${seconds / 3600}h ago"
        else -> "${seconds / 86400}d ago"
    }
}
