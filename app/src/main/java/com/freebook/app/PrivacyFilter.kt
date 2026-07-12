package com.freebook.app

object PrivacyFilter {

    private val BLOCKED_DOMAINS = setOf(
        // Facebook tracking
        "facebook.com/tr",
        "facebook.com/tr/",
        "facebook.net/en_US",
        "facebook.net/signals",
        "connect.facebook.net",
        "pixel.facebook.com",
        "analytics.facebook.com",
        // Major ad/tracking networks
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "google-analytics.com",
        "googletagmanager.com",
        "googletagservices.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "stats.g.doubleclick.net",
        // Social trackers
        "platform.twitter.com",
        "syndication.twitter.com",
        "platform.linkedin.com",
        "snap.licdn.com",
        "ads.reddit.com",
        "tr.snapchat.com",
        "ct.pinterest.com",
        "ads.tiktok.com",
        // Data brokers
        "bluekai.com",
        "chartbeat.com",
        "quantserve.com",
        "scorecardresearch.com",
        "omtrdc.net",
        "demdex.net",
        "everesttech.net",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "criteo.com",
        "criteo.net",
        "taboola.com",
        "outbrain.com",
        "taboola.com",
        "moatads.com",
        "oracle.com/ias",
        "bidswitch.net",
        "contextweb.com",
        "adsrvr.org",
        "mathtag.com",
        "turn.com",
        "exelator.com",
        "rlcdn.com",
        "liadm.com",
        "bounceexchange.com",
        "bouncepilot.com",
        "hotjar.com",
        "mouseflow.com",
        "crazyegg.com",
        "luckyorange.com",
        "fullstory.com",
        "mixpanel.com",
        "amplitude.com",
        "segment.com",
        "segment.io",
        "branch.io",
        "adjust.com",
        "appsflyer.com",
        "kochava.com",
        "singular.net",
        "hasoffers.com",
        "mobileapptracking.com",
        "instabug.com",
        "bugsnag.com",
        "sentry.io",
        "instabug.com",
        "branch.io",
        // Additional tracking
        "facebook.com/a/external",
        "facebook.com/restoration/request",
        "facebook.com/sem_pixel",
        "facebook.com/cookie",
        "www.facebook.com/ajax/bz",
        "www.facebook.com/tr/",
        "www.facebook.com/platform/cookie",
        "www.facebook.com/whitelisted_resources",
    )

    private val BLOCKED_URL_PATTERNS = listOf(
        Regex("facebook\\.com/tr(\\?|$)"),
        Regex("facebook\\.com/restoration/"),
        Regex("facebook\\.com/sem_pixel"),
        Regex("facebook\\.com/ajax/bz"),
        Regex("facebook\\.com/platform/cookie"),
        Regex("facebook\\.com/whitelisted_resources"),
        Regex("facebook\\.com/a/external"),
        Regex("connect\\.facebook\\.net"),
        Regex("facebook\\.net/en_US/fbevents"),
        Regex("facebook\\.net/signals"),
        Regex("facebook\\.net/en_US/sdk/"),
        Regex("pixel\\.facebook\\.com"),
        Regex("analytics\\.facebook\\.com"),
        Regex("google-analytics\\.com"),
        Regex("googletagmanager\\.com"),
        Regex("googlesyndication\\.com"),
        Regex("googleadservices\\.com"),
        Regex("doubleclick\\.net"),
        Regex("stats\\.g\\.doubleclick\\.net"),
        Regex("pagead2\\.googlesyndication\\.com"),
        Regex("adservice\\.google\\.com"),
        Regex("ads\\.reddit\\.com"),
        Regex("tr\\.snapchat\\.com"),
        Regex("ct\\.pinterest\\.com"),
        Regex("platform\\.twitter\\.com"),
        Regex("syndication\\.twitter\\.com"),
        Regex("hotjar\\.com"),
        Regex("mixpanel\\.com"),
        Regex("amplitude\\.com"),
        Regex("segment\\.(com|io)"),
        Regex("adjust\\.com"),
        Regex("appsflyer\\.com"),
        Regex("branch\\.io"),
        Regex("chartbeat\\.com"),
        Regex("quantserve\\.com"),
        Regex("scorecardresearch\\.com"),
        Regex("omtrdc\\.net"),
        Regex("demdex\\.net"),
        Regex("everesttech\\.net"),
        Regex("rubiconproject\\.com"),
        Regex("pubmatic\\.com"),
        Regex("openx\\.net"),
        Regex("criteo\\.(com|net)"),
        Regex("taboola\\.com"),
        Regex("outbrain\\.com"),
        Regex("moatads\\.com"),
        Regex("bidswitch\\.net"),
        Regex("adsrvr\\.org"),
        Regex("mathtag\\.com"),
        Regex("turn\\.com"),
        Regex("exelator\\.com"),
        Regex("rlcdn\\.com"),
        Regex("liadm\\.com"),
        Regex("bluekai\\.com"),
    )

