package net.svaroh.passly.feature.authentication.mfa

import net.svaroh.passly.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Mfa.MfaProvider

sealed interface MfaResult {
    data class Succeeded(
        val mfaHeader: String?,
    ) : MfaResult

    data class OtherProvider(
        val bearer: String?,
        val currentProvider: MfaProvider,
    ) : MfaResult
}
