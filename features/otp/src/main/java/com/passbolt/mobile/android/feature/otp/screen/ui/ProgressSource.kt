package net.svaroh.passly.feature.otp.screen.ui

internal sealed interface ProgressSource {
    val remainingSeconds: Long
    val expirySeconds: Long

    data class RevealedOtp(
        override val remainingSeconds: Long,
        override val expirySeconds: Long,
    ) : ProgressSource

    data class UniversalAutofillCountdown(
        override val remainingSeconds: Long,
        override val expirySeconds: Long,
    ) : ProgressSource
}
