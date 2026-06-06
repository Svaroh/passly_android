package net.svaroh.passly.feature.startup

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import net.svaroh.passly.core.compose.SideEffectDispatcher
import net.svaroh.passly.core.navigation.AccountSetupDataModel
import net.svaroh.passly.core.navigation.AppContext
import net.svaroh.passly.core.navigation.compose.AppNavigator
import net.svaroh.passly.core.navigation.compose.NavigationActivity.AuthenticationStartUp
import net.svaroh.passly.core.navigation.compose.NavigationActivity.SetupWithPredefinedAccountData
import net.svaroh.passly.feature.startup.StartUpSideEffect.NavigateToSetup
import net.svaroh.passly.feature.startup.StartUpSideEffect.NavigateToSignIn
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun StartUpScreen(
    accountSetupDataModel: AccountSetupDataModel?,
    viewModel: StartUpViewModel =
        koinViewModel(parameters = { parametersOf(accountSetupDataModel) }),
    navigator: AppNavigator = koinInject(),
) {
    val context = LocalContext.current
    val activity = LocalActivity.current

    SideEffectDispatcher(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is NavigateToSetup -> {
                navigator.startNavigationActivity(context, SetupWithPredefinedAccountData(sideEffect.accountSetupDataModel))
                navigator.finishActivity(activity)
            }
            NavigateToSignIn -> {
                navigator.startNavigationActivity(context, AuthenticationStartUp(AppContext.APP))
                navigator.finishActivity(activity)
            }
        }
    }
}
