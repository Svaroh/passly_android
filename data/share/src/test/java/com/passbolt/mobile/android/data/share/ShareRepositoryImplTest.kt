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

package net.svaroh.passly.data.share

import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.domain.share.ShareDataSource
import net.svaroh.passly.domain.share.model.EncryptedSecret
import net.svaroh.passly.domain.share.model.ShareChanges
import net.svaroh.passly.domain.share.model.SharePermission
import net.svaroh.passly.domain.share.model.ShareRecipient
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
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

class ShareRepositoryImplTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single<ShareDataSource> { mock<ShareDataSource>() }
                    factory { ShareRepositoryImpl(remoteDataSource = get()) }
                },
            )
        }

    private lateinit var remote: ShareDataSource
    private lateinit var repository: ShareRepositoryImpl

    @Before
    fun setUp() {
        remote = get()
        repository = get()
    }

    @Test
    fun `simulateShareResource delegates to remote and returns its result`() =
        runTest {
            val permissions = listOf(newPermission())
            val expected =
                DomainResult.Finished(ShareChanges(added = listOf(ShareRecipient("u1")), removed = emptyList()))
            remote.stub { on { simulateShareResource(RESOURCE_ID, permissions) }.thenReturn(expected) }

            val result = repository.simulateShareResource(RESOURCE_ID, permissions)

            assertThat(result).isEqualTo(expected)
            verify(remote).simulateShareResource(RESOURCE_ID, permissions)
        }

    @Test
    fun `shareResource delegates to remote and returns its result`() =
        runTest {
            val permissions = listOf(newPermission())
            val secrets = listOf(EncryptedSecret(RESOURCE_ID, "u1", "cipher"))
            remote.stub {
                on { shareResource(RESOURCE_ID, permissions, secrets) }.thenReturn(DomainResult.Finished(Unit))
            }

            val result = repository.shareResource(RESOURCE_ID, permissions, secrets)

            assertThat(result).isEqualTo(DomainResult.Finished(Unit))
            verify(remote).shareResource(RESOURCE_ID, permissions, secrets)
        }

    @Test
    fun `shareFolder delegates to remote and returns its result`() =
        runTest {
            val permissions = listOf(newPermission())
            remote.stub { on { shareFolder(FOLDER_ID, permissions) }.thenReturn(DomainResult.Finished(Unit)) }

            val result = repository.shareFolder(FOLDER_ID, permissions)

            assertThat(result).isEqualTo(DomainResult.Finished(Unit))
            verify(remote).shareFolder(FOLDER_ID, permissions)
        }

    private companion object {
        private const val RESOURCE_ID = "resource-id"
        private const val FOLDER_ID = "folder-id"

        private fun newPermission() =
            SharePermission.NewSharePermission(
                aro = "User",
                aroForeignKey = "user-id",
                aco = "Resource",
                acoForeignKey = "resource-id",
                type = 1,
            )
    }
}
