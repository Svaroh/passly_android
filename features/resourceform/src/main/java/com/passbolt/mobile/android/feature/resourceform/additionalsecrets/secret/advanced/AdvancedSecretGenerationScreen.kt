/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2026 Passbolt SA
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

package com.passbolt.mobile.android.feature.resourceform.additionalsecrets.secret.advanced

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.passbolt.mobile.android.core.compose.PassboltTheme
import com.passbolt.mobile.android.core.compose.SideEffectDispatcher
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.compose.results.NavigationResultEventBus
import com.passbolt.mobile.android.core.security.flagsecure.FlagSecureEffect
import com.passbolt.mobile.android.core.ui.button.PrimaryButton
import com.passbolt.mobile.android.core.ui.section.Section
import com.passbolt.mobile.android.core.ui.tabs.ButtonTabItemModel
import com.passbolt.mobile.android.core.ui.tabs.ButtonTabs
import com.passbolt.mobile.android.core.ui.topbar.BackNavigationIcon
import com.passbolt.mobile.android.core.ui.topbar.TitleAppBar
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.secret.advanced.AdvancedSecretGenerationIntent.GoBack
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.secret.advanced.AdvancedSecretGenerationIntent.PreviewMaskToggled
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.secret.advanced.AdvancedSecretGenerationIntent.SavePreferences
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.secret.advanced.AdvancedSecretGenerationIntent.TabSelected
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.secret.advanced.AdvancedSecretGenerationSideEffect.ApplyAndGoBack
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.secret.advanced.AdvancedSecretGenerationSideEffect.NavigateBack
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.secret.advanced.ui.PassphraseTabContent
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.secret.advanced.ui.PasswordTabContent
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.secret.advanced.ui.SecretPreviewSection
import com.passbolt.mobile.android.feature.resourceform.navigation.AdvancedSecretGenerationFormResult
import com.passbolt.mobile.android.ui.CaseTypeUiModel.LOWERCASE
import com.passbolt.mobile.android.ui.PassphraseGeneratorSettingsUiModel
import com.passbolt.mobile.android.ui.PasswordGeneratorSettingsUiModel
import com.passbolt.mobile.android.ui.PasswordGeneratorTypeUiModel
import com.passbolt.mobile.android.ui.PasswordGeneratorTypeUiModel.PASSPHRASE
import com.passbolt.mobile.android.ui.PasswordGeneratorTypeUiModel.PASSWORD
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import com.passbolt.mobile.android.core.localization.R as LocalizationR

@Composable
internal fun AdvancedSecretGenerationScreen(
    initialTab: PasswordGeneratorTypeUiModel,
    initialPasswordSettings: PasswordGeneratorSettingsUiModel,
    initialPassphraseSettings: PassphraseGeneratorSettingsUiModel,
    modifier: Modifier = Modifier,
    navigator: AppNavigator = koinInject(),
    viewModel: AdvancedSecretGenerationViewModel =
        koinViewModel(
            parameters = {
                parametersOf(initialTab, initialPasswordSettings, initialPassphraseSettings)
            },
        ),
) {
    FlagSecureEffect()

    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val resultBus = NavigationResultEventBus.current

    AdvancedSecretGenerationScreen(
        modifier = modifier,
        state = state.value,
        onIntent = viewModel::onIntent,
    )

    SideEffectDispatcher(viewModel.sideEffect) { effect ->
        when (effect) {
            is ApplyAndGoBack -> {
                resultBus.sendResult(
                    result =
                        AdvancedSecretGenerationFormResult(
                            passwordSettings = effect.passwordSettings,
                            passphraseSettings = effect.passphraseSettings,
                            selectedTab = effect.selectedTab,
                            generatedSecret = effect.generatedSecret,
                        ),
                )
                navigator.navigateBack()
            }
            NavigateBack -> navigator.navigateBack()
        }
    }
}

