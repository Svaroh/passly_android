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

package com.passbolt.mobile.android.domain.permissionsconfirmation.usecase

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.core.architecture.result.DomainResult.Incomplete.Error.Reason.UNKNOWN
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountUseCase
import com.passbolt.mobile.android.domain.folders.usecase.FetchFolderPermissionsUseCase
import com.passbolt.mobile.android.domain.groups.model.Group
import com.passbolt.mobile.android.domain.groups.model.GroupMember
import com.passbolt.mobile.android.domain.groups.model.GroupWithMembers
import com.passbolt.mobile.android.domain.groups.usecase.FetchGroupsByIdsUseCase
import com.passbolt.mobile.android.domain.permissionsconfirmation.PermissionsSnapshotRepository
import com.passbolt.mobile.android.domain.users.model.UserProfile
import com.passbolt.mobile.android.domain.users.usecase.FetchUsersByIdsUseCase
import com.passbolt.mobile.android.ui.GroupModel
import com.passbolt.mobile.android.ui.PermissionModel
import com.passbolt.mobile.android.ui.ResourcePermission
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class CreatePermissionsSnapshotInteractorTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mock<FetchFolderPermissionsUseCase>() }
                    single { mock<FetchGroupsByIdsUseCase>() }
                    single { mock<FetchUsersByIdsUseCase>() }
                    single { mock<PermissionsSnapshotRepository>() }
                    single { mock<GetSelectedAccountUseCase>() }
                    singleOf(::PermissionsSnapshotComparator)
                    singleOf(::CreatePermissionsSnapshotInteractor)
                },
            )
        }

    private lateinit var fetchFolderPermissionsUseCase: FetchFolderPermissionsUseCase
    private lateinit var fetchGroupsByIdsUseCase: FetchGroupsByIdsUseCase
    private lateinit var fetchUsersByIdsUseCase: FetchUsersByIdsUseCase
    private lateinit var permissionsSnapshotRepository: PermissionsSnapshotRepository
    private lateinit var interactor: CreatePermissionsSnapshotInteractor

    @Before
    fun setUp() {
        fetchFolderPermissionsUseCase = get()
        fetchGroupsByIdsUseCase = get()
        fetchUsersByIdsUseCase = get()
        permissionsSnapshotRepository = get()
        get<GetSelectedAccountUseCase>().stub {
            on { execute(Unit) } doReturn GetSelectedAccountUseCase.Output(ACCOUNT_ID)
        }
        interactor = get()
    }

    @Test
    fun `snapshot contains permissions with group members and deduplicated users`() =
        runTest {
            val permissions = listOf(userPermission(USER_A), groupPermission(GROUP_ID))
            stubFolderPermissions(permissions)
            fetchGroupsByIdsUseCase.stub {
                onBlocking { execute(FetchGroupsByIdsUseCase.Input(listOf(GROUP_ID))) }
                    .thenReturn(
                        FetchGroupsByIdsUseCase.Output.Success(
                            listOf(groupWithMembers(GROUP_ID, USER_A, USER_B)),
                        ),
                    )
            }
            stubUsers(requestedIds = listOf(USER_A, USER_B), returnedProfiles = listOf(userProfile(USER_A), userProfile(USER_B)))

            val output = interactor.createForFolder(FOLDER_ID)

            val snapshot = (output as CreatePermissionsSnapshotInteractor.Output.Success).snapshot
            assertThat(snapshot.permissions).isEqualTo(permissions)
            assertThat(snapshot.groupsMembers).containsExactly(GROUP_ID, listOf(USER_A, USER_B))
            assertThat(snapshot.users.keys).containsExactly(USER_A, USER_B)
        }

    @Test
    fun `groups are not fetched when there are no group permissions`() =
        runTest {
            stubFolderPermissions(listOf(userPermission(USER_A)))
            stubUsers(requestedIds = listOf(USER_A), returnedProfiles = listOf(userProfile(USER_A)))

            val output = interactor.createForFolder(FOLDER_ID)

            assertThat(output).isInstanceOf(CreatePermissionsSnapshotInteractor.Output.Success::class.java)
            verifyNoInteractions(fetchGroupsByIdsUseCase)
        }

    @Test
    fun `permissions and members referencing users that cannot be fetched are dropped`() =
        runTest {
            val resolvableUserPermission = userPermission(USER_A)
            val groupPermission = groupPermission(GROUP_ID)
            stubFolderPermissions(listOf(resolvableUserPermission, userPermission(USER_B), groupPermission))
            fetchGroupsByIdsUseCase.stub {
                onBlocking { execute(FetchGroupsByIdsUseCase.Input(listOf(GROUP_ID))) }
                    .thenReturn(
                        FetchGroupsByIdsUseCase.Output.Success(
                            listOf(groupWithMembers(GROUP_ID, USER_C)),
                        ),
                    )
            }
            stubUsers(requestedIds = listOf(USER_A, USER_B, USER_C), returnedProfiles = listOf(userProfile(USER_A)))

            val output = interactor.createForFolder(FOLDER_ID)

            val snapshot = (output as CreatePermissionsSnapshotInteractor.Output.Success).snapshot
            assertThat(snapshot.permissions).containsExactly(resolvableUserPermission, groupPermission)
            assertThat(snapshot.groupsMembers).containsExactly(GROUP_ID, emptyList<String>())
            assertThat(snapshot.users.keys).containsExactly(USER_A)
        }

    @Test
    fun `created snapshot is stored in the repository`() =
        runTest {
            stubFolderPermissions(listOf(userPermission(USER_A)))
            stubUsers(requestedIds = listOf(USER_A), returnedProfiles = listOf(userProfile(USER_A)))

            val output = interactor.createForFolder(FOLDER_ID)

            val snapshot = (output as CreatePermissionsSnapshotInteractor.Output.Success).snapshot
            verify(permissionsSnapshotRepository).setPermissionsSnapshot(ACCOUNT_ID, snapshot)
        }

    @Test
    fun `drift check without a stored snapshot detects drift`() =
        runTest {
            permissionsSnapshotRepository.stub {
                onBlocking { getPermissionsSnapshot(ACCOUNT_ID) } doReturn null
            }

            val output = interactor.detectDriftForFolder(FOLDER_ID)

            assertThat(output).isEqualTo(CreatePermissionsSnapshotInteractor.DriftOutput.DriftDetected)
        }

    @Test
    fun `drift check with unchanged permissions detects no drift`() =
        runTest {
            stubFolderPermissions(listOf(userPermission(USER_A)))
            stubUsers(requestedIds = listOf(USER_A), returnedProfiles = listOf(userProfile(USER_A)))
            val original = (interactor.createForFolder(FOLDER_ID) as CreatePermissionsSnapshotInteractor.Output.Success).snapshot
            permissionsSnapshotRepository.stub {
                onBlocking { getPermissionsSnapshot(ACCOUNT_ID) } doReturn original
            }

            val output = interactor.detectDriftForFolder(FOLDER_ID)

            assertThat(output).isEqualTo(CreatePermissionsSnapshotInteractor.DriftOutput.NoDrift)
        }

    @Test
    fun `drift check with changed permissions detects drift`() =
        runTest {
            stubFolderPermissions(listOf(userPermission(USER_A)))
            stubUsers(requestedIds = listOf(USER_A), returnedProfiles = listOf(userProfile(USER_A)))
            val original = (interactor.createForFolder(FOLDER_ID) as CreatePermissionsSnapshotInteractor.Output.Success).snapshot
            permissionsSnapshotRepository.stub {
                onBlocking { getPermissionsSnapshot(ACCOUNT_ID) } doReturn original
            }
            stubFolderPermissions(listOf(userPermission(USER_A), userPermission(USER_B)))
            stubUsers(
                requestedIds = listOf(USER_A, USER_B),
                returnedProfiles = listOf(userProfile(USER_A), userProfile(USER_B)),
            )

            val output = interactor.detectDriftForFolder(FOLDER_ID)

            assertThat(output).isEqualTo(CreatePermissionsSnapshotInteractor.DriftOutput.DriftDetected)
        }

    @Test
    fun `drift check fetch failure is propagated`() =
        runTest {
            stubFolderPermissions(listOf(userPermission(USER_A)))
            stubUsers(requestedIds = listOf(USER_A), returnedProfiles = listOf(userProfile(USER_A)))
            val original = (interactor.createForFolder(FOLDER_ID) as CreatePermissionsSnapshotInteractor.Output.Success).snapshot
            permissionsSnapshotRepository.stub {
                onBlocking { getPermissionsSnapshot(ACCOUNT_ID) } doReturn original
            }
            fetchFolderPermissionsUseCase.stub {
                onBlocking { execute(FetchFolderPermissionsUseCase.Input(FOLDER_ID)) }
                    .thenReturn(FetchFolderPermissionsUseCase.Output.Failure(FAILURE))
            }

            val output = interactor.detectDriftForFolder(FOLDER_ID)

            assertThat(output).isEqualTo(CreatePermissionsSnapshotInteractor.DriftOutput.Failure(FAILURE))
        }

    @Test
    fun `folder permissions fetch failure is propagated`() =
        runTest {
            fetchFolderPermissionsUseCase.stub {
                onBlocking { execute(FetchFolderPermissionsUseCase.Input(FOLDER_ID)) }
                    .thenReturn(FetchFolderPermissionsUseCase.Output.Failure(FAILURE))
            }

            val output = interactor.createForFolder(FOLDER_ID)

            assertThat(output).isEqualTo(CreatePermissionsSnapshotInteractor.Output.Failure(FAILURE))
            verifyNoInteractions(fetchGroupsByIdsUseCase)
            verifyNoInteractions(fetchUsersByIdsUseCase)
        }

    @Test
    fun `groups fetch failure is propagated`() =
        runTest {
            stubFolderPermissions(listOf(groupPermission(GROUP_ID)))
            fetchGroupsByIdsUseCase.stub {
                onBlocking { execute(FetchGroupsByIdsUseCase.Input(listOf(GROUP_ID))) }
                    .thenReturn(FetchGroupsByIdsUseCase.Output.Failure(FAILURE))
            }

            val output = interactor.createForFolder(FOLDER_ID)

            assertThat(output).isEqualTo(CreatePermissionsSnapshotInteractor.Output.Failure(FAILURE))
            verifyNoInteractions(fetchUsersByIdsUseCase)
        }

    @Test
    fun `users fetch failure is propagated`() =
        runTest {
            stubFolderPermissions(listOf(userPermission(USER_A)))
            fetchUsersByIdsUseCase.stub {
                onBlocking { execute(FetchUsersByIdsUseCase.Input(listOf(USER_A))) }
                    .thenReturn(FetchUsersByIdsUseCase.Output.Failure(FAILURE))
            }

            val output = interactor.createForFolder(FOLDER_ID)

            assertThat(output).isEqualTo(CreatePermissionsSnapshotInteractor.Output.Failure(FAILURE))
        }

    private fun stubFolderPermissions(permissions: List<PermissionModel>) {
        fetchFolderPermissionsUseCase.stub {
            onBlocking { execute(FetchFolderPermissionsUseCase.Input(FOLDER_ID)) }
                .thenReturn(FetchFolderPermissionsUseCase.Output.Success(permissions))
        }
    }

    private fun stubUsers(
        requestedIds: List<String>,
        returnedProfiles: List<UserProfile>,
    ) {
        fetchUsersByIdsUseCase.stub {
            onBlocking { execute(FetchUsersByIdsUseCase.Input(requestedIds)) }
                .thenReturn(FetchUsersByIdsUseCase.Output.Success(returnedProfiles))
        }
    }

    private fun userPermission(userId: String) =
        PermissionModel.UserPermissionModel(
            permission = ResourcePermission.READ,
            permissionId = "permission-$userId",
            userId = userId,
        )

    private fun groupPermission(groupId: String) =
        PermissionModel.GroupPermissionModel(
            permission = ResourcePermission.UPDATE,
            permissionId = "permission-$groupId",
            group = GroupModel(groupId, "group-name"),
        )

    private fun groupWithMembers(
        groupId: String,
        vararg memberIds: String,
    ) = GroupWithMembers(
        group = Group(groupId, "group-name"),
        members = memberIds.map { GroupMember(it) },
    )

    private fun userProfile(userId: String) =
        UserProfile(
            id = userId,
            username = "$userId@passbolt.com",
            disabled = false,
            role = null,
            firstName = "first-$userId",
            lastName = "last-$userId",
            avatarUrl = null,
            gpgKey = null,
        )

    private companion object {
        const val ACCOUNT_ID = "account-id"
        const val FOLDER_ID = "folder-id"
        const val GROUP_ID = "group-id"
        const val USER_A = "user-a"
        const val USER_B = "user-b"
        const val USER_C = "user-c"
        val FAILURE = DomainResult.Incomplete.Error(UNKNOWN, "error")
    }
}
