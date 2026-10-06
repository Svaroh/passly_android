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

package net.svaroh.passly.domain.resources.actions

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.svaroh.passly.commontest.session.validSessionTestModule
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.architecture.result.DomainResult.Incomplete.Error.Reason.UNKNOWN
import net.svaroh.passly.core.architecture.result.displayMessage
import net.svaroh.passly.domain.permissionsconfirmation.model.PermissionsSnapshot
import net.svaroh.passly.domain.permissionsconfirmation.usecase.GetPermissionsSnapshotUseCase
import net.svaroh.passly.domain.resources.usecase.CreatePermissionsSnapshotInteractor
import net.svaroh.passly.domain.resources.usecase.CreatePermissionsSnapshotInteractor.DriftOutput
import net.svaroh.passly.domain.resources.usecase.ResourceShareInteractor
import net.svaroh.passly.domain.resources.usecase.db.GetLocalResourceUseCase
import net.svaroh.passly.domain.users.model.GpgKey
import net.svaroh.passly.domain.users.model.UserProfile
import net.svaroh.passly.jsonmodel.jsonModelModule
import net.svaroh.passly.supportedresourceTypes.ContentType.PasswordAndDescription
import net.svaroh.passly.supportedresourceTypes.ContentType.V5Default
import net.svaroh.passly.ui.MetadataJsonModel
import net.svaroh.passly.ui.MetadataKeyTypeModel.PERSONAL
import net.svaroh.passly.ui.PermissionModel
import net.svaroh.passly.ui.PermissionModelUi
import net.svaroh.passly.ui.ResourcePermission
import net.svaroh.passly.ui.ResourceUiModel
import net.svaroh.passly.ui.UserWithAvatar
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import java.time.ZonedDateTime
import java.util.UUID

