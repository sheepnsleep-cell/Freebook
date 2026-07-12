package com.freebook.app

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.freebook.app.FreebookApp.Companion.NOTIFICATION_CHANNEL_ID

class MainActivity : AppCompatActivity() {

    companion object {
        private const val FACEBOOK_URL = "https://m.facebook.com"
        private const val FACEBOOK_HOME = "https://m.facebook.com/home.php"
        private const val FACEBOOK_WATCH = "https://m.facebook.com/watch"
        private const val FACEBOOK_MARKETPLACE = "https://m.facebook.com/marketplace"
        private const val FACEBOOK_NOTIFICATIONS = "https://m.facebook.com/notifications"
        private const val FACEBOOK_MENU = "https://m.facebook.com/menu"
        private const val FACEBOOK_SEARCH = "https://m.facebook.com/search"
        private const val FACEBOOK_MESSENGER = "https://m.facebook.com/messages"
        private const val NOTIFICATION_CHECK_REQUEST = 1001
    }

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var settings: SettingsRepository
    private lateinit var handler: Handler

    private var currentUrl = FACEBOOK_HOME
    private var isNavigating = false
    private var notificationCount = 0

    // Bottom nav items
    private lateinit var navHome: LinearLayout
    private lateinit var navWatch: LinearLayout
    private lateinit var navMarketplace: LinearLayout
    private lateinit var navNotifications: LinearLayout
    private lateinit var navMenu: LinearLayout
    private lateinit var navHomeIcon: ImageView
    private lateinit var navWatchIcon: ImageView
    private lateinit var navMarketplaceIcon: ImageView
    private lateinit var navNotificationsIcon: ImageView
    private lateinit var navMenuIcon: ImageView
    private lateinit var notificationBadge: TextView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settings = SettingsRepository(this)
        handler = Handler(Looper.getMainLooper())

        initViews()
        setupWebView()
        setupBottomNav()
        setupTopBar()
        setupSwipeRefresh()

        // Handle intent (deep links)
        handleIntent(intent)

        // Load Facebook
        webView.loadUrl(FACEBOOK_HOME)

