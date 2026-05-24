package com.passbolt.mobile.android.core.navigation.deeplink

class BrowserFirstLoginDeepLinkStore {
    private var pendingDeepLink: String? = null

    fun save(deepLink: String) {
        pendingDeepLink = deepLink
    }

    fun consume(): String? =
        pendingDeepLink.also {
            pendingDeepLink = null
        }
}
