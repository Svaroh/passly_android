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

package net.svaroh.passly.feature.resourceform.additionalsecrets.pincode.advanced

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.svaroh.passly.core.compose.PassboltTheme
import net.svaroh.passly.core.compose.SideEffectDispatcher
import net.svaroh.passly.core.navigation.compose.AppNavigator
import net.svaroh.passly.core.navigation.compose.results.NavigationResultEventBus
import net.svaroh.passly.core.ui.button.PrimaryButton
import net.svaroh.passly.core.ui.slider.LabelledSlider
import net.svaroh.passly.core.ui.topbar.BackNavigationIcon
import net.svaroh.passly.core.ui.topbar.TitleAppBar
import net.svaroh.passly.feature.resourceform.additionalsecrets.pincode.advanced.PinCodeAdvancedGenerationFormIntent.GoBack
import net.svaroh.passly.feature.resourceform.additionalsecrets.pincode.advanced.PinCodeAdvancedGenerationFormIntent.LengthChanged
import net.svaroh.passly.feature.resourceform.additionalsecrets.pincode.advanced.PinCodeAdvancedGenerationFormIntent.SavePreferences
import net.svaroh.passly.feature.resourceform.additionalsecrets.pincode.advanced.PinCodeAdvancedGenerationFormSideEffect.ApplyAndGoBack
import net.svaroh.passly.feature.resourceform.additionalsecrets.pincode.advanced.PinCodeAdvancedGenerationFormSideEffect.NavigateBack
import net.svaroh.passly.feature.resourceform.navigation.PinCodeAdvancedGenerationFormResult
import net.svaroh.passly.ui.LeadingContentType
import net.svaroh.passly.ui.PinCodeUiModel
import net.svaroh.passly.ui.PinCodeUiModel.Companion.MAX_LENGTH
import net.svaroh.passly.ui.PinCodeUiModel.Companion.MIN_LENGTH
import net.svaroh.passly.ui.ResourceFormMode
import net.svaroh.passly.ui.ResourceFormMode.Create
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import net.svaroh.passly.core.localization.R as LocalizationR
import net.svaroh.passly.core.ui.R as CoreUiR

@Composable
internal fun PinCodeAdvancedGenerationFormScreen(
    mode: ResourceFormMode,
    pinCodeUiModel: PinCodeUiModel,
    modifier: Modifier = Modifier,
    navigator: AppNavigator = koinInject(),
    viewModel: PinCodeAdvancedGenerationFormViewModel =
        koinViewModel(parameters = { parametersOf(mode, pinCodeUiModel) }),
) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()
    val resultBus = NavigationResultEventBus.current

    PinCodeAdvancedGenerationFormScreen(
        modifier = modifier,
        state = state.value,
        onIntent = viewModel::onIntent,
    )

    SideEffectDispatcher(viewModel.sideEffect) {
        when (it) {
            is ApplyAndGoBack -> {
                resultBus.sendResult(result = PinCodeAdvancedGenerationFormResult(it.pinCodeUiModel))
                navigator.navigateBack()
            }
            NavigateBack -> navigator.navigateBack()
        }
    }
}

@Composable
private fun PinCodeAdvancedGenerationFormScreen(
    onIntent: (PinCodeAdvancedGenerationFormIntent) -> Unit,
    state: PinCodeAdvancedGenerationFormState,
    modifier: Modifier = Modifier,
) {
    val sectionColor = colorResource(CoreUiR.color.section_background)

    Scaffold(
        modifier = modifier,
        topBar = {
            TitleAppBar(
                title = stringResource(LocalizationR.string.resource_form_pin_code_advanced),
                navigationIcon = { BackNavigationIcon(onBackClick = { onIntent(GoBack) }) },
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                PrimaryButton(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(LocalizationR.string.resource_form_pin_code_save_preferences),
                    onClick = { onIntent(SavePreferences) },
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
            LabelledSlider(
                title = stringResource(LocalizationR.string.resource_form_pin_code_length),
                value = state.length,
                valueRange = MIN_LENGTH..MAX_LENGTH,
                onValueChange = { onIntent(LengthChanged(it)) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(sectionColor)
                        .padding(16.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PinCodeAdvancedGenerationFormScreenPreview() {
    PassboltTheme {
        PinCodeAdvancedGenerationFormScreen(
            onIntent = {},
            state =
                PinCodeAdvancedGenerationFormState(
                    resourceFormMode =
                        Create(
                            leadingContentType = LeadingContentType.PIN_CODE,
                            parentFolderId = null,
                        ),
                    length = 4,
                ),
        )
    }
}
