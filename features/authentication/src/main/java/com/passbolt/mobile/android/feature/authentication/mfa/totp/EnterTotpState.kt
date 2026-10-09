package net.svaroh.passly.feature.authentication.mfa.totp

data class EnterTotpState(
    val showProgress: Boolean = false,
    val hasOtherProvider: Boolean = false,
    val rememberMe: Boolean = true,
    val otpTextColor: OtpTextColor = OtpTextColor.DEFAULT,
    val showSetupLeaveConfirmationDialog: Boolean = false,
) {
    enum class OtpTextColor { DEFAULT, ERROR }
}
