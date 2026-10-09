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

package net.svaroh.passly.domain.passwordpolicies.model

data class PasswordPolicies(
    val defaultGenerator: PasswordGeneratorType,
    val passwordGeneratorSettings: PasswordGeneratorSettings,
    val passphraseGeneratorSettings: PassphraseGeneratorSettings,
    val isExternalDictionaryCheckEnabled: Boolean,
) {
    companion object {
        fun defaults(): PasswordPolicies =
            PasswordPolicies(
                defaultGenerator = PasswordGeneratorType.PASSWORD,
                passwordGeneratorSettings =
                    PasswordGeneratorSettings(
                        length = 18,
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
                passphraseGeneratorSettings =
                    PassphraseGeneratorSettings(
                        words = 9,
                        wordSeparator = " ",
                        wordCase = CaseType.LOWERCASE,
                    ),
                isExternalDictionaryCheckEnabled = true,
            )
    }
}

data class PasswordGeneratorSettings(
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

data class PassphraseGeneratorSettings(
    val words: Int,
    val wordSeparator: String,
    val wordCase: CaseType,
)

enum class PasswordGeneratorType {
    PASSWORD,
    PASSPHRASE,
}

enum class CaseType {
    LOWERCASE,
    UPPERCASE,
    CAMELCASE,
}
