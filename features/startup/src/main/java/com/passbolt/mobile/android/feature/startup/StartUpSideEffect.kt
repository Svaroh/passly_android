package net.svaroh.passly.feature.startup

import net.svaroh.passly.core.navigation.AccountSetupDataModel

sealed class StartUpSideEffect {
    data class NavigateToSetup(
        val accountSetupDataModel: AccountSetupDataModel?,
    ) : StartUpSideEffect()

    data object NavigateToSignIn : StartUpSideEffect()
}
