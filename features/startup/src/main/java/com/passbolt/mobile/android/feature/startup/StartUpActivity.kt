package net.svaroh.passly.feature.startup

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import net.svaroh.passly.core.navigation.deeplink.BrowserFirstLoginDeepLinkStore
import org.koin.android.ext.android.inject

// NOTE: When changing name or package read core/navigation/README.md
class StartUpActivity : AppCompatActivity() {
    private val accountSetupModelCreator: AccountSetupModelCreator by inject()
    private val browserFirstLoginDeepLinkStore: BrowserFirstLoginDeepLinkStore by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        saveBrowserFirstLoginDeepLink()
        setContent {
            StartUpScreen(
                accountSetupDataModel = accountSetupModelCreator.createFromIntent(intent),
            )
        }
    }

    private fun saveBrowserFirstLoginDeepLink() {
        intent.dataString
            ?.takeIf { deepLink -> BROWSER_FIRST_LOGIN_DEEP_LINK_PREFIXES.any(deepLink::startsWith) }
            ?.let(browserFirstLoginDeepLinkStore::save)
    }

    private companion object {
        private val BROWSER_FIRST_LOGIN_DEEP_LINK_PREFIXES =
            listOf(
                "passbolt://browser-first-login",
                "intent://browser-first-login",
            )
    }
}
