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

package net.svaroh.passly.feature.resourceform.additionalsecrets.secret.advanced.ui

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.svaroh.passly.core.ui.dropdown.DropdownInput
import net.svaroh.passly.core.ui.slider.LabelledSlider
import net.svaroh.passly.core.ui.text.TextInput
import net.svaroh.passly.feature.resourceform.additionalsecrets.secret.advanced.AdvancedSecretGenerationIntent
import net.svaroh.passly.feature.resourceform.additionalsecrets.secret.advanced.AdvancedSecretGenerationIntent.PassphraseWordCaseChanged
import net.svaroh.passly.feature.resourceform.additionalsecrets.secret.advanced.AdvancedSecretGenerationIntent.PassphraseWordSeparatorChanged
import net.svaroh.passly.feature.resourceform.additionalsecrets.secret.advanced.AdvancedSecretGenerationIntent.PassphraseWordsCountChanged
import net.svaroh.passly.ui.CaseTypeUiModel
import net.svaroh.passly.ui.CaseTypeUiModel.CAMELCASE
import net.svaroh.passly.ui.CaseTypeUiModel.LOWERCASE
import net.svaroh.passly.ui.CaseTypeUiModel.UPPERCASE
import net.svaroh.passly.ui.PassphraseGeneratorSettingsUiModel
import net.svaroh.passly.core.localization.R as LocalizationR

private const val PASSPHRASE_WORDS_MIN = 4
private const val PASSPHRASE_WORDS_MAX = 40

@Composable
internal fun PassphraseTabContent(
    passphraseSettings: PassphraseGeneratorSettingsUiModel,
    onIntent: (AdvancedSecretGenerationIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(modifier = modifier) {
        LabelledSlider(
            title = stringResource(LocalizationR.string.resource_form_advanced_password_generation_number_of_words),
            value = passphraseSettings.words,
            valueRange = PASSPHRASE_WORDS_MIN..PASSPHRASE_WORDS_MAX,
            onValueChange = { onIntent(PassphraseWordsCountChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextInput(
            title = stringResource(LocalizationR.string.resource_form_advanced_password_generation_word_separator),
            hint = stringResource(LocalizationR.string.resource_form_advanced_password_generation_word_separator_hint),
            text = passphraseSettings.wordSeparator,
            onTextChange = { onIntent(PassphraseWordSeparatorChanged(it)) },
        )
        Spacer(modifier = Modifier.height(16.dp))
        val caseLabels = wordCaseLabels(context)
        DropdownInput(
            title = stringResource(LocalizationR.string.resource_form_advanced_password_generation_word_case),
            items = caseLabels.values.toList(),
            selectedItem = caseLabels.getValue(passphraseSettings.wordCase),
            onItemSelect = { label ->
                val case = caseLabels.entries.first { it.value == label }.key
                onIntent(PassphraseWordCaseChanged(case))
            },
        )
    }
}

private fun wordCaseLabels(context: Context): Map<CaseTypeUiModel, String> =
    linkedMapOf(
        LOWERCASE to context.getString(LocalizationR.string.resource_form_word_case_lowercase),
        UPPERCASE to context.getString(LocalizationR.string.resource_form_word_case_uppercase),
        CAMELCASE to context.getString(LocalizationR.string.resource_form_word_case_camelcase),
    )
