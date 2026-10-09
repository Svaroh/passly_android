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

package net.svaroh.passly.data.users

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.architecture.result.DomainResult.Incomplete.Error.Reason.UNKNOWN
import net.svaroh.passly.domain.users.UsersDataSource
import net.svaroh.passly.domain.users.UsersLocalDataSource
import net.svaroh.passly.domain.users.model.UserProfile
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

class UsersRepositoryImplTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single<UsersLocalDataSource> { mock<UsersLocalDataSource>() }
                        single<UsersDataSource> { mock<UsersDataSource>() }
                        factory {
                            UsersRepositoryImpl(
                                localDataSource = get(),
                                remoteDataSource = get(),
                            )
                        }
                    },
                ),
            )
        }

    private lateinit var local: UsersLocalDataSource
    private lateinit var remote: UsersDataSource
    private lateinit var repository: UsersRepositoryImpl
    private val users =
        listOf(
            UserProfile(
                id = "user-id",
                username = "username",
                disabled = false,
                role = null,
                firstName = null,
                lastName = null,
                avatarUrl = null,
                gpgKey = null,
            ),
        )

    @Before
    fun setUp() {
        local = get()
        remote = get()
        repository = get()
    }

    @Test
    fun `refreshUsers with remote success returns success and writes to local`() =
        runTest {
            remote.stub { on { getUsers() }.thenReturn(DomainResult.Finished(users)) }

            val result = repository.refreshUsers(SELECTED_ACCOUNT_ID)

            assertThat(result).isEqualTo(DomainResult.Finished(users))
            verify(local).upsertUsers(SELECTED_ACCOUNT_ID, users)
        }

    @Test
    fun `refreshUsers with remote failure returns failure and does not write to local`() =
        runTest {
            val failure = DomainResult.Incomplete.Error(UNKNOWN, "boom")
            remote.stub { on { getUsers() }.thenReturn(failure) }

            val result = repository.refreshUsers(SELECTED_ACCOUNT_ID)

            assertThat(result).isEqualTo(failure)
            verify(local, never()).upsertUsers(SELECTED_ACCOUNT_ID, users)
        }

    private companion object {
        const val SELECTED_ACCOUNT_ID = "selected-account-id"
    }
}
