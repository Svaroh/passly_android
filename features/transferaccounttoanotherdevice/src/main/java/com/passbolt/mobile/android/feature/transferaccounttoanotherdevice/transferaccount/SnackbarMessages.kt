package net.svaroh.passly.feature.transferaccounttoanotherdevice.transferaccount

import android.content.Context
import net.svaroh.passly.core.localization.R
import net.svaroh.passly.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountScreenSideEffect.ErrorSnackbarType.FAILED_TO_CREATE_TRANSFER
import net.svaroh.passly.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountScreenSideEffect.ErrorSnackbarType.FAILED_TO_FETCH_TRANSFER_DETAILS
import net.svaroh.passly.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountScreenSideEffect.ErrorSnackbarType.FAILED_TO_GENERATE_QR_DATA
import net.svaroh.passly.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountScreenSideEffect.ErrorSnackbarType.FAILED_TO_INITIALIZE_PARAMETERS
import net.svaroh.passly.feature.transferaccounttoanotherdevice.transferaccount.TransferAccountScreenSideEffect.ShowErrorSnackbar

internal fun getSnackbarMessage(
    context: Context,
    snackbar: ShowErrorSnackbar,
): String =
    when (snackbar.type) {
        FAILED_TO_INITIALIZE_PARAMETERS -> context.getString(R.string.transfer_account_could_not_initialize_parameters)
        FAILED_TO_CREATE_TRANSFER ->
            context.getString(
                R.string.transfer_account_could_not_create_transfer_format,
                snackbar.errorMessage.orEmpty(),
            )
        FAILED_TO_GENERATE_QR_DATA -> context.getString(R.string.transfer_account_could_not_initialize_qr_code_page_data)
        FAILED_TO_FETCH_TRANSFER_DETAILS ->
            context.getString(
                R.string.transfer_account_error_during_fetch_transfer_format,
                snackbar.errorMessage.orEmpty(),
            )
    }
