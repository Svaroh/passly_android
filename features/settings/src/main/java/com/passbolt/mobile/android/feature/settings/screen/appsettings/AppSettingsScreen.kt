/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * The name "Passbolt" is a registered trademark of Passbolt SA, and Passbolt SA hereby declines to grant a trademark
 * license to "Passbolt" pursuant to the GNU Affero General Public License version 3 Section 7(e), without a separate
 * agreement with Passbolt SA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Passbolt SA (https://www.passbolt.com)
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://www.passbolt.com Passbolt (tm)
 * @since v1.0
 */

package net.svaroh.passly.feature.settings.screen.appsettings

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.svaroh.passly.core.compose.SideEffectDispatcher
import net.svaroh.passly.core.navigation.ActivityIntents
import net.svaroh.passly.core.navigation.ActivityIntents.AuthConfig.RefreshPassphrase
import net.svaroh.passly.core.navigation.compose.AppNavigator
import net.svaroh.passly.core.navigation.compose.keys.SettingsNavigationKey.Autofill
import net.svaroh.passly.core.navigation.compose.keys.SettingsNavigationKey.DefaultFilter
import net.svaroh.passly.core.navigation.compose.keys.SettingsNavigationKey.ExpertSettings
import net.svaroh.passly.core.ui.R
import net.svaroh.passly.core.ui.dialogs.CancelAccountTransferAlertDialog
import net.svaroh.passly.core.ui.dialogs.ConfigureBiometricAlertDialog
import net.svaroh.passly.core.ui.dialogs.DisableBiometricAlertDialog
import net.svaroh.passly.core.ui.menu.OpenableSettingsItem
import net.svaroh.passly.core.ui.menu.SwitchableSettingsItem
import net.svaroh.passly.core.ui.topbar.BackNavigationIcon
import net.svaroh.passly.core.ui.topbar.TitleAppBar
import net.svaroh.passly.feature.authentication.auth.showBiometricPrompt
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.CancelConfigureBiometric
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.CancelConfirmKeyChange
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.CancelDisableBiometric
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.CanceledBiometricAuth
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.ConfigureBiometric
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.ConfirmDisableBiometric
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.ConfirmKeyChangeClick
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.ErroredBiometricAuth
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.FinalizedBiometricAuth
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.GoBack
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.GoToAutofill
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.GoToDefaultFilter
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.GoToExpertSettings
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.Initialize
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.InvalidateBiometricKeyPermanently
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.RefreshedPassphrase
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsIntent.ToggleBiometric
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsSideEffect.NavigateToAutofill
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsSideEffect.NavigateToDefaultFilter
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsSideEffect.NavigateToExpertSettings
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsSideEffect.NavigateToGetPassphrase
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsSideEffect.NavigateToSystemSettings
import net.svaroh.passly.feature.settings.screen.appsettings.AppSettingsSideEffect.NavigateUp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import java.util.concurrent.Executor
import net.svaroh.passly.core.localization.R as LocalizationR
import net.svaroh.passly.core.ui.R as CoreUiR

@Composable
internal fun AppSettingsScreen(
    modifier: Modifier = Modifier,
    navigator: AppNavigator = koinInject(),
    viewModel: AppSettingsViewModel = koinViewModel(),
) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val environment = rememberAppSettingsEnvironment(viewModel::onIntent)

    LaunchedEffect(Unit) {
        viewModel.onIntent(Initialize)
    }

    AppSettingsSideEffectsHandler(
        sideEffectFlow = viewModel.sideEffect,
        environment = environment,
        onIntent = viewModel::onIntent,
        navigator = navigator,
    )

    AppSettingsScreen(
        modifier = modifier,
        state = state.value,
        snackbarHostState = environment.snackbarHostState,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun rememberAppSettingsEnvironment(onIntent: (AppSettingsIntent) -> Unit): AppSettingsEnvironment {
    val context = LocalContext.current
    val authenticationLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                onIntent(RefreshedPassphrase)
            }
        }
    val biometricPromptBuilder = getKoin().get<BiometricPrompt.PromptInfo.Builder>()
    val executor = getKoin().get<Executor>()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    return remember {
        AppSettingsEnvironment(
            context = context,
            authenticationLauncher = authenticationLauncher,
            biometricPromptBuilder = biometricPromptBuilder,
            executor = executor,
            snackbarHostState = snackbarHostState,
            coroutineScope = coroutineScope,
        )
    }
}

