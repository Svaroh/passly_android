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

package net.svaroh.passly.ui

import kotlinx.serialization.Serializable

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

data class PasswordPoliciesUiModel(
    val defaultGenerator: PasswordGeneratorTypeUiModel,
    val passwordGeneratorSettings: PasswordGeneratorSettingsUiModel,
    val passphraseGeneratorSettings: PassphraseGeneratorSettingsUiModel,
    val isExternalDictionaryCheckEnabled: Boolean,
)

@Serializable
data class PasswordGeneratorSettingsUiModel(
    val length: Int,
    val maskUpper: Boolean,
    val maskLower: Boolean,
    val maskDigit: Boolean,
    val maskParenthesis: Boolean,
    val maskEmoji: Boolean,
    val maskChar1: Boolean,
    val maskChar2: Boolean,
    val maskChar3: Boolean,
    val maskChar4: Boolean,
    val maskChar5: Boolean,
    val excludeLookAlikeChars: Boolean,
)

@Serializable
data class PassphraseGeneratorSettingsUiModel(
    val words: Int,
    val wordSeparator: String,
    val wordCase: CaseTypeUiModel,
)

@Serializable
enum class PasswordGeneratorTypeUiModel {
    PASSWORD,
    PASSPHRASE,
}

@Serializable
enum class CaseTypeUiModel {
    LOWERCASE,
    UPPERCASE,
    CAMELCASE,
}
