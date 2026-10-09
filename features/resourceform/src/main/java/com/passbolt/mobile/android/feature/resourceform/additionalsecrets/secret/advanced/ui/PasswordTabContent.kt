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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.svaroh.passly.core.passwordgenerator.Alphabets.MASK_DIGIT
import net.svaroh.passly.core.passwordgenerator.Alphabets.MASK_EMOJI
import net.svaroh.passly.core.passwordgenerator.Alphabets.MASK_LOWER
import net.svaroh.passly.core.passwordgenerator.Alphabets.MASK_PARENTHESIS
import net.svaroh.passly.core.passwordgenerator.Alphabets.MASK_SPECIAL_CHAR1
import net.svaroh.passly.core.passwordgenerator.Alphabets.MASK_SPECIAL_CHAR2
import net.svaroh.passly.core.passwordgenerator.Alphabets.MASK_SPECIAL_CHAR3
import net.svaroh.passly.core.passwordgenerator.Alphabets.MASK_SPECIAL_CHAR4
import net.svaroh.passly.core.passwordgenerator.Alphabets.MASK_SPECIAL_CHAR5
import net.svaroh.passly.core.passwordgenerator.Alphabets.MASK_UPPER
import net.svaroh.passly.core.ui.chip.SelectableChipCloud
import net.svaroh.passly.core.ui.chip.SelectableChipItemModel
import net.svaroh.passly.core.ui.slider.LabelledSlider
import net.svaroh.passly.core.ui.switchwithdescription.SwitchWithDescriptionItem
import net.svaroh.passly.feature.resourceform.additionalsecrets.secret.advanced.AdvancedSecretGenerationIntent
import net.svaroh.passly.feature.resourceform.additionalsecrets.secret.advanced.AdvancedSecretGenerationIntent.PasswordCharacterSetToggled
import net.svaroh.passly.feature.resourceform.additionalsecrets.secret.advanced.AdvancedSecretGenerationIntent.PasswordExcludeLookAlikeChanged
import net.svaroh.passly.feature.resourceform.additionalsecrets.secret.advanced.AdvancedSecretGenerationIntent.PasswordLengthChanged
import net.svaroh.passly.ui.PasswordGeneratorSettingsUiModel
import net.svaroh.passly.core.localization.R as LocalizationR

private const val PASSWORD_LENGTH_MIN = 8
private const val PASSWORD_LENGTH_MAX = 128

@Composable
internal fun PasswordTabContent(
    passwordSettings: PasswordGeneratorSettingsUiModel,
    onIntent: (AdvancedSecretGenerationIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(modifier = modifier) {
        LabelledSlider(
            title = stringResource(LocalizationR.string.resource_form_advanced_password_generation_length),
            value = passwordSettings.length,
            valueRange = PASSWORD_LENGTH_MIN..PASSWORD_LENGTH_MAX,
            onValueChange = { onIntent(PasswordLengthChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(LocalizationR.string.resource_form_advanced_password_generation_character_types),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        SelectableChipCloud(
            items = characterSetChips(context, passwordSettings),
            onToggle = { mask -> onIntent(PasswordCharacterSetToggled(mask)) },
        )
        Spacer(modifier = Modifier.height(8.dp))
        SwitchWithDescriptionItem(
            title = stringResource(LocalizationR.string.resource_form_advanced_password_generation_exclude_look_alike),
            isChecked = passwordSettings.excludeLookAlikeChars,
            onClick = { onIntent(PasswordExcludeLookAlikeChanged(!passwordSettings.excludeLookAlikeChars)) },
            contentPadding = PaddingValues(vertical = 8.dp),
        )
    }
}

private fun characterSetChips(
    context: Context,
    settings: PasswordGeneratorSettingsUiModel,
): List<SelectableChipItemModel> =
    listOf(
        SelectableChipItemModel(
            id = MASK_UPPER,
            label = context.getString(LocalizationR.string.resource_form_character_set_upper),
            isSelected = settings.maskUpper,
        ),
        SelectableChipItemModel(
            id = MASK_DIGIT,
            label = context.getString(LocalizationR.string.resource_form_character_set_digit),
            isSelected = settings.maskDigit,
        ),
        SelectableChipItemModel(
            id = MASK_LOWER,
            label = context.getString(LocalizationR.string.resource_form_character_set_lower),
            isSelected = settings.maskLower,
        ),
        SelectableChipItemModel(
            id = MASK_SPECIAL_CHAR1,
            label = context.getString(LocalizationR.string.resource_form_character_set_special_char1),
            isSelected = settings.maskChar1,
        ),
        SelectableChipItemModel(
            id = MASK_PARENTHESIS,
            label = context.getString(LocalizationR.string.resource_form_character_set_parenthesis),
            isSelected = settings.maskParenthesis,
        ),
        SelectableChipItemModel(
            id = MASK_SPECIAL_CHAR2,
            label = context.getString(LocalizationR.string.resource_form_character_set_special_char2),
            isSelected = settings.maskChar2,
        ),
        SelectableChipItemModel(
            id = MASK_SPECIAL_CHAR3,
            label = context.getString(LocalizationR.string.resource_form_character_set_special_char3),
            isSelected = settings.maskChar3,
        ),
        SelectableChipItemModel(
            id = MASK_SPECIAL_CHAR4,
            label = context.getString(LocalizationR.string.resource_form_character_set_special_char4),
            isSelected = settings.maskChar4,
        ),
        SelectableChipItemModel(
            id = MASK_SPECIAL_CHAR5,
            label = context.getString(LocalizationR.string.resource_form_character_set_special_char5),
            isSelected = settings.maskChar5,
        ),
        SelectableChipItemModel(
            id = MASK_EMOJI,
            label = context.getString(LocalizationR.string.resource_form_character_set_emoji),
            isSelected = settings.maskEmoji,
        ),
    )
