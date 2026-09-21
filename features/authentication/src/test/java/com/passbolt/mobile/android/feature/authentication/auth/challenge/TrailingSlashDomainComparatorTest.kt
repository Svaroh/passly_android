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

package com.passbolt.mobile.android.feature.authentication.auth.challenge

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TrailingSlashDomainComparatorTest {
    private val domainComparator: DomainComparator = TrailingSlashDomainComparator()

    @Test
    fun `identical domains match`() {
        val result = domainComparator.matches("https://passbolt.dev", "https://passbolt.dev")

        assertThat(result).isTrue()
    }

    @Test
    fun `trailing slash on the first domain only is ignored`() {
        val result = domainComparator.matches("https://passbolt.dev/", "https://passbolt.dev")

        assertThat(result).isTrue()
    }

    @Test
    fun `trailing slash on the second domain only is ignored`() {
        val result = domainComparator.matches("https://passbolt.dev", "https://passbolt.dev/")

        assertThat(result).isTrue()
    }

    @Test
    fun `trailing slashes on both domains are ignored`() {
        val result = domainComparator.matches("https://passbolt.dev/", "https://passbolt.dev/")

        assertThat(result).isTrue()
    }

    @Test
    fun `multiple trailing slashes are ignored`() {
        val result = domainComparator.matches("https://passbolt.dev///", "https://passbolt.dev")

        assertThat(result).isTrue()
    }

    @Test
    fun `domains with the same path match ignoring trailing slash`() {
        val result = domainComparator.matches("https://passbolt.dev/tenant/", "https://passbolt.dev/tenant")

        assertThat(result).isTrue()
    }

    @Test
    fun `different hosts do not match`() {
        val result = domainComparator.matches("https://attacker.dev", "https://passbolt.dev")

        assertThat(result).isFalse()
    }

    @Test
    fun `different schemes do not match`() {
        val result = domainComparator.matches("http://passbolt.dev", "https://passbolt.dev")

        assertThat(result).isFalse()
    }

    @Test
    fun `domain extended with an attacker suffix does not match`() {
        val result = domainComparator.matches("https://passbolt.dev.attacker.com", "https://passbolt.dev")

        assertThat(result).isFalse()
    }

    @Test
    fun `trailing path segment is not treated as a trailing slash`() {
        val result = domainComparator.matches("https://passbolt.dev/tenant", "https://passbolt.dev")

        assertThat(result).isFalse()
    }

    @Test
    fun `domains differing only in letter case do not match`() {
        val result = domainComparator.matches("https://PASSBOLT.dev", "https://passbolt.dev")

        assertThat(result).isFalse()
    }
}