        // Start notification checker
        if (settings.notificationsEnabled) {
            scheduleNotificationCheck()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                val data = intent.data
                if (data != null) {
                    currentUrl = data.toString()
                }
            }
        }
    }

    private fun initViews() {
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        swipeRefresh = findViewById(R.id.swipeRefresh)

        navHome = findViewById(R.id.navHome)
        navWatch = findViewById(R.id.navWatch)
        navMarketplace = findViewById(R.id.navMarketplace)
        navNotifications = findViewById(R.id.navNotifications)
        navMenu = findViewById(R.id.navMenu)
        navHomeIcon = findViewById(R.id.navHomeIcon)
        navWatchIcon = findViewById(R.id.navWatchIcon)
        navMarketplaceIcon = findViewById(R.id.navMarketplaceIcon)
        navNotificationsIcon = findViewById(R.id.navNotificationsIcon)
        navMenuIcon = findViewById(R.id.navMenuIcon)
        notificationBadge = findViewById(R.id.notificationBadge)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT

            // Privacy settings
            setSupportMultipleWindows(false)
            userAgentString = settings.userAgent()

            // Media
            mediaPlaybackRequiresUserGesture = false
        }

        // Cookie manager - clear third-party cookies
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, false)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                isNavigating = true

                // Block tracking URLs
                if (settings.adBlockerEnabled && PrivacyFilter.shouldBlock(url)) {
                    return true // Block the request
                }

                // Keep navigation within the app
                if (url.contains("facebook.com") || url.contains("fb.com") ||
                    url.contains("fbcdn.net")) {
                    return false // Let WebView handle it
                }

                // External links open in browser
                if (url.startsWith("http")) {
                    val intent = Intent(Intent.ACTION_VIEW, request?.url)
                    startActivity(intent)
                    return true
                }
                return false
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url?.toString() ?: return null

                // Block tracking/ad requests at network level
                if (settings.adBlockerEnabled && PrivacyFilter.shouldBlock(url)) {
                    return WebResourceResponse(
                        "text/plain",
                        "UTF-8",
                        "".byteInputStream()
                    )
                }
                return null
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
                isNavigating = true
                updateNavigationState(url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                isNavigating = false
                swipeRefresh.isRefreshing = false
                currentUrl = url ?: FACEBOOK_HOME

                // Inject privacy protections
                view?.let { injectPrivacyScripts(it) }

                // Check for notification count
                view?.evaluateJavascript("""
                    (function() {
                        var badge = document.querySelector('[data-testid="notifications-badge"]');
                        if (badge) return badge.textContent || '0';
                        var badge2 = document.querySelector('.notif-badge');
                        if (badge2) return badge2.textContent || '0';
                        return '0';
                    })();
                """.trimIndent()) { result ->
                    try {
                        val count = result.trim('"', ' ').toIntOrNull() ?: 0
                        if (count > 0) {
                            showNotificationBadge(count)
                        }
                    } catch (_: Exception) {}
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    swipeRefresh.isRefreshing = false
                    Toast.makeText(
                        this@MainActivity,
                        "Connection error. Pull to refresh.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                handler?.cancel()
                Toast.makeText(
                    this@MainActivity,
                    "SSL error. Connection blocked for security.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                if (newProgress == 100) {
                    progressBar.visibility = View.GONE
                }
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                // Handle file upload (photo posting etc.)
                return true
            }
        }

        // Enable mixed content handling for images
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                if (newProgress == 100) {
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun injectPrivacyScripts(webView: WebView) {
        if (settings.privacyInjection) {
            webView.evaluateJavascript(PrivacyFilter.getPrivacyInjection(), null)
        }
        if (settings.darkMode) {
            webView.evaluateJavascript(PrivacyFilter.getDarkModeInjection(), null)
        }
    }

    private fun setupBottomNav() {
        val navItems = listOf(
            Triple(navHome, navHomeIcon, FACEBOOK_HOME),
            Triple(navWatch, navWatchIcon, FACEBOOK_WATCH),
            Triple(navMarketplace, navMarketplaceIcon, FACEBOOK_MARKETPLACE),
            Triple(navNotifications, navNotificationsIcon, FACEBOOK_NOTIFICATIONS),
            Triple(navMenu, navMenuIcon, FACEBOOK_MENU)
        )

        navItems.forEach { (layout, icon, url) ->
            layout.setOnClickListener {
                updateActiveNav(icon)
                if (webView.url != url) {
                    webView.loadUrl(url)
                }
            }
        }
    }

    private fun updateActiveNav(activeIcon: ImageView) {
        val icons = listOf(navHomeIcon, navWatchIcon, navMarketplaceIcon, navNotificationsIcon, navMenuIcon)
        icons.forEach { it.setColorFilter(ContextCompat.getColor(this, R.color.nav_icon_inactive_dark)) }
        activeIcon.setColorFilter(ContextCompat.getColor(this, R.color.nav_icon_active))
    }

    private fun updateNavigationState(url: String?) {
        when {
            url?.contains("watch") == true -> updateActiveNav(navWatchIcon)
            url?.contains("marketplace") == true -> updateActiveNav(navMarketplaceIcon)
            url?.contains("notifications") == true -> updateActiveNav(navNotificationsIcon)
            url?.contains("menu") == true -> updateActiveNav(navMenuIcon)
            else -> updateActiveNav(navHomeIcon)
        }
    }

    private fun setupTopBar() {
        findViewById<ImageButton>(R.id.btnSearch).setOnClickListener {
            webView.loadUrl(FACEBOOK_SEARCH)
        }
        findViewById<ImageButton>(R.id.btnMessenger).setOnClickListener {
            webView.loadUrl(FACEBOOK_MESSENGER)
        }
    }

    private fun setupSwipeRefresh() {
        // Use the main swipeRefresh (re-parent if needed)
        val swipe = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipe.setColorSchemeColors(ContextCompat.getColor(this, R.color.primary))
        swipe.setOnRefreshListener {
            webView.reload()
        }
    }

    private fun showNotificationBadge(count: Int) {
        if (count > 0) {
            notificationBadge.visibility = View.VISIBLE
            notificationBadge.text = if (count > 99) "99+" else count.toString()
        } else {
            notificationBadge.visibility = View.GONE
        }
    }

    private fun scheduleNotificationCheck() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            NOTIFICATION_CHECK_REQUEST,
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

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            // Double-tap to exit
            AlertDialog.Builder(this)
                .setTitle("Exit Freebook?")
                .setPositiveButton("Exit") { _, _ -> finishAffinity() }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        if (settings.notificationsEnabled) {
            scheduleNotificationCheck()
        }
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }
}
