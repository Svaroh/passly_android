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
import net.svaroh.passly.ui.ParsedStructure
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class FillableInputsFinderTest {
    private val autofillHintsFactory = mock<AutofillHintsFactory>()
    private lateinit var finder: FillableInputsFinder

    @Before
    fun setUp() {
        whenever(autofillHintsFactory.getHintValues(AutofillField.TOTP))
            .thenReturn(arrayOf("otp", "totp", "one-time-code"))
        whenever(autofillHintsFactory.getHintValues(AutofillField.USERNAME))
            .thenReturn(arrayOf("username"))
        whenever(autofillHintsFactory.getHintValues(AutofillField.PASSWORD))
            .thenReturn(arrayOf("password"))

        finder = FillableInputsFinder(autofillHintsFactory)
    }

    @Test
    fun `finds structure matching TOTP hint`() {
        val totpStructure =
            ParsedStructure(
                id = mock<AutofillId>(),
                autofillHints = listOf("one-time-code"),
                domain = null,
            )
        val otherStructure =
            ParsedStructure(
                id = mock<AutofillId>(),
                autofillHints = listOf("username"),
                domain = null,
            )

        val result =
            finder.findStructureForAutofillFields(
                AutofillField.TOTP,
                setOf(otherStructure, totpStructure),
            )

        assertThat(result).isEqualTo(totpStructure)
    }

    @Test
    fun `finds structure matching TOTP hint with partial match`() {
        val totpStructure =
            ParsedStructure(
                id = mock<AutofillId>(),
                autofillHints = listOf("enter-otp-here"),
                domain = null,
            )

        val result =
            finder.findStructureForAutofillFields(
                AutofillField.TOTP,
                setOf(totpStructure),
            )

        assertThat(result).isEqualTo(totpStructure)
    }

    @Test
    fun `returns null when no TOTP structure matches`() {
        val passwordStructure =
            ParsedStructure(
                id = mock<AutofillId>(),
                autofillHints = listOf("password"),
                domain = null,
            )

        val result =
            finder.findStructureForAutofillFields(
                AutofillField.TOTP,
                setOf(passwordStructure),
            )

        assertThat(result).isNull()
    }

    @Test
    fun `returns null when hints are empty`() {
        val emptyHintsStructure =
            ParsedStructure(
                id = mock<AutofillId>(),
                autofillHints = emptyList(),
                domain = null,
            )

        val result =
            finder.findStructureForAutofillFields(
                AutofillField.TOTP,
                setOf(emptyHintsStructure),
            )

        assertThat(result).isNull()
    }

    @Test
    fun `returns null when hints are null`() {
        val nullHintsStructure =
            ParsedStructure(
                id = mock<AutofillId>(),
                autofillHints = null,
                domain = null,
            )

        val result =
            finder.findStructureForAutofillFields(
                AutofillField.TOTP,
                setOf(nullHintsStructure),
            )

        assertThat(result).isNull()
    }

    @Test
    fun `requires domain set in browser mode for TOTP`() {
        val totpWithDomain =
            ParsedStructure(
                id = mock<AutofillId>(),
                autofillHints = listOf("otp"),
                domain = "example.com",
            )
        val totpWithoutDomain =
            ParsedStructure(
                id = mock<AutofillId>(),
                autofillHints = listOf("otp"),
                domain = null,
            )
        // In browser mode (some structure has domain set), only structures with domains match
        val browserIndicator =
            ParsedStructure(
                id = mock<AutofillId>(),
                autofillHints = listOf("username"),
                domain = "example.com",
            )

        val resultWithDomain =
            finder.findStructureForAutofillFields(
                AutofillField.TOTP,
                setOf(browserIndicator, totpWithDomain),
            )
        assertThat(resultWithDomain).isEqualTo(totpWithDomain)

        val resultWithoutDomain =
            finder.findStructureForAutofillFields(
                AutofillField.TOTP,
                setOf(browserIndicator, totpWithoutDomain),
            )
        assertThat(resultWithoutDomain).isNull()
    }

    @Test
    fun `does not require domain in app mode`() {
        val totpStructure =
            ParsedStructure(
                id = mock<AutofillId>(),
                autofillHints = listOf("totp"),
                domain = null,
            )

        val result =
            finder.findStructureForAutofillFields(
                AutofillField.TOTP,
                setOf(totpStructure),
            )

        assertThat(result).isEqualTo(totpStructure)
    }

    @Test
    fun `finds username structure`() {
        val usernameStructure =
            ParsedStructure(
                id = mock<AutofillId>(),
                autofillHints = listOf("username"),
                domain = null,
            )

        val result =
            finder.findStructureForAutofillFields(
                AutofillField.USERNAME,
                setOf(usernameStructure),
            )

        assertThat(result).isEqualTo(usernameStructure)
    }

    @Test
    fun `finds password structure`() {
        val passwordStructure =
            ParsedStructure(
                id = mock<AutofillId>(),
                autofillHints = listOf("password"),
                domain = null,
            )

        val result =
            finder.findStructureForAutofillFields(
                AutofillField.PASSWORD,
                setOf(passwordStructure),
            )

        assertThat(result).isEqualTo(passwordStructure)
    }
}
