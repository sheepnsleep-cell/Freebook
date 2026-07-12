package com.freebook.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.webkit.CookieManager
import android.webkit.WebStorage
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {

    private lateinit var settings: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        settings = SettingsRepository(this)

        // Toolbar back button
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        setupAdBlocker()
        setupDoNotTrack()
        setupJavaScript()
        setupDarkMode()
        setupNotifications()
        setupClearData()
        setupLogout()
    }

    private fun setupAdBlocker() {
        val switch = findViewById<SwitchMaterial>(R.id.switchAdBlocker)
        switch.isChecked = settings.adBlockerEnabled
        switch.setOnCheckedChangeListener { _, isChecked ->
            settings.adBlockerEnabled = isChecked
            Toast.makeText(
                this,
                if (isChecked) "Tracker blocking enabled" else "Tracker blocking disabled",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupDoNotTrack() {
        val switch = findViewById<SwitchMaterial>(R.id.switchDoNotTrack)
        switch.isChecked = settings.doNotTrack
        switch.setOnCheckedChangeListener { _, isChecked ->
            settings.doNotTrack = isChecked
        }
    }

    private fun setupJavaScript() {
        val switch = findViewById<SwitchMaterial>(R.id.switchJavaScript)
        switch.isChecked = settings.privacyInjection
        switch.setOnCheckedChangeListener { _, isChecked ->
            settings.privacyInjection = isChecked
        }
    }

    private fun setupDarkMode() {
        val switch = findViewById<SwitchMaterial>(R.id.switchDarkMode)
        switch.isChecked = settings.darkMode
        switch.setOnCheckedChangeListener { _, isChecked ->
            settings.darkMode = isChecked
            Toast.makeText(
                this,
                if (isChecked) "Dark mode will apply on next page load" else "Dark mode disabled",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupNotifications() {
        val switch = findViewById<SwitchMaterial>(R.id.switchNotifications)
        switch.isChecked = settings.notificationsEnabled
        switch.setOnCheckedChangeListener { _, isChecked ->
            settings.notificationsEnabled = isChecked
            if (isChecked) {
                requestNotificationPermission()
                scheduleNotificationCheck()
            } else {
                cancelNotificationCheck()
            }
        }
    }

    private fun setupClearData() {
        findViewById<LinearLayout>(R.id.settingClearData).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear Browsing Data")
                .setMessage("This will clear all cookies, cache, and stored data. You will need to log in again.")
                .setPositiveButton("Clear") { _, _ ->
                    CookieManager.getInstance().removeAllCookies(null)
                    WebStorage.getInstance().deleteAllData(null)
                    val sharedPrefs = getSharedPreferences("freebook_settings", Context.MODE_PRIVATE)
                    sharedPrefs.edit().clear().apply()
                    settings.isLoggedIn = false
                    Toast.makeText(this, "Data cleared", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun setupLogout() {
        findViewById<LinearLayout>(R.id.settingLogout).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out") { _, _ ->
                    settings.logout()
                    CookieManager.getInstance().removeAllCookies(null)
                    WebStorage.getInstance().deleteAllData(null)
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }

    private fun scheduleNotificationCheck() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val intervalMs = settings.notificationInterval * 60 * 1000L
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + intervalMs,
            intervalMs,
            pendingIntent
        )
    }

    private fun cancelNotificationCheck() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
