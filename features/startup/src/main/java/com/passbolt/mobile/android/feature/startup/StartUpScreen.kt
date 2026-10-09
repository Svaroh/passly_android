package net.svaroh.passly.feature.startup

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.svaroh.passly.core.compose.PassboltTheme
import net.svaroh.passly.core.compose.SideEffectDispatcher
import net.svaroh.passly.core.navigation.AppContext
import net.svaroh.passly.core.navigation.compose.AppNavigator
import net.svaroh.passly.core.navigation.compose.NavigationActivity.AuthenticationStartUp
import net.svaroh.passly.core.navigation.compose.NavigationActivity.SetupWithPredefinedAccountData
import net.svaroh.passly.feature.startup.StartUpIntent.AcknowledgeDeprecatedOsWarning
import net.svaroh.passly.feature.startup.StartUpIntent.HideDeprecatedOsWarning
import net.svaroh.passly.feature.startup.StartUpSideEffect.NavigateToSetup
import net.svaroh.passly.feature.startup.StartUpSideEffect.NavigateToSignIn
import net.svaroh.passly.feature.startup.deprecatedoswarning.DeprecatedOsWarningBottomSheet
import net.svaroh.passly.ui.AccountSetupDataModel
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
    val state by viewModel.viewState.collectAsStateWithLifecycle()

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

    if (state.showDeprecatedOsWarning) {
        PassboltTheme {
            DeprecatedOsWarningBottomSheet(
                onAcknowledge = { viewModel.onIntent(AcknowledgeDeprecatedOsWarning) },
                onHide = { viewModel.onIntent(HideDeprecatedOsWarning) },
            )
        }
    }
}
