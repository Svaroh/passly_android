package net.svaroh.passly.feature.autofill.resources

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import net.svaroh.passly.core.compose.PassboltTheme
import net.svaroh.passly.core.compose.SideEffectDispatcher
import net.svaroh.passly.core.navigation.ActivityIntents
import net.svaroh.passly.core.navigation.ActivityIntents.AuthConfig.SignIn
import net.svaroh.passly.core.navigation.AppContext
import net.svaroh.passly.core.navigation.AutofillType
import net.svaroh.passly.core.navigation.compose.AppNavigator
import net.svaroh.passly.core.navigation.compose.NavigationActivity.Start
import net.svaroh.passly.core.ui.progressdialog.ProgressDialog
import net.svaroh.passly.domain.accounts.AuthenticatedAccountFlow
import net.svaroh.passly.feature.authentication.compose.AuthenticationHandler
import net.svaroh.passly.feature.autofill.resources.AutofillResourcesIntent.UserAuthenticated
import net.svaroh.passly.feature.autofill.resources.AutofillResourcesSideEffect.AutofillReturn
import net.svaroh.passly.feature.autofill.resources.AutofillResourcesSideEffect.NavigateToAuth
import net.svaroh.passly.feature.autofill.resources.AutofillResourcesSideEffect.NavigateToSetup
import net.svaroh.passly.feature.autofill.resources.AutofillResourcesSideEffect.ShowToast
import net.svaroh.passly.feature.autofill.resources.datasetstrategy.ReturnAutofillDatasetStrategy
import net.svaroh.passly.feature.home.navigation.HomeTabContent
import net.svaroh.passly.feature.otp.navigation.TotpTabContent
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun AutofillResourcesScreen(
    autofillUri: String?,
    autofillType: AutofillType,
    returnAutofillDatasetStrategy: ReturnAutofillDatasetStrategy,
    modifier: Modifier = Modifier,
    viewModel: AutofillResourcesViewModel =
        koinViewModel(
            parameters = { parametersOf(autofillUri) },
        ),
    appNavigator: AppNavigator = koinInject(),
    authenticatedAccountFlow: AuthenticatedAccountFlow = koinInject(),
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val authenticatedAccount by authenticatedAccountFlow.authenticatedAccount.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current

    val authLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.onIntent(UserAuthenticated)
            } else {
                activity?.setResult(Activity.RESULT_CANCELED)
                activity?.finish()
            }
        }

    AuthenticationHandler()

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            NavigateToAuth ->
                authLauncher.launch(
                    ActivityIntents.authentication(
                        context,
                        SignIn,
                        appContext = AppContext.AUTOFILL,
                    ),
                )
            NavigateToSetup -> {
                appNavigator.apply {
                    startNavigationActivity(context, Start)
                    finishActivity(activity)
                }
            }
            is AutofillReturn ->
                returnAutofillDatasetStrategy.returnDataset(it.payload)
            is ShowToast ->
                Toast.makeText(context, getToastMessage(context, it.type), Toast.LENGTH_SHORT).show()
        }
    }

    AutofillResourcesScreen(
        showHome = state.showHome,
        showProgress = state.showProgress,
        autofillType = autofillType,
        authenticatedAccount = authenticatedAccount,
        modifier = modifier,
    )
}

@Composable
private fun AutofillResourcesScreen(
    showHome: Boolean,
    showProgress: Boolean,
    autofillType: AutofillType,
    authenticatedAccount: String?,
    modifier: Modifier = Modifier,
) {
    PassboltTheme {
        Scaffold(modifier = modifier) { innerPadding ->
            Box(
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding),
            ) {
                if (showHome) {
                    key(authenticatedAccount) {
                        CompositionLocalProvider(
                            LocalViewModelStoreOwner provides rememberViewModelStoreOwner(),
                        ) {
                            when (autofillType) {
                                AutofillType.CREDENTIALS, AutofillType.CREDENTIALS_AND_TOTP -> HomeTabContent()
                                AutofillType.TOTP -> TotpTabContent()
                            }
                        }
                    }
                }

                ProgressDialog(showProgress)
            }
        }
    }
}