@Composable
private fun AppSettingsSideEffectsHandler(
    sideEffectFlow: Flow<AppSettingsSideEffect>,
    environment: AppSettingsEnvironment,
    onIntent: (AppSettingsIntent) -> Unit,
    navigator: AppNavigator,
) {
    SideEffectDispatcher(sideEffectFlow) {
        when (it) {
            NavigateToAutofill -> navigator.navigateToKey(Autofill)
            NavigateToDefaultFilter -> navigator.navigateToKey(DefaultFilter)
            NavigateToExpertSettings -> navigator.navigateToKey(ExpertSettings)
            NavigateUp -> navigator.navigateBack()
            NavigateToGetPassphrase ->
                environment.authenticationLauncher.launch(
                    ActivityIntents.authentication(
                        environment.context,
                        RefreshPassphrase,
                    ),
                )
            NavigateToSystemSettings -> environment.context.startActivity(Intent(Settings.ACTION_SETTINGS))
            is AppSettingsSideEffect.LaunchBiometricPrompt ->
                showBiometricPrompt(
                    activity = environment.context as AppCompatActivity,
                    executor = environment.executor,
                    biometricPromptBuilder = environment.biometricPromptBuilder,
                    biometricEncryptionCipher = it.cipher,
                    onAuthenticationSuccess = { onIntent(FinalizedBiometricAuth(it)) },
                    onAuthenticationCancelled = { onIntent(CanceledBiometricAuth) },
                    onAuthenticationError = { onIntent(ErroredBiometricAuth(it)) },
                    onKeyPermanentlyInvalidated = { exception -> onIntent(InvalidateBiometricKeyPermanently(exception)) },
                )
            is AppSettingsSideEffect.ShowErrorSnackbar ->
                environment.coroutineScope.launch {
                    environment.snackbarHostState.showSnackbar(getSnackbarMessage(it, environment), duration = SnackbarDuration.Short)
                }
        }
    }
}

private fun getSnackbarMessage(
    snackbar: AppSettingsSideEffect.ShowErrorSnackbar,
    environment: AppSettingsEnvironment,
): String =
    when (snackbar.snackbarKind) {
        AppSettingsSideEffect.SnackbarKind.AUTHENTICAION_ERROR ->
            environment.context.getString(
                LocalizationR.string.settings_app_settings_biometry_authentication_error,
                snackbar.additionalMessage.orEmpty(),
            )
        AppSettingsSideEffect.SnackbarKind.BIOMETRY_ERROR ->
            environment.context.getString(
                LocalizationR.string.settings_app_settings_biometry_error,
                snackbar.additionalMessage.orEmpty(),
            )
    }

@Composable
private fun AppSettingsScreen(
    state: AppSettingsState,
    snackbarHostState: SnackbarHostState,
    onIntent: (AppSettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TitleAppBar(
                title = stringResource(LocalizationR.string.settings_app_settings),
                navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = colorResource(CoreUiR.color.red),
                        contentColor = colorResource(CoreUiR.color.white),
                    )
                },
            )
        },
        content = { paddingValues ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState()),
            ) {
                SwitchableSettingsItem(
                    iconPainter = painterResource(R.drawable.ic_fingerprint),
                    title = stringResource(LocalizationR.string.settings_app_settings_biometric),
                    isChecked = state.isBiometricEnabled,
                    onCheckedChange = { onIntent(ToggleBiometric) },
                )

                OpenableSettingsItem(
                    iconPainter = painterResource(R.drawable.ic_key),
                    title = stringResource(LocalizationR.string.settings_app_settings_autofill),
                    onClick = { onIntent(GoToAutofill) },
                    hasWarningBadge = state.isAutofillConflictDetected,
                )

                OpenableSettingsItem(
                    iconPainter = painterResource(R.drawable.ic_filter),
                    title = stringResource(LocalizationR.string.settings_app_settings_default_filter),
                    onClick = { onIntent(GoToDefaultFilter) },
                )

                OpenableSettingsItem(
                    iconPainter = painterResource(R.drawable.ic_cog),
                    title = stringResource(LocalizationR.string.settings_app_settings_expert_settings),
                    onClick = { onIntent(GoToExpertSettings) },
                )
            }

            DisableBiometricAlertDialog(
                isVisible = state.isDisableBiometricDialogVisible,
                onDisableConfirm = { onIntent(ConfirmDisableBiometric) },
                onDismiss = { onIntent(CancelDisableBiometric) },
            )

            ConfigureBiometricAlertDialog(
                isVisible = state.isConfigureBiometricDialogVisible,
                onConfigureBiometric = { onIntent(ConfigureBiometric) },
                onDismiss = { onIntent(CancelConfigureBiometric) },
            )

            CancelAccountTransferAlertDialog(
                isVisible = state.isKeyChangesDialogDetectedVisible,
                onConfirm = { onIntent(ConfirmKeyChangeClick) },
                onDismiss = { onIntent(CancelConfirmKeyChange) },
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun AppSettingsPreview() {
    AppSettingsScreen(
        state = AppSettingsState(isBiometricEnabled = true),
        snackbarHostState = remember { SnackbarHostState() },
        onIntent = {},
        modifier = Modifier,
    )
}
