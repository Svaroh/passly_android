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

package net.svaroh.passly.data.passwordpolicies.datasource.memory

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.domain.passwordpolicies.model.PasswordPolicies
import org.junit.Test

class PasswordPoliciesMemoryDataSourceTest {
    private val dataSource = PasswordPoliciesMemoryDataSource()
    private val defaultPolicies = PasswordPolicies.defaults()
    private val policiesWithDisabledExternalCheck = defaultPolicies.copy(isExternalDictionaryCheckEnabled = false)

    @Test
    fun `get on an empty cache is a miss`() =
        runTest {
            assertThat(dataSource.getPasswordPolicies(USER_A)).isEqualTo(DomainResult.Incomplete.NotCached)
        }

    @Test
    fun `cached value is returned for the same account`() =
        runTest {
            dataSource.setPasswordPolicies(USER_A, defaultPolicies)

            assertThat(dataSource.getPasswordPolicies(USER_A)).isEqualTo(DomainResult.Finished(defaultPolicies))
        }

    @Test
    fun `cached value of one account is not served to another account`() =
        runTest {
            dataSource.setPasswordPolicies(USER_A, defaultPolicies)

            assertThat(dataSource.getPasswordPolicies(USER_B)).isEqualTo(DomainResult.Incomplete.NotCached)
        }

    @Test
    fun `each account retains its own cached value`() =
        runTest {
            dataSource.setPasswordPolicies(USER_A, defaultPolicies)
            dataSource.setPasswordPolicies(USER_B, policiesWithDisabledExternalCheck)

            assertThat(dataSource.getPasswordPolicies(USER_A)).isEqualTo(DomainResult.Finished(defaultPolicies))
            assertThat(dataSource.getPasswordPolicies(USER_B))
                .isEqualTo(DomainResult.Finished(policiesWithDisabledExternalCheck))
        }

    private companion object {
        const val USER_A = "userA"
        const val USER_B = "userB"
    }
}
