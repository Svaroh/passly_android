package com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.browserfirstlogin

import com.passbolt.mobile.android.ui.TransferAccountStatusType

internal sealed interface BrowserFirstLoginScanSideEffect {
    data object NavigateBack : BrowserFirstLoginScanSideEffect

    data class NavigateToResult(
        val statusType: TransferAccountStatusType,
    ) : BrowserFirstLoginScanSideEffect

    data class ShowErrorSnackbar(
        val message: String,
    ) : BrowserFirstLoginScanSideEffect
}
