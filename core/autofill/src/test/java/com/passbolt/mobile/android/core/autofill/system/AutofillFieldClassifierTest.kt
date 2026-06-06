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

package net.svaroh.passly.core.autofill.system

import android.view.autofill.AutofillId
import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.core.autofill.system.classification.AutofillFieldClassifier
import net.svaroh.passly.core.autofill.system.classification.FillClassification
import net.svaroh.passly.ui.ParsedStructure
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AutofillFieldClassifierTest {
    private val fillableInputsFinder = mock<FillableInputsFinder>()
    private lateinit var classifier: AutofillFieldClassifier

    @Before
    fun setUp() {
        classifier = AutofillFieldClassifier(fillableInputsFinder)
    }

    @Test
    fun `classifies as Credentials when only username and password found`() {
        val usernameStructure = createParsedStructure(domain = "example.com")
        val passwordStructure = createParsedStructure(domain = "example.com")

        whenever(
            fillableInputsFinder.findStructureForAutofillFields(eq(AutofillField.USERNAME), any()),
        ).thenReturn(usernameStructure)
        whenever(
            fillableInputsFinder.findStructureForAutofillFields(eq(AutofillField.PASSWORD), any()),
        ).thenReturn(passwordStructure)
        whenever(
            fillableInputsFinder.findStructureForAutofillFields(eq(AutofillField.TOTP), any()),
        ).thenReturn(null)

        val result = classifier.classifyFill(emptySet())

        assertThat(result).isEqualTo(
            FillClassification.Credentials(
                username = usernameStructure,
                password = passwordStructure,
            ),
        )
    }

    @Test
    fun `classifies as Totp when only totp found`() {
        val totpStructure = createParsedStructure(domain = "example.com")

        whenever(
            fillableInputsFinder.findStructureForAutofillFields(eq(AutofillField.USERNAME), any()),
        ).thenReturn(null)
        whenever(
            fillableInputsFinder.findStructureForAutofillFields(eq(AutofillField.PASSWORD), any()),
        ).thenReturn(null)
        whenever(
            fillableInputsFinder.findStructureForAutofillFields(eq(AutofillField.TOTP), any()),
        ).thenReturn(totpStructure)

        val result = classifier.classifyFill(emptySet())

        assertThat(result).isEqualTo(FillClassification.Totp(totp = totpStructure))
    }

    @Test
    fun `classifies as CredentialsAndTotp when username + password + TOTP found on same domain`() {
        val usernameStructure = createParsedStructure(domain = "example.com")
        val passwordStructure = createParsedStructure(domain = "example.com")
        val totpStructure = createParsedStructure(domain = "example.com")

        whenever(
            fillableInputsFinder.findStructureForAutofillFields(eq(AutofillField.USERNAME), any()),
        ).thenReturn(usernameStructure)
        whenever(
            fillableInputsFinder.findStructureForAutofillFields(eq(AutofillField.PASSWORD), any()),
        ).thenReturn(passwordStructure)
        whenever(
            fillableInputsFinder.findStructureForAutofillFields(eq(AutofillField.TOTP), any()),
        ).thenReturn(totpStructure)

        val result = classifier.classifyFill(emptySet())

        assertThat(result).isEqualTo(
            FillClassification.CredentialsAndTotp(
                username = usernameStructure,
                password = passwordStructure,
                totp = totpStructure,
            ),
        )
    }

    @Test
    fun `falls back to Credentials when totp on different domain`() {
        val usernameStructure = createParsedStructure(domain = "example.com")
        val passwordStructure = createParsedStructure(domain = "example.com")
        val totpStructure = createParsedStructure(domain = "other.com")

        whenever(
            fillableInputsFinder.findStructureForAutofillFields(eq(AutofillField.USERNAME), any()),
        ).thenReturn(usernameStructure)
        whenever(
            fillableInputsFinder.findStructureForAutofillFields(eq(AutofillField.PASSWORD), any()),
        ).thenReturn(passwordStructure)
        whenever(
            fillableInputsFinder.findStructureForAutofillFields(eq(AutofillField.TOTP), any()),
        ).thenReturn(totpStructure)

        val result = classifier.classifyFill(emptySet())

        assertThat(result).isEqualTo(
            FillClassification.Credentials(
                username = usernameStructure,
                password = passwordStructure,
            ),
        )
    }

    @Test
    fun `returns null when no fields found`() {
        whenever(
            fillableInputsFinder.findStructureForAutofillFields(eq(AutofillField.USERNAME), any()),
        ).thenReturn(null)
        whenever(
            fillableInputsFinder.findStructureForAutofillFields(eq(AutofillField.PASSWORD), any()),
        ).thenReturn(null)
        whenever(
            fillableInputsFinder.findStructureForAutofillFields(eq(AutofillField.TOTP), any()),
        ).thenReturn(null)

        val result = classifier.classifyFill(emptySet())

        assertThat(result).isNull()
    }

    @Test
    fun `returns null when credentials on different domains`() {
        val usernameStructure = createParsedStructure(domain = "example.com")
        val passwordStructure = createParsedStructure(domain = "other.com")

        whenever(
            fillableInputsFinder.findStructureForAutofillFields(eq(AutofillField.USERNAME), any()),
        ).thenReturn(usernameStructure)
        whenever(
            fillableInputsFinder.findStructureForAutofillFields(eq(AutofillField.PASSWORD), any()),
        ).thenReturn(passwordStructure)
        whenever(
            fillableInputsFinder.findStructureForAutofillFields(eq(AutofillField.TOTP), any()),
        ).thenReturn(null)

        val result = classifier.classifyFill(emptySet())

        assertThat(result).isNull()
    }

    private fun createParsedStructure(domain: String?) =
        ParsedStructure(
            id = mock<AutofillId>(),
            autofillHints = emptyList(),
            domain = domain,
        )
}
