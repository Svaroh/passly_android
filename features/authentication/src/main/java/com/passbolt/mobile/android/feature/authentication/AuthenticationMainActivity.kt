package net.svaroh.passly.feature.authentication

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.IntentCompat
import androidx.fragment.app.FragmentActivity
import net.svaroh.passly.core.navigation.ActivityIntents
import net.svaroh.passly.core.navigation.ActivityIntents.AuthConfig
import net.svaroh.passly.core.navigation.AppContext
import net.svaroh.passly.core.navigation.compose.APP_NAVIGATOR_SCOPE
import net.svaroh.passly.core.navigation.compose.AuthenticationNavigation
import net.svaroh.passly.core.security.flagsecure.FlagSecureSetter
import net.svaroh.passly.core.ui.orientation.LockCompactScreenOrientation
import org.koin.android.ext.android.inject
import org.koin.compose.scope.KoinScope
import org.koin.core.annotation.KoinExperimentalAPI
import java.util.UUID

// NOTE: When changing name or package read core/navigation/README.md
class AuthenticationMainActivity : FragmentActivity() {
    private val flagSecureSetter: FlagSecureSetter by inject()
    private val startUpResolver: AuthenticationStartUpResolver by inject()
    private val authNavigatorScopeId = "auth_navigator_${UUID.randomUUID()}"

    private val authConfig: AuthConfig by lazy {
        requireNotNull(
            IntentCompat.getSerializableExtra(
                intent,
                ActivityIntents.EXTRA_AUTH_CONFIG,
                AuthConfig::class.java,
            ),
        )
    }

    private val appContext: AppContext by lazy {
        requireNotNull(
            IntentCompat.getSerializableExtra(
                intent,
                ActivityIntents.EXTRA_CONTEXT,
                AppContext::class.java,
            ),
        )
    }

    private val userId: String? by lazy {
        intent.getStringExtra(ActivityIntents.EXTRA_USER_ID)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        flagSecureSetter.set(this)
        setResult(RESULT_CANCELED)

        val startUp = startUpResolver.resolve(authConfig, userId)

        setContent {
            LockCompactScreenOrientation()
            @OptIn(KoinExperimentalAPI::class)
            KoinScope(
                scopeID = authNavigatorScopeId,
                scopeQualifier = APP_NAVIGATOR_SCOPE,
            ) {
                AuthenticationNavigation(
                    authConfig = authConfig,
                    appContext = appContext,
                    skipAccountsList = startUp.skipAccountsList,
                    initialUserId = startUp.initialUserId,
                )
            }
        }
    }
}
