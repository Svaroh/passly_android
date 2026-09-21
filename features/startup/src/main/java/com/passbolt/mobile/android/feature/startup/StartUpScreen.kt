package com.passbolt.mobile.android.feature.startup

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.PassboltTheme
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.AuthenticationStartUp
import com.passbolt.mobile.android.core.navigation.compose.NavigationActivity.SetupWithPredefinedAccountData
import com.passbolt.mobile.android.feature.startup.StartUpIntent.AcknowledgeDeprecatedOsWarning
import com.passbolt.mobile.android.feature.startup.StartUpIntent.HideDeprecatedOsWarning
import com.passbolt.mobile.android.feature.startup.StartUpSideEffect.NavigateToSetup
import com.passbolt.mobile.android.feature.startup.StartUpSideEffect.NavigateToSignIn
import com.passbolt.mobile.android.feature.startup.deprecatedoswarning.DeprecatedOsWarningBottomSheet
import com.passbolt.mobile.android.ui.AccountSetupDataModel
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