@ExperimentalCoroutinesApi
class ResourceShareActionsInteractorTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mock<CreatePermissionsSnapshotInteractor>() }
                    single { mock<GetPermissionsSnapshotUseCase>() }
                    single { mock<ResourceShareInteractor>() }
                    single { mock<ConfirmedRecipientsPublicKeysResolver>() }
                    single { mock<GetLocalResourceUseCase>() }
                    single { mock<ResourceUpdateActionsInteractor>() }
                    single<ResourceUpdateActionsInteractorFactory> {
                        ResourceUpdateActionsInteractorFactory { get() }
                    }
                    singleOf(::ResourceShareActionsInteractor)
                },
                jsonModelModule,
                validSessionTestModule,
            )
        }

    @Before
    fun setUp() {
        get<CreatePermissionsSnapshotInteractor>().stub {
            on { detectDriftForResource(RESOURCE_ID) } doReturn DriftOutput.NoDrift
        }
        get<GetPermissionsSnapshotUseCase>().stub {
            on { execute(Unit) } doReturn GetPermissionsSnapshotUseCase.Output(SNAPSHOT)
        }
        get<ConfirmedRecipientsPublicKeysResolver>().stub {
            on { resolve(any()) } doReturn CONFIRMED_KEYS
        }
        get<ResourceShareInteractor>().stub {
            on { simulateAndShareResource(any(), any(), any(), anyOrNull()) } doReturn
                ResourceShareInteractor.Output.Success
        }
        get<GetLocalResourceUseCase>().stub {
            on { execute(GetLocalResourceUseCase.Input(RESOURCE_ID)) } doReturn
                GetLocalResourceUseCase.Output(resourceModel(PasswordAndDescription.slug))
        }
    }

    @Test
    fun `drift detected before the share stops with the drifted names`() =
        runTest {
            get<CreatePermissionsSnapshotInteractor>().stub {
                on { detectDriftForResource(RESOURCE_ID) } doReturn
                    DriftOutput.DriftDetected(listOf("drifted-user"))
            }

            val result = get<ResourceShareActionsInteractor>().shareWithConfirmedPermissions(RESOURCE_ID, listOf(OPERATOR_UI))

            assertThat(result).isEqualTo(ShareActionResult.PermissionsDrifted(listOf("drifted-user")))
            verifyNoInteractions(get<ResourceShareInteractor>())
        }

    @Test
    fun `missing snapshot during the drift check stops with a drift result without names`() =
        runTest {
            get<CreatePermissionsSnapshotInteractor>().stub {
                on { detectDriftForResource(RESOURCE_ID) } doReturn DriftOutput.SnapshotMissing
            }

            val result = get<ResourceShareActionsInteractor>().shareWithConfirmedPermissions(RESOURCE_ID, listOf(OPERATOR_UI))

            assertThat(result).isEqualTo(ShareActionResult.PermissionsDrifted(driftedEntityNames = emptyList()))
            verifyNoInteractions(get<ResourceShareInteractor>())
        }

    @Test
    fun `share is applied with the confirmed keys against the snapshot permissions`() =
        runTest {
            val result =
                get<ResourceShareActionsInteractor>().shareWithConfirmedPermissions(RESOURCE_ID, listOf(OPERATOR_UI, USER_UI))

            assertThat(result).isEqualTo(ShareActionResult.Success)
            val existingPermissionsCaptor = argumentCaptor<List<PermissionModelUi>>()
            val keysCaptor = argumentCaptor<Map<String, String>>()
            verify(get<ResourceShareInteractor>()).simulateAndShareResource(
                eq(RESOURCE_ID),
                any(),
                keysCaptor.capture(),
                existingPermissionsCaptor.capture(),
            )
            assertThat(keysCaptor.firstValue).isEqualTo(CONFIRMED_KEYS)
            assertThat(
                existingPermissionsCaptor.firstValue
                    .filterIsInstance<PermissionModelUi.UserPermissionModel>()
                    .map { it.user.userId },
            ).containsExactly(OPERATOR_ID, USER_ID)
        }

    @Test
    fun `v4 resource share does not re-encrypt the metadata`() =
        runTest {
            get<ResourceShareActionsInteractor>().shareWithConfirmedPermissions(RESOURCE_ID, listOf(OPERATOR_UI))

            verifyNoInteractions(get<ResourceUpdateActionsInteractor>())
        }

    @Test
    fun `v5 resource metadata is re-encrypted with the shared key before the share`() =
        runTest {
            stubV5Resource()
            get<ResourceUpdateActionsInteractor>().stub {
                on { reEncryptResourceMetadata() } doReturn
                    flowOf(ResourceUpdateActionResult.Success(RESOURCE_ID, "name"))
            }

            val result = get<ResourceShareActionsInteractor>().shareWithConfirmedPermissions(RESOURCE_ID, listOf(OPERATOR_UI))

            assertThat(result).isEqualTo(ShareActionResult.Success)
            verify(get<ResourceUpdateActionsInteractor>()).reEncryptResourceMetadata()
            verify(get<ResourceShareInteractor>()).simulateAndShareResource(any(), any(), any(), anyOrNull())
        }

    @Test
    fun `v5 metadata re-encryption failure stops before the share`() =
        runTest {
            stubV5Resource()
            get<ResourceUpdateActionsInteractor>().stub {
                on { reEncryptResourceMetadata() } doReturn
                    flowOf(ResourceUpdateActionResult.MetadataKeyVerificationFailure)
            }

            val result = get<ResourceShareActionsInteractor>().shareWithConfirmedPermissions(RESOURCE_ID, listOf(OPERATOR_UI))

            assertThat(result).isEqualTo(ShareActionResult.MetadataKeyVerificationFailure)
            verifyNoInteractions(get<ResourceShareInteractor>())
        }

    @Test
    fun `dry-run drift during the share reloads the confirmation`() =
        runTest {
            get<ResourceShareInteractor>().stub {
                on { simulateAndShareResource(any(), any(), any(), anyOrNull()) } doReturn
                    ResourceShareInteractor.Output.DriftDetected
            }

            val result = get<ResourceShareActionsInteractor>().shareWithConfirmedPermissions(RESOURCE_ID, listOf(OPERATOR_UI))

            assertThat(result).isEqualTo(ShareActionResult.PermissionsDrifted(driftedEntityNames = emptyList()))
        }

    @Test
    fun `share failure is reported`() =
        runTest {
            get<ResourceShareInteractor>().stub {
                on { simulateAndShareResource(any(), any(), any(), anyOrNull()) } doReturn
                    ResourceShareInteractor.Output.ShareFailure(FAILURE)
            }

            val result = get<ResourceShareActionsInteractor>().shareWithConfirmedPermissions(RESOURCE_ID, listOf(OPERATOR_UI))

            assertThat(result).isEqualTo(ShareActionResult.ShareFailure(FAILURE.displayMessage()))
        }

    private fun stubV5Resource() {
        get<GetLocalResourceUseCase>().stub {
            on { execute(GetLocalResourceUseCase.Input(RESOURCE_ID)) } doReturn
                GetLocalResourceUseCase.Output(resourceModel(V5Default.slug))
        }
    }

    private companion object {
        private const val RESOURCE_ID = "resource-id"
        private const val OPERATOR_ID = "operator-id"
        private const val USER_ID = "user-id"

        private val FAILURE = DomainResult.Incomplete.Error(UNKNOWN, "error")
        private val CONFIRMED_KEYS = mapOf(OPERATOR_ID to "key-operator", USER_ID to "key-user")

        private val OPERATOR_UI = userPermissionUi(OPERATOR_ID, "perm-operator", ResourcePermission.OWNER)
        private val USER_UI = userPermissionUi(USER_ID, "perm-user", ResourcePermission.READ)

        private val SNAPSHOT =
            PermissionsSnapshot(
                permissions =
                    listOf(
                        PermissionModel.UserPermissionModel(ResourcePermission.OWNER, "perm-operator", OPERATOR_ID),
                        PermissionModel.UserPermissionModel(ResourcePermission.READ, "perm-user", USER_ID),
                    ),
                groupsMembers = emptyMap(),
                users = listOf(userProfile(OPERATOR_ID), userProfile(USER_ID)).associateBy { it.id },
                created = ZonedDateTime.now(),
            )

        private fun resourceModel(slug: String) =
            ResourceUiModel(
                resourceId = RESOURCE_ID,
                resourceTypeId = UUID.randomUUID().toString(),
                slug = slug,
                folderId = null,
                permission = ResourcePermission.OWNER,
                favouriteId = null,
                modified = ZonedDateTime.now(),
                expiry = null,
                metadataKeyId = null,
                metadataKeyType = PERSONAL,
                metadataJsonModel = MetadataJsonModel("""{"name": "Test"}"""),
            )

        private fun userPermissionUi(
            userId: String,
            permissionId: String,
            permission: ResourcePermission,
        ) = PermissionModelUi.UserPermissionModel(
            permission = permission,
            permissionId = permissionId,
            user =
                UserWithAvatar(
                    userId = userId,
                    firstName = "first-$userId",
                    lastName = "last-$userId",
                    userName = "$userId@passbolt.com",
                    isDisabled = false,
                    avatarUrl = null,
                ),
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
                gpgKey =
                    GpgKey(
                        id = "gpg-$userId",
                        armoredKey = "armored-key-$userId",
                        fingerprint = "fingerprint-$userId",
                        bits = 2048,
                        uid = null,
                        keyId = "key-$userId",
                        type = null,
                        keyExpirationDate = null,
                        keyCreationDate = null,
                    ),
            )
    }
}