@Composable
private fun AdvancedSecretGenerationScreen(
    state: AdvancedSecretGenerationState,
    onIntent: (AdvancedSecretGenerationIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TitleAppBar(
                title = getScreenTitle(context, state.selectedTab),
                navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.background) {
                PrimaryButton(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(LocalizationR.string.resource_form_advanced_password_generation_save_preferences),
                    onClick = { onIntent(SavePreferences) },
                    isEnabled = state.preview.isNotEmpty(),
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
            SecretPreviewSection(
                selectedTab = state.selectedTab,
                preview = state.preview,
                isPreviewMasked = state.isPreviewMasked,
                onToggleMask = { onIntent(PreviewMaskToggled) },
                minimumEntropyBits = state.minimumEntropyBits,
            )
            Spacer(modifier = Modifier.height(16.dp))
            ButtonTabs(
                items =
                    listOf(
                        ButtonTabItemModel(
                            id = PASSWORD.name,
                            label =
                                stringResource(LocalizationR.string.resource_form_advanced_password_generation_tab_password),
                            isSelected = state.selectedTab == PASSWORD,
                        ),
                        ButtonTabItemModel(
                            id = PASSPHRASE.name,
                            label =
                                stringResource(LocalizationR.string.resource_form_advanced_password_generation_tab_passphrase),
                            isSelected = state.selectedTab == PASSPHRASE,
                        ),
                    ),
                onSelect = { id -> onIntent(TabSelected(PasswordGeneratorTypeUiModel.valueOf(id))) },
            )
            Spacer(modifier = Modifier.height(16.dp))
            Section {
                when (state.selectedTab) {
                    PASSWORD ->
                        PasswordTabContent(
                            passwordSettings = state.passwordSettings,
                            onIntent = onIntent,
                        )
                    PASSPHRASE ->
                        PassphraseTabContent(
                            passphraseSettings = state.passphraseSettings,
                            onIntent = onIntent,
                        )
                }
            }
        }
    }
}

private fun getScreenTitle(
    context: Context,
    selectedTab: PasswordGeneratorTypeUiModel,
): String =
    when (selectedTab) {
        PASSWORD ->
            context.getString(LocalizationR.string.resource_form_advanced_password_generation)
        PASSPHRASE ->
            context.getString(LocalizationR.string.resource_form_advanced_passphrase_generation)
    }

@Preview(showBackground = true)
@Composable
private fun AdvancedSecretGenerationScreenPasswordPreview() {
    PassboltTheme {
        AdvancedSecretGenerationScreen(
            state =
                AdvancedSecretGenerationState(
                    selectedTab = PASSWORD,
                    passwordSettings =
                        PasswordGeneratorSettingsUiModel(
                            length = 16,
                            maskUpper = true,
                            maskLower = false,
                            maskDigit = true,
                            maskParenthesis = false,
                            maskEmoji = false,
                            maskChar1 = true,
                            maskChar2 = false,
                            maskChar3 = false,
                            maskChar4 = false,
                            maskChar5 = false,
                            excludeLookAlikeChars = true,
                        ),
                    passphraseSettings =
                        PassphraseGeneratorSettingsUiModel(
                            words = 9,
                            wordSeparator = " ",
                            wordCase = LOWERCASE,
                        ),
                    preview = "I76V17Z6D73rél§\"réluhdciuzeg",
                ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AdvancedSecretGenerationScreenPassphrasePreview() {
    PassboltTheme {
        AdvancedSecretGenerationScreen(
            state =
                AdvancedSecretGenerationState(
                    selectedTab = PASSPHRASE,
                    passwordSettings =
                        PasswordGeneratorSettingsUiModel(
                            length = 16,
                            maskUpper = true,
                            maskLower = true,
                            maskDigit = true,
                            maskParenthesis = true,
                            maskEmoji = false,
                            maskChar1 = true,
                            maskChar2 = true,
                            maskChar3 = true,
                            maskChar4 = true,
                            maskChar5 = true,
                            excludeLookAlikeChars = true,
                        ),
                    passphraseSettings =
                        PassphraseGeneratorSettingsUiModel(
                            words = 18,
                            wordSeparator = " ",
                            wordCase = LOWERCASE,
                        ),
                    preview = "bodacious alibi wriggle unheated verbal...",
                ),
            onIntent = {},
        )
    }
}
