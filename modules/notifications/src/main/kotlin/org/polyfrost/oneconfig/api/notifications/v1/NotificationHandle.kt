package org.polyfrost.oneconfig.api.notifications.v1

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Experimental
public interface NotificationHandle {
    public fun update(title: String? = null, message: String? = null, type: NotificationType? = null, progress: Float? = null)

    public fun dismiss()
}
