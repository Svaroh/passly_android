package net.svaroh.passly.core.navigation.compose.keys

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import net.svaroh.passly.ui.ResultStatus

sealed interface SetupNavigationKey : NavKey {
    @Serializable
    object AccessibilityPolicies : SetupNavigationKey

    @Serializable
    object Welcome : SetupNavigationKey

    @Serializable
    object TransferDetails : SetupNavigationKey

    @Serializable
    object ScanQrCodes : SetupNavigationKey

    @Serializable
    object ImportProfile : SetupNavigationKey

    @Serializable
    object BiometricSetup : SetupNavigationKey

    @Serializable
    data class Summary(
        val status: ResultStatus,
    ) : SetupNavigationKey
}
