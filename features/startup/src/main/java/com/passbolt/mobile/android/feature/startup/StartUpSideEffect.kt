package net.svaroh.passly.feature.startup

import net.svaroh.passly.ui.AccountSetupDataModel

sealed class StartUpSideEffect {
    data class NavigateToSetup(
        val accountSetupDataModel: AccountSetupDataModel?,
    ) : StartUpSideEffect()

    data object NavigateToSignIn : StartUpSideEffect()
}
