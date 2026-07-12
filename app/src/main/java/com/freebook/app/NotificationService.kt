package com.freebook.app

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        // Monitor Facebook notifications from the system
        sbn?.let {
            if (it.packageName == "com.facebook.katana" ||
                it.packageName == "com.facebook.orca") {
                // Facebook notification detected
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
