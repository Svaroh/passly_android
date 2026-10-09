package net.svaroh.passly.feature.transferaccounttoanotherdevice.summary

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import java.io.Serializable
import net.svaroh.passly.core.localization.R as LocalizationR
import net.svaroh.passly.core.ui.R as CoreUiR

sealed class TransferAccountStatus(
    @param:DrawableRes val icon: Int,
    @param:StringRes val title: Int,
    @param:StringRes val buttonText: Int,
) : Serializable {
    class Success :
        TransferAccountStatus(
            CoreUiR.drawable.ic_success,
            LocalizationR.string.transfer_account_summary_success,
            LocalizationR.string.transfer_account_summary_go_back,
        )

    class Failure :
        TransferAccountStatus(
            CoreUiR.drawable.ic_failed,
            LocalizationR.string.common_failure,
            LocalizationR.string.transfer_account_summary_go_back,
        )

    class Canceled :
        TransferAccountStatus(
            CoreUiR.drawable.ic_failed,
            LocalizationR.string.transfer_account_summary_cancelled,
            LocalizationR.string.transfer_account_summary_go_back,
        )
}
