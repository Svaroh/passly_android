package net.svaroh.passly.feature.resourceform.additionalsecrets.password

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.svaroh.passly.core.compose.PassboltTheme
import net.svaroh.passly.core.compose.SideEffectDispatcher
import net.svaroh.passly.core.navigation.compose.AppNavigator
import net.svaroh.passly.core.navigation.compose.keys.ResourceFormNavigationKey.AdvancedSecretGenerationForm
import net.svaroh.passly.core.navigation.compose.results.NavigationResultEventBus
import net.svaroh.passly.core.navigation.compose.results.ResultEffect
import net.svaroh.passly.core.security.flagsecure.FlagSecureEffect
import net.svaroh.passly.core.ui.button.PrimaryButton
import net.svaroh.passly.core.ui.dialogs.UnableToGeneratePasswordAlertDialog
import net.svaroh.passly.core.ui.text.TextInput
import net.svaroh.passly.core.ui.topbar.BackNavigationIcon
import net.svaroh.passly.core.ui.topbar.TitleAppBar
import net.svaroh.passly.feature.resourceform.additionalsecrets.password.PasswordFormIntent.AdvancedSecretGenerationResult
import net.svaroh.passly.feature.resourceform.additionalsecrets.password.PasswordFormIntent.ApplyChanges
import net.svaroh.passly.feature.resourceform.additionalsecrets.password.PasswordFormIntent.DismissUnableToGeneratePassword
import net.svaroh.passly.feature.resourceform.additionalsecrets.password.PasswordFormIntent.GeneratePassword
import net.svaroh.passly.feature.resourceform.additionalsecrets.password.PasswordFormIntent.GoBack
import net.svaroh.passly.feature.resourceform.additionalsecrets.password.PasswordFormIntent.MainUriTextChanged
import net.svaroh.passly.feature.resourceform.additionalsecrets.password.PasswordFormIntent.OpenAdvancedSecretGeneration
import net.svaroh.passly.feature.resourceform.additionalsecrets.password.PasswordFormIntent.PasswordTextChanged
import net.svaroh.passly.feature.resourceform.additionalsecrets.password.PasswordFormIntent.UsernameTextChanged
import net.svaroh.passly.feature.resourceform.additionalsecrets.password.PasswordFormSideEffect.ApplyAndGoBack
import net.svaroh.passly.feature.resourceform.additionalsecrets.password.PasswordFormSideEffect.NavigateBack
import net.svaroh.passly.feature.resourceform.additionalsecrets.password.PasswordFormSideEffect.NavigateToAdvancedSecretGeneration
import net.svaroh.passly.feature.resourceform.additionalsecrets.password.ui.PasswordGenerationInput
import net.svaroh.passly.feature.resourceform.main.ui.SettingRow
import net.svaroh.passly.feature.resourceform.navigation.AdvancedSecretGenerationFormResult
import net.svaroh.passly.feature.resourceform.navigation.PasswordFormResult
import net.svaroh.passly.ui.LeadingContentType
import net.svaroh.passly.ui.PasswordStrength
import net.svaroh.passly.ui.PasswordUiModel
import net.svaroh.passly.ui.ResourceFormMode
import net.svaroh.passly.ui.ResourceFormMode.Create
import net.svaroh.passly.ui.ResourceFormMode.Edit
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import net.svaroh.passly.core.localization.R as LocalizationR
import net.svaroh.passly.core.ui.R as CoreUiR

@Composable
internal fun PasswordFormScreen(
    mode: ResourceFormMode,
    passwordModel: PasswordUiModel,
    modifier: Modifier = Modifier,
    navigator: AppNavigator = koinInject(),
    viewModel: PasswordFormViewModel =
        koinViewModel(
            parameters = {
                parametersOf(mode, passwordModel)
            },
        ),
) {
    FlagSecureEffect()

    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val resultBus = NavigationResultEventBus.current

    PasswordFormScreen(
        modifier = modifier,
        state = state,
        onIntent = viewModel::onIntent,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            is ApplyAndGoBack -> {
                resultBus.sendResult(result = PasswordFormResult(it.model))
                navigator.navigateBack()
            }
            NavigateBack -> navigator.navigateBack()
            is NavigateToAdvancedSecretGeneration ->
                navigator.navigateToKey(
                    AdvancedSecretGenerationForm(
                        selectedTab = it.selectedTab,
                        passwordSettings = it.passwordSettings,
                        passphraseSettings = it.passphraseSettings,
                    ),
                )
        }
    }

    ResultEffect<AdvancedSecretGenerationFormResult> { result ->
        viewModel.onIntent(AdvancedSecretGenerationResult(result))
    }
}

@Composable
private fun PasswordFormScreen(
    state: PasswordFormState,
    onIntent: (PasswordFormIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TitleAppBar(
                title = getScreenTitle(context, state.resourceFormMode),
                navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.background,
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                PrimaryButton(
                    text = stringResource(LocalizationR.string.apply),
                    onClick = { onIntent(ApplyChanges) },
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
        ) {
            Text(
                text = stringResource(LocalizationR.string.resource_form_password),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorResource(CoreUiR.color.section_background))
                        .padding(12.dp),
            ) {
                TextInput(
                    title = stringResource(LocalizationR.string.resource_form_main_uri),
                    text = state.mainUri,
                    onTextChange = { onIntent(MainUriTextChanged(it)) },
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextInput(
                    title = stringResource(LocalizationR.string.resource_form_username),
                    text = state.username,
                    onTextChange = { onIntent(UsernameTextChanged(it)) },
                )
                Spacer(modifier = Modifier.height(16.dp))
                PasswordGenerationInput(
                    password = state.password,
                    passwordStrength = state.passwordStrength,
                    entropy = state.entropy,
                    onPasswordChange = { onIntent(PasswordTextChanged(it)) },
                    onGenerateClick = { onIntent(GeneratePassword) },
                )
                Spacer(modifier = Modifier.height(16.dp))
                SettingRow(
                    leadingIconResId = CoreUiR.drawable.ic_cog,
                    text = stringResource(LocalizationR.string.resource_form_advanced_password_generation),
                    onClick = { onIntent(OpenAdvancedSecretGeneration) },
                )
            }
        }
    }

    UnableToGeneratePasswordAlertDialog(
        isVisible = state.isUnableToGeneratePasswordDialogVisible,
        requiredEntropy = state.minimumEntropyBits,
        onDismiss = { onIntent(DismissUnableToGeneratePassword) },
    )
}

private fun getScreenTitle(
    context: Context,
    resourceFormMode: ResourceFormMode?,
): String =
    when (resourceFormMode) {
        is Create -> context.getString(LocalizationR.string.resource_form_create_password)
        is Edit -> context.getString(LocalizationR.string.resource_form_edit_resource, resourceFormMode.resourceName)
        null -> ""
    }

@Preview(showBackground = true)
@Composable
private fun PasswordFormScreenPreview() {
    PassboltTheme {
        PasswordFormScreen(
            state =
                PasswordFormState(
                    resourceFormMode = Create(LeadingContentType.PASSWORD, null),
                    password = "p@ssb0lt!",
                    passwordStrength = PasswordStrength.Strong,
                    entropy = 60.0,
                    mainUri = "https://passbolt.com",
                    username = "ada@passbolt.com",
                ),
            onIntent = {},
        )
    }
}
