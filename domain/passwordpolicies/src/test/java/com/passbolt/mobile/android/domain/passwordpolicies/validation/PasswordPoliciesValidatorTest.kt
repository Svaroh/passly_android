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

import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.domain.passwordpolicies.model.PasswordPolicies
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get

class PasswordPoliciesValidatorTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        singleOf(::PasswordPoliciesValidator)
                    },
                ),
            )
        }

    private lateinit var validator: PasswordPoliciesValidator
    private val defaults = PasswordPolicies.defaults()

    @Before
    fun setUp() {
        validator = get()
    }

    @Test
    fun `valid defaults pass`() {
        assertThat(validator.arePasswordPoliciesValid(defaults)).isTrue()
    }

    @Test
    fun `password length below minimum fails`() {
        assertThat(validator.arePasswordPoliciesValid(defaults.withPasswordLength(7))).isFalse()
    }

    @Test
    fun `password length above maximum fails`() {
        assertThat(validator.arePasswordPoliciesValid(defaults.withPasswordLength(129))).isFalse()
    }

    @Test
    fun `password length at boundaries passes`() {
        assertThat(validator.arePasswordPoliciesValid(defaults.withPasswordLength(8))).isTrue()
        assertThat(validator.arePasswordPoliciesValid(defaults.withPasswordLength(128))).isTrue()
    }

    @Test
    fun `passphrase words below minimum fails`() {
        assertThat(validator.arePasswordPoliciesValid(defaults.withPassphraseWords(3))).isFalse()
    }

    @Test
    fun `passphrase words above maximum fails`() {
        assertThat(validator.arePasswordPoliciesValid(defaults.withPassphraseWords(41))).isFalse()
    }

    @Test
    fun `passphrase words at boundaries pass`() {
        assertThat(validator.arePasswordPoliciesValid(defaults.withPassphraseWords(4))).isTrue()
        assertThat(validator.arePasswordPoliciesValid(defaults.withPassphraseWords(40))).isTrue()
    }

    @Test
    fun `no password mask set fails`() {
        assertThat(validator.arePasswordPoliciesValid(defaults.withAllMasksDisabled())).isFalse()
    }

    private fun PasswordPolicies.withPasswordLength(length: Int) =
        copy(passwordGeneratorSettings = passwordGeneratorSettings.copy(length = length))

    private fun PasswordPolicies.withPassphraseWords(words: Int) =
        copy(passphraseGeneratorSettings = passphraseGeneratorSettings.copy(words = words))

    private fun PasswordPolicies.withAllMasksDisabled() =
        copy(
            passwordGeneratorSettings =
                passwordGeneratorSettings.copy(
                    maskUpper = false,
                    maskLower = false,
                    maskDigit = false,
                    maskParenthesis = false,
                    maskEmoji = false,
                    maskChar1 = false,
                    maskChar2 = false,
                    maskChar3 = false,
                    maskChar4 = false,
                    maskChar5 = false,
                ),
        )
}
