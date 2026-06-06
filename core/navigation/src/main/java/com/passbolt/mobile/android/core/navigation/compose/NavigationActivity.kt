package net.svaroh.passly.core.navigation.compose

import net.svaroh.passly.core.navigation.AccountSetupDataModel
import net.svaroh.passly.core.navigation.AppContext

sealed interface NavigationActivity {
    data class AuthenticationStartUp(
        val appContext: AppContext,
    ) : NavigationActivity

    object AuthenticationSignIn : NavigationActivity

    object AuthenticationManageAccounts : NavigationActivity

    object Home : NavigationActivity

    object Start : NavigationActivity

    object Setup : NavigationActivity

    data class SetupWithPredefinedAccountData(
        val accountSetupDataModel: AccountSetupDataModel?,
    ) : NavigationActivity

    object AutofillReorderToFront : NavigationActivity
}