    fun shouldBlock(url: String): Boolean {
        val lowerUrl = url.lowercase()
        if (BLOCKED_DOMAINS.any { lowerUrl.contains(it) }) return true
        return BLOCKED_URL_PATTERNS.any { it.containsMatchIn(lowerUrl) }
    }

    fun getPrivacyInjection(): String {
        return """
        (function() {
            'use strict';

            // Block Facebook tracking pixels
            var origImage = Image;
            window.Image = function() {
                var img = new origImage();
                var origSrc = Object.getOwnPropertyDescriptor(origImage.prototype, 'src');
                Object.defineProperty(img, 'src', {
                    set: function(val) {
                        if (val && (val.indexOf('facebook.com/tr') !== -1 ||
                            val.indexOf('facebook.net/tr') !== -1 ||
                            val.indexOf('pixel') !== -1)) {
                            return;
                        }
                        origSrc.set.call(this, val);
                    },
                    get: function() { return origSrc.get.call(this); }
                });
                return img;
            };

            // Override navigator properties to reduce fingerprinting
            Object.defineProperty(navigator, 'plugins', {
                get: function() { return []; }
            });
            Object.defineProperty(navigator, 'languages', {
                get: function() { return ['en-US', 'en']; }
            });

            // Block window.open for tracking popups
            var origOpen = window.open;
            window.open = function(url, name, features) {
                if (url && (url.indexOf('facebook.com/tr') !== -1 ||
                    url.indexOf('doubleclick') !== -1 ||
                    url.indexOf('google-analytics') !== -1)) {
                    return null;
                }
                return origOpen.call(this, url, name, features);
            };

            // Remove tracking attributes from links
            document.addEventListener('click', function(e) {
                var link = e.target.closest('a');
                if (link) {
                    link.removeAttribute('data-testid');
                    link.removeAttribute('data-href');
                }
            }, true);

            // Block third-party cookies
            document.cookie.split(';').forEach(function(c) {
                var parts = c.trim().split('=');
                if (parts[0] && !parts[0].startsWith('c_user') &&
                    !parts[0].startsWith('xs') &&
                    !parts[0].startsWith('fr') &&
                    !parts[0].startsWith('datr') &&
                    !parts[0].startsWith('sb') &&
                    !parts[0].startsWith('locale') &&
                    !parts[0].startsWith('wd') &&
                    !parts[0].startsWith('presence')) {
                    document.cookie = parts[0] + '=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/';
                }
            });

            console.log('[Freebook] Privacy protections active');
        })();
        """
    }

    fun getDarkModeInjection(): String {
        return """
        (function() {
            'use strict';
            // Force dark mode on Facebook
            document.documentElement.setAttribute('data-color-mode', 'dark');
            document.documentElement.classList.add('dark');
            var meta = document.querySelector('meta[name="color-scheme"]');
            if (meta) meta.content = 'dark';
            else {
                meta = document.createElement('meta');
                meta.name = 'color-scheme';
                meta.content = 'dark';
                document.head.appendChild(meta);
            }
        })();
        """
    }
}
