package net.svaroh.passly.core.navigation.compose.keys

import android.annotation.SuppressLint
import androidx.navigation3.runtime.NavKey
import net.svaroh.passly.ui.TransferAccountStatusType
import kotlinx.serialization.Serializable

sealed interface TransferAccountToAnotherDeviceKey : NavKey {
    @Serializable
    object Onboarding : TransferAccountToAnotherDeviceKey

    @Serializable
    object Transfer : TransferAccountToAnotherDeviceKey

    @Serializable
    object BrowserFirstLoginScan : TransferAccountToAnotherDeviceKey

    @SuppressLint("UnsafeOptInUsageError") // false positive in K2
    @Serializable
    data class TransferStatus(
        val statusType: TransferAccountStatusType,
    ) : TransferAccountToAnotherDeviceKey
}
