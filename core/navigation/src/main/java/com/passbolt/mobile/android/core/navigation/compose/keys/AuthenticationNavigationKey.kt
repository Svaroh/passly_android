package net.svaroh.passly.core.navigation.compose.keys

import androidx.navigation3.runtime.NavKey
import net.svaroh.passly.core.navigation.ActivityIntents.AuthConfig
import kotlinx.serialization.Serializable

sealed interface AuthenticationNavigationKey : NavKey {
    @Serializable
    data object AccountsList : AuthenticationNavigationKey

    @Serializable
    data class Auth(
        val userId: String,
        val authConfig: AuthConfig? = null,
    ) : AuthenticationNavigationKey

    @Serializable
    data class MfaTotp(
        val authToken: String?,
        val hasOtherProviders: Boolean,
    ) : AuthenticationNavigationKey

    @Serializable
    data class MfaYubikey(
        val authToken: String?,
        val hasOtherProviders: Boolean,
    ) : AuthenticationNavigationKey

    @Serializable
    data class MfaDuo(
        val authToken: String?,
        val hasOtherProviders: Boolean,
    ) : AuthenticationNavigationKey

    @Serializable
    data object MfaUnknownProvider : AuthenticationNavigationKey
}
