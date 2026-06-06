package net.svaroh.passly.feature.transferaccounttoanotherdevice.browserfirstlogin

import net.svaroh.passly.ui.TransferAccountStatusType

internal sealed interface BrowserFirstLoginScanSideEffect {
    data object NavigateBack : BrowserFirstLoginScanSideEffect

    data class NavigateToResult(
        val statusType: TransferAccountStatusType,
    ) : BrowserFirstLoginScanSideEffect

    data class ShowErrorSnackbar(
        val message: String,
    ) : BrowserFirstLoginScanSideEffect
}
