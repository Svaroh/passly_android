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

package net.svaroh.passly.feature.autofill.resources.datasetstrategy

import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.core.autofill.accessibility.AccessibilityCommunicator
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ReturnAccessibilityDatasetTest {
    private val autofillCallback = mock<AutofillCallback>()
    private val accessibilityCommunicator = AccessibilityCommunicator()
    private val strategy = ReturnAccessibilityDataset(autofillCallback, accessibilityCommunicator)

    @Test
    fun `stashes credentials-only payload and finishes`() {
        val payload =
            AutofillPayload(
                username = "alice",
                password = "s3cret",
                totpCode = null,
                uri = "https://example.com",
            )

        strategy.returnDataset(payload)

        val lastFill = accessibilityCommunicator.lastFill
        assertThat(lastFill).isNotNull()
        assertThat(lastFill!!.username).isEqualTo("alice")
        assertThat(lastFill.password).isEqualTo("s3cret")
        assertThat(lastFill.totpCode).isNull()
        assertThat(lastFill.uri).isEqualTo("https://example.com")
        verify(autofillCallback).finishAutofill()
    }

    @Test
    fun `stashes TOTP-only payload and finishes`() {
        val payload =
            AutofillPayload(
                username = null,
                password = null,
                totpCode = "123456",
                uri = "https://example.com",
            )

        strategy.returnDataset(payload)

        val lastFill = accessibilityCommunicator.lastFill
        assertThat(lastFill).isNotNull()
        assertThat(lastFill!!.username).isNull()
        assertThat(lastFill.password).isNull()
        assertThat(lastFill.totpCode).isEqualTo("123456")
        assertThat(lastFill.uri).isEqualTo("https://example.com")
        verify(autofillCallback).finishAutofill()
    }

    @Test
    fun `stashes combined payload and finishes`() {
        val payload =
            AutofillPayload(
                username = "alice",
                password = "s3cret",
                totpCode = "123456",
                uri = "https://example.com",
            )

        strategy.returnDataset(payload)

        val lastFill = accessibilityCommunicator.lastFill
        assertThat(lastFill).isNotNull()
        assertThat(lastFill!!.username).isEqualTo("alice")
        assertThat(lastFill.password).isEqualTo("s3cret")
        assertThat(lastFill.totpCode).isEqualTo("123456")
        assertThat(lastFill.uri).isEqualTo("https://example.com")
        verify(autofillCallback).finishAutofill()
    }

    @Test
    fun `forwards null uri from payload`() {
        val payload =
            AutofillPayload(
                username = null,
                password = null,
                totpCode = "123456",
                uri = null,
            )

        strategy.returnDataset(payload)

        val lastFill = accessibilityCommunicator.lastFill
        assertThat(lastFill?.uri).isNull()
    }
}
