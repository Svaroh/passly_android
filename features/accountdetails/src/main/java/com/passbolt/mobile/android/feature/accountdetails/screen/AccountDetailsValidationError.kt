package net.svaroh.passly.feature.accountdetails.screen

sealed class AccountDetailsValidationError {
    data class MaxLengthExceeded(
        val maxLength: Int,
    ) : AccountDetailsValidationError()
}
