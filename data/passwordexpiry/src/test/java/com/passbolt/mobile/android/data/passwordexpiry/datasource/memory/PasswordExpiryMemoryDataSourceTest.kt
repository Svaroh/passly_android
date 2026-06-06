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

package net.svaroh.passly.data.passwordexpiry.datasource.memory

import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.domain.passwordexpiry.model.PasswordExpirySettings
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PasswordExpiryMemoryDataSourceTest {
    private val dataSource = PasswordExpiryMemoryDataSource()
    private val settingsA = PasswordExpirySettings.defaults()
    private val settingsB =
        PasswordExpirySettings(
            automaticExpiry = false,
            automaticUpdate = false,
            defaultExpiryPeriodDays = 30,
        )

    @Test
    fun `get on an empty cache is a miss`() =
        runTest {
            assertThat(dataSource.getPasswordExpirySettings(USER_A)).isEqualTo(DomainResult.Incomplete.NotCached)
        }

    @Test
    fun `cached value is returned for the same account`() =
        runTest {
            dataSource.setPasswordExpirySettings(USER_A, settingsA)

            assertThat(dataSource.getPasswordExpirySettings(USER_A)).isEqualTo(DomainResult.Finished(settingsA))
        }

    @Test
    fun `cached value of one account is not served to another account`() =
        runTest {
            dataSource.setPasswordExpirySettings(USER_A, settingsA)

            assertThat(dataSource.getPasswordExpirySettings(USER_B)).isEqualTo(DomainResult.Incomplete.NotCached)
        }

    @Test
    fun `each account retains its own cached value`() =
        runTest {
            dataSource.setPasswordExpirySettings(USER_A, settingsA)
            dataSource.setPasswordExpirySettings(USER_B, settingsB)

            assertThat(dataSource.getPasswordExpirySettings(USER_A)).isEqualTo(DomainResult.Finished(settingsA))
            assertThat(dataSource.getPasswordExpirySettings(USER_B)).isEqualTo(DomainResult.Finished(settingsB))
        }

    private companion object {
        const val USER_A = "userA"
        const val USER_B = "userB"
    }
}
