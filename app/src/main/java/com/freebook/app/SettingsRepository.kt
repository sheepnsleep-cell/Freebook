package com.freebook.app

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("freebook_settings", Context.MODE_PRIVATE)

    var adBlockerEnabled: Boolean
        get() = prefs.getBoolean("ad_blocker", true)
        set(value) = prefs.edit().putBoolean("ad_blocker", value).apply()

    var doNotTrack: Boolean
        get() = prefs.getBoolean("do_not_track", true)
        set(value) = prefs.edit().putBoolean("do_not_track", value).apply()

    var privacyInjection: Boolean
        get() = prefs.getBoolean("privacy_injection", true)
        set(value) = prefs.edit().putBoolean("privacy_injection", value).apply()

    var darkMode: Boolean
        get() = prefs.getBoolean("dark_mode", true)
        set(value) = prefs.edit().putBoolean("dark_mode", value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications_enabled", true)
        set(value) = prefs.edit().putBoolean("notifications_enabled", value).apply()

    var notificationInterval: Int
        get() = prefs.getInt("notification_interval", 15)
        set(value) = prefs.edit().putInt("notification_interval", value).apply()

    var lastNotificationCount: Int
        get() = prefs.getInt("last_notification_count", 0)
        set(value) = prefs.edit().putInt("last_notification_count", value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean("is_logged_in", false)
        set(value) = prefs.edit().putBoolean("is_logged_in", value).apply()

    fun logout() {
        prefs.edit().clear().apply()
    }
}
