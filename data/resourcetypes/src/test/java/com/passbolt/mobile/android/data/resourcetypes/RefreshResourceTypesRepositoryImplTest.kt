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

package net.svaroh.passly.data.resourcetypes

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.architecture.result.DomainResult.Incomplete.Error.Reason.UNKNOWN
import net.svaroh.passly.domain.resourcetypes.ResourceTypesLocalDataSource
import net.svaroh.passly.domain.resourcetypes.ResourceTypesRemoteDataSource
import net.svaroh.passly.domain.resourcetypes.model.ResourceType
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
import java.util.UUID

class RefreshResourceTypesRepositoryImplTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single<ResourceTypesLocalDataSource> { mock<ResourceTypesLocalDataSource>() }
                        single<ResourceTypesRemoteDataSource> { mock<ResourceTypesRemoteDataSource>() }
                        factory {
                            RefreshResourceTypesRepositoryImpl(
                                localDataSource = get(),
                                remoteDataSource = get(),
                            )
                        }
                    },
                ),
            )
        }

    private lateinit var local: ResourceTypesLocalDataSource
    private lateinit var remote: ResourceTypesRemoteDataSource
    private lateinit var repository: RefreshResourceTypesRepositoryImpl

    private val resourceTypes =
        listOf(
            ResourceType(
                id = UUID.fromString("669f8c64-242a-59fb-92fc-81e660052e60"),
                slug = "password-and-description",
                name = "Password and description",
                deleted = null,
            ),
        )

    @Before
    fun setUp() {
        local = get()
        remote = get()
        repository = get()
    }

    @Test
    fun `refreshResourceTypes with remote success returns success and writes to local`() =
        runTest {
            remote.stub { on { getResourceTypes() }.thenReturn(DomainResult.Finished(resourceTypes)) }

            val result = repository.refreshResourceTypes(USER_ID)

            assertThat(result).isEqualTo(DomainResult.Finished(resourceTypes))
            verify(local).setResourceTypes(USER_ID, resourceTypes)
        }

    @Test
    fun `refreshResourceTypes with remote failure returns failure and does not write to local`() =
        runTest {
            val failure = DomainResult.Incomplete.Error(UNKNOWN, "boom")
            remote.stub { on { getResourceTypes() }.thenReturn(failure) }

            val result = repository.refreshResourceTypes(USER_ID)

            assertThat(result).isEqualTo(failure)
            verify(local, never()).setResourceTypes(USER_ID, resourceTypes)
        }

    private companion object {
        const val USER_ID = "user-id"
    }
}
