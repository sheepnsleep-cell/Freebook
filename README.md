# Freebook

A privacy-focused, open-source Facebook client for Android. No ads. No trackers. No compromises.

## Features

- **Full Facebook functionality** — News Feed, Messenger, Watch, Marketplace, Notifications, Groups, Pages, Events, Stories
- **Tracker & ad blocking** — Blocks 100+ known tracking domains and Facebook's own tracking pixels
- **Privacy injection** — JavaScript injections to disable fingerprinting, block tracking pixels, and limit data collection
- **Dark mode** — System-wide dark theme override
- **Pull-to-refresh** — Native swipe-to-refresh gesture
- **Bottom navigation** — Quick access to Home, Watch, Marketplace, Notifications, and Menu
- **Background notifications** — Periodic polling for new notifications (configurable interval)
- **Deep linking** — Opens Facebook links from other apps
- **Data saving** — No ad rendering means less bandwidth usage
- **Lightweight** — ~2MB APK vs 100MB+ official app

## Building

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34

### Build APK
```bash
cd Freebook
./gradlew assembleRelease
```

The APK will be at `app/build/outputs/apk/release/app-release.apk`

### Debug Build
```bash
./gradlew assembleDebug
```

## Architecture

```
Freebook/
├── app/src/main/
│   ├── java/com/freebook/app/
│   │   ├── MainActivity.kt          # Main WebView container + navigation
│   │   ├── SettingsActivity.kt      # Privacy settings UI
│   │   ├── FreebookApp.kt           # Application class + notification channels
│   │   ├── PrivacyFilter.kt         # URL blocking + JS injection engine
│   │   ├── SettingsRepository.kt    # SharedPreferences wrapper
│   │   ├── NotificationReceiver.kt  # Background notification polling
│   │   ├── NotificationService.kt   # System notification listener
│   │   └── Extensions.kt            # Utility extensions
│   ├── res/
│   │   ├── layout/                  # Activity layouts
│   │   ├── drawable/                # Vector icons + backgrounds
│   │   ├── values/                  # Strings, colors, themes
│   │   └── xml/                     # Network security config
│   └── AndroidManifest.xml
├── build.gradle.kts
└── settings.gradle.kts
```

## Privacy Details

### Blocked Tracking Domains
- Facebook tracking pixels (`facebook.com/tr`, `pixel.facebook.com`)
- Google Analytics & Ads (`google-analytics.com`, `googletagmanager.com`, `doubleclick.net`)
- Social trackers (Twitter, LinkedIn, Pinterest, Snapchat)
- Data brokers (BlueKai, Oracle, Criteo, Taboola, Outbrain)
- Analytics (Mixpanel, Amplitude, Segment, Hotjar)
- Attribution (Adjust, AppsFlyer, Branch, Kochava)

### Privacy Injections
- Disables `navigator.plugins` fingerprinting
- Overrides `navigator.languages`
- Blocks tracking pixel image loads
- Removes third-party cookies
- Strips tracking attributes from links
- Intercepts popup-based tracking

## Permissions

| Permission | Purpose |
|-----------|---------|
| `INTERNET` | Load Facebook web content |
| `ACCESS_NETWORK_STATE` | Detect connectivity |
| `CAMERA` | Photo posting (passed to WebView) |
| `READ_MEDIA_*` | Photo/video upload (passed to WebView) |
| `RECEIVE_BOOT_COMPLETED` | Restart notification polling |
| `POST_NOTIFICATIONS` | Show new notification alerts |

No location, contacts, phone, or other sensitive permissions are requested.

## License

MIT License — Use freely, modify as needed.
