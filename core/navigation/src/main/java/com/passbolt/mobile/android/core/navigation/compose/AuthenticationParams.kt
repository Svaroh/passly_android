package net.svaroh.passly.core.navigation.compose

import androidx.compose.runtime.compositionLocalOf
import net.svaroh.passly.core.navigation.ActivityIntents
import net.svaroh.passly.core.navigation.AppContext

data class AuthenticationParams(
    val authConfig: ActivityIntents.AuthConfig,
    val appContext: AppContext,
)

val LocalAuthenticationParams =
    compositionLocalOf<AuthenticationParams> { error("No AuthenticationParams provided") }
