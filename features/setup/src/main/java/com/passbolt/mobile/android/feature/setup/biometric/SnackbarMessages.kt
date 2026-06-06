package net.svaroh.passly.feature.setup.biometric

import net.svaroh.passly.core.localization.R
import net.svaroh.passly.feature.setup.biometric.SnackbarErrorType.AUTHENTICATION_GENERIC
import net.svaroh.passly.feature.setup.biometric.SnackbarErrorType.AUTHENTICATION_LOCKOUT
import net.svaroh.passly.feature.setup.biometric.SnackbarErrorType.AUTHENTICATION_LOCKOUT_PERMANENT
import net.svaroh.passly.feature.setup.biometric.SnackbarErrorType.BIOMETRIC_ENCRYPT_ERROR
import net.svaroh.passly.feature.setup.biometric.SnackbarErrorType.BIOMETRIC_NO_CRYPTO_CIPHER
import net.svaroh.passly.feature.setup.biometric.SnackbarErrorType.GENERIC_ERROR

internal fun getSnackbarMessage(
    errorType: SnackbarErrorType,
    environment: BiometricSetupEnvironment,
): String =
    when (errorType) {
        GENERIC_ERROR -> environment.context.getString(R.string.common_failure)
        AUTHENTICATION_LOCKOUT -> environment.context.getString(R.string.biometric_error_blocked)
        AUTHENTICATION_LOCKOUT_PERMANENT -> environment.context.getString(R.string.biometric_error_too_many_attempts)
        AUTHENTICATION_GENERIC -> environment.context.getString(R.string.biometric_error_generic)
        BIOMETRIC_ENCRYPT_ERROR -> environment.context.getString(R.string.biometric_encrypt_error_message)
        BIOMETRIC_NO_CRYPTO_CIPHER -> environment.context.getString(R.string.biometric_no_crypto_cipher)
    }
