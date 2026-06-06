package net.svaroh.passly.feature.transferaccounttoanotherdevice.summary

import net.svaroh.passly.ui.TransferAccountStatusType

class TransferAccountStatusFactory {
    fun create(type: TransferAccountStatusType): TransferAccountStatus =
        when (type) {
            TransferAccountStatusType.SUCCESS -> TransferAccountStatus.Success()
            TransferAccountStatusType.FAILURE -> TransferAccountStatus.Failure()
            TransferAccountStatusType.CANCELED -> TransferAccountStatus.Canceled()
        }
}
