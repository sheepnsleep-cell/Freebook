package com.freebook.app

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.freebook.app.FreebookApp.Companion.NOTIFICATION_CHANNEL_ID

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val settings = SettingsRepository(context)
        if (!settings.notificationsEnabled) return

        // Check for new notifications by parsing the notifications page
        Thread {
            try {
                val url = java.net.URL("https://m.facebook.com/notifications")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.setRequestProperty("User-Agent", settings.userAgent())
                connection.connect()

                val response = connection.inputStream.bufferedReader().readText()
                connection.disconnect()

                // Simple count of notification items
                val count = Regex("""notif_item""").findAll(response).count()

                if (count > settings.lastNotificationCount && count > 0) {
                    val newCount = count - settings.lastNotificationCount
                    settings.lastNotificationCount = count
                    showNotification(context, newCount)
                } else {
                    settings.lastNotificationCount = count
                }
            } catch (_: Exception) {
                // Network error, skip this cycle
            }
        }.start()
    }

    private fun showNotification(context: Context, count: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_notifications", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Freebook")
            .setContentText("You have $count new notification${if (count > 1) "s" else ""}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setNumber(count)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1, notification)
    }
}
