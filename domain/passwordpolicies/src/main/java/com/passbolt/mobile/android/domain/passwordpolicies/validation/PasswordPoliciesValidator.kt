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

package net.svaroh.passly.domain.passwordpolicies.validation

import net.svaroh.passly.domain.passwordpolicies.model.PassphraseGeneratorSettings
import net.svaroh.passly.domain.passwordpolicies.model.PasswordGeneratorSettings
import net.svaroh.passly.domain.passwordpolicies.model.PasswordPolicies

class PasswordPoliciesValidator {
    fun arePasswordPoliciesValid(passwordPolicies: PasswordPolicies) =
        isPasswordLengthValid(passwordPolicies.passwordGeneratorSettings.length) &&
            isAtLeastOnePasswordMaskSet(passwordPolicies.passwordGeneratorSettings) &&
            isPassphraseWordCountValid(passwordPolicies.passphraseGeneratorSettings)

    private fun isPassphraseWordCountValid(passphraseGeneratorSettings: PassphraseGeneratorSettings) =
        passphraseGeneratorSettings.words in PASSPHRASE_GEN_MIN_WORDS..PASSPHRASE_GEN_MAX_WORDS

    private fun isAtLeastOnePasswordMaskSet(passwordGeneratorSettings: PasswordGeneratorSettings) =
        listOf(
            passwordGeneratorSettings.maskChar1,
            passwordGeneratorSettings.maskChar2,
            passwordGeneratorSettings.maskChar3,
            passwordGeneratorSettings.maskChar4,
            passwordGeneratorSettings.maskChar5,
            passwordGeneratorSettings.maskEmoji,
            passwordGeneratorSettings.maskDigit,
            passwordGeneratorSettings.maskParenthesis,
            passwordGeneratorSettings.maskLower,
            passwordGeneratorSettings.maskUpper,
        ).any { it }

    private fun isPasswordLengthValid(passwordLength: Int) =
        passwordLength in PASSWORD_GEN_MIN_PASSWORD_LENGTH..PASSWORD_GEN_MAX_PASSWORD_LENGTH

    private companion object {
        const val PASSWORD_GEN_MIN_PASSWORD_LENGTH = 8
        const val PASSWORD_GEN_MAX_PASSWORD_LENGTH = 128
        const val PASSPHRASE_GEN_MIN_WORDS = 4
        const val PASSPHRASE_GEN_MAX_WORDS = 40
    }
}
