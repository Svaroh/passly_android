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

package net.svaroh.passly.domain.folders.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.architecture.result.DomainResult.Incomplete.Error.Reason.OFFLINE
import net.svaroh.passly.core.mvp.authentication.AuthenticationState
import net.svaroh.passly.domain.folders.FoldersRepository
import net.svaroh.passly.domain.folders.model.FolderModel
import net.svaroh.passly.domain.folders.model.FolderModelWithAttributes
import net.svaroh.passly.domain.folders.model.FoldersPage
import net.svaroh.passly.ui.ResourcePermission
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.koin.test.inject
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import java.time.ZonedDateTime
import kotlin.test.assertIs

@ExperimentalCoroutinesApi
class GetFoldersPaginatedUseCaseTest : KoinTest {
    private val useCase: GetFoldersPaginatedUseCase by inject()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mock<FoldersRepository>() }
                    singleOf(::GetFoldersPaginatedUseCase)
                },
            )
        }

    @Test
    fun `should return success with folders and total count`() =
        runTest {
            stubRepository(DomainResult.Finished(FoldersPage(folders = listOf(FOLDER_WITH_ATTRIBUTES), totalCount = 10)))

            val result = useCase.execute(GetFoldersPaginatedUseCase.Input(page = 1, limit = 2000))

            val success = assertIs<GetFoldersPaginatedUseCase.Output.Success>(result)
            assertThat(success.totalCount).isEqualTo(10)
            assertThat(success.folders).hasSize(1)
            assertThat(success.authenticationState).isEqualTo(AuthenticationState.Authenticated)
        }

    @Test
    fun `should return authenticated failure on offline error`() =
        runTest {
            stubRepository(DomainResult.Incomplete.Error(OFFLINE, null))

            val result = useCase.execute(GetFoldersPaginatedUseCase.Input(page = 1, limit = 2000))

            assertIs<GetFoldersPaginatedUseCase.Output.Failure>(result)
            assertThat(result.authenticationState).isEqualTo(AuthenticationState.Authenticated)
        }

    @Test
    fun `should return session unauthenticated on unauthorized`() =
        runTest {
            stubRepository(DomainResult.Incomplete.Unauthorized)

            val result = useCase.execute(GetFoldersPaginatedUseCase.Input(page = 1, limit = 2000))

            assertIs<GetFoldersPaginatedUseCase.Output.Failure>(result)
            val unauthenticated = assertIs<AuthenticationState.Unauthenticated>(result.authenticationState)
            assertIs<AuthenticationState.Unauthenticated.Reason.Session>(unauthenticated.reason)
        }

    @Test
    fun `should return passphrase unauthenticated when passphrase not in cache`() =
        runTest {
            stubRepository(DomainResult.Incomplete.PassphraseNotInCache)

            val result = useCase.execute(GetFoldersPaginatedUseCase.Input(page = 1, limit = 2000))

            assertIs<GetFoldersPaginatedUseCase.Output.Failure>(result)
            val unauthenticated = assertIs<AuthenticationState.Unauthenticated>(result.authenticationState)
            assertIs<AuthenticationState.Unauthenticated.Reason.Passphrase>(unauthenticated.reason)
        }

    @Test
    fun `should return mfa unauthenticated when mfa is required`() =
        runTest {
            stubRepository(DomainResult.Incomplete.MfaRequired(listOf(null)))

            val result = useCase.execute(GetFoldersPaginatedUseCase.Input(page = 1, limit = 2000))

            assertIs<GetFoldersPaginatedUseCase.Output.Failure>(result)
            val unauthenticated = assertIs<AuthenticationState.Unauthenticated>(result.authenticationState)
            assertIs<AuthenticationState.Unauthenticated.Reason.Mfa>(unauthenticated.reason)
        }

    private fun stubRepository(result: DomainResult<FoldersPage>) {
        get<FoldersRepository>().stub {
            on { getFoldersPage(any(), any()) }.doReturn(result)
        }
    }

    private companion object {
        private val FOLDER_WITH_ATTRIBUTES =
            FolderModelWithAttributes(
                folderModel =
                    FolderModel(
                        folderId = "folderId",
                        parentFolderId = null,
                        name = "Test Folder",
                        isShared = false,
                        permission = ResourcePermission.READ,
                        modified = ZonedDateTime.now(),
                    ),
                folderPermissions = emptyList(),
            )
    }
}
