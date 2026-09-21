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

package com.passbolt.mobile.android.data.groups

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.core.architecture.result.DomainResult.Incomplete.Error.Reason.UNKNOWN
import com.passbolt.mobile.android.domain.groups.datasource.GroupsLocalDataSource
import com.passbolt.mobile.android.domain.groups.datasource.GroupsRemoteDataSource
import com.passbolt.mobile.android.domain.groups.model.Group
import com.passbolt.mobile.android.domain.groups.model.GroupMember
import com.passbolt.mobile.android.domain.groups.model.GroupWithMembers
import kotlinx.coroutines.test.runTest
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

class GroupsRepositoryImplTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single<GroupsLocalDataSource> { mock<GroupsLocalDataSource>() }
                        single<GroupsRemoteDataSource> { mock<GroupsRemoteDataSource>() }
                        factory {
                            GroupsRepositoryImpl(
                                localDataSource = get(),
                                remoteDataSource = get(),
                            )
                        }
                    },
                ),
            )
        }

    private lateinit var local: GroupsLocalDataSource
    private lateinit var remote: GroupsRemoteDataSource
    private lateinit var repository: GroupsRepositoryImpl
    private val groupsWithMembers =
        listOf(
            GroupWithMembers(
                group = Group(id = "group-id", name = "group-name"),
                members = listOf(GroupMember(userId = "member-id")),
            ),
        )

    @Before
    fun setUp() {
        local = get()
        remote = get()
        repository = get()
    }

    @Test
    fun `refreshGroups with remote success returns success and writes to local`() =
        runTest {
            remote.stub { on { getGroups() }.thenReturn(DomainResult.Finished(groupsWithMembers)) }

            val result = repository.refreshGroups(USER_ID)

            assertThat(result).isEqualTo(DomainResult.Finished(groupsWithMembers))
            verify(local).upsertGroups(groupsWithMembers, USER_ID)
        }

    @Test
    fun `refreshGroups with remote failure returns failure and does not write to local`() =
        runTest {
            val failure = DomainResult.Incomplete.Error(UNKNOWN, "boom")
            remote.stub { on { getGroups() }.thenReturn(failure) }

            val result = repository.refreshGroups(USER_ID)

            assertThat(result).isEqualTo(failure)
            verify(local, never()).upsertGroups(groupsWithMembers, USER_ID)
        }

    private companion object {
        const val USER_ID = "user-id"
    }
}
