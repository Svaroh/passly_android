package net.svaroh.passly.feature.authentication.mfa.duo

import net.svaroh.passly.feature.authentication.mfa.duo.duowebviewsheet.DuoState

sealed interface AuthWithDuoIntent {
    data object AuthenticateWithDuo : AuthWithDuoIntent

    data object ChooseOtherProvider : AuthWithDuoIntent

    data object Close : AuthWithDuoIntent

    data class DuoAuthFinished(
        val state: DuoState,
    ) : AuthWithDuoIntent

    data object DismissDuoAuth : AuthWithDuoIntent
}
