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

package net.svaroh.passly.domain.resources.actions

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runTest
import net.svaroh.passly.core.resourcetypes.graph.redesigned.ResourceTypesUpdatesAdjacencyGraph
import net.svaroh.passly.core.secrets.usecase.db.RemoveLocalSecretUseCase
import net.svaroh.passly.core.secrets.usecase.db.UpsertLocalSecretsUseCase
import net.svaroh.passly.domain.folders.usecase.GetLocalFolderPermissionsUseCase
import net.svaroh.passly.domain.metadata.interactor.MetadataPrivateKeysInteractor
import net.svaroh.passly.domain.metadata.usecase.GetMetadataKeysSettingsUseCase
import net.svaroh.passly.domain.metadata.usecase.db.GetLocalMetadataKeysUseCase
import net.svaroh.passly.domain.permissionsconfirmation.usecase.GetPermissionsSnapshotUseCase
import net.svaroh.passly.domain.resources.actions.ResourceUpdateActionResult.CannotUpdateWithCurrentConfig
import net.svaroh.passly.domain.resources.interactor.update.UpdateResourceInteractor
import net.svaroh.passly.domain.resources.usecase.CreatePermissionsSnapshotInteractor
import net.svaroh.passly.domain.resources.usecase.ResourceShareInteractor
import net.svaroh.passly.domain.resources.usecase.db.GetLocalResourcePermissionsUseCase
import net.svaroh.passly.domain.resources.usecase.db.UpdateLocalResourceUseCase
import net.svaroh.passly.domain.resourcetypes.usecase.ResourceTypeIdToSlugMappingProvider
import net.svaroh.passly.domain.users.usecase.GetLocalCurrentUserUseCase
import net.svaroh.passly.jsonmodel.jsonModelModule
import net.svaroh.passly.supportedresourceTypes.ContentType
import net.svaroh.passly.supportedresourceTypes.ContentType.PasswordAndDescription
import net.svaroh.passly.supportedresourceTypes.ContentType.PasswordDescriptionTotp
import net.svaroh.passly.supportedresourceTypes.ContentType.PasswordString
import net.svaroh.passly.supportedresourceTypes.ContentType.Totp
import net.svaroh.passly.supportedresourceTypes.ContentType.V5Default
import net.svaroh.passly.supportedresourceTypes.ContentType.V5DefaultWithTotp
import net.svaroh.passly.supportedresourceTypes.ContentType.V5PasswordString
import net.svaroh.passly.supportedresourceTypes.ContentType.V5TotpStandalone
import net.svaroh.passly.ui.MetadataJsonModel
import net.svaroh.passly.ui.MetadataKeyTypeModel.PERSONAL
import net.svaroh.passly.ui.ResourcePermission.OWNER
import net.svaroh.passly.ui.ResourceUiModel
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertIs

@ExperimentalCoroutinesApi
class ResourceUpdateActionsInteractorUpgradeTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(jsonModelModule)
        }

    @Test
    fun `upgradeToV5 returns CannotUpdateWithCurrentConfig when resource is already v5`() =
        runTest {
            val interactor = buildInteractor(resourceSlug = V5Default.slug, mapping = emptyMap())

            val result = interactor.upgradeToV5().single()

            assertIs<CannotUpdateWithCurrentConfig>(result)
        }

    @Test
    fun `upgradeToV5 PasswordString looks up v5-password-string slug in account mapping`() =
        runTest {
            val mapping = mappingExcluding(V5PasswordString)
            val interactor = buildInteractor(resourceSlug = PasswordString.slug, mapping = mapping)

            val result = interactor.upgradeToV5().single()

            assertIs<CannotUpdateWithCurrentConfig>(result)
        }

    @Test
    fun `upgradeToV5 PasswordAndDescription looks up v5-default slug in account mapping`() =
        runTest {
            val mapping = mappingExcluding(V5Default)
            val interactor = buildInteractor(resourceSlug = PasswordAndDescription.slug, mapping = mapping)

            val result = interactor.upgradeToV5().single()

            assertIs<CannotUpdateWithCurrentConfig>(result)
        }

    @Test
    fun `upgradeToV5 PasswordDescriptionTotp looks up v5-default-with-totp slug in account mapping`() =
        runTest {
            val mapping = mappingExcluding(V5DefaultWithTotp)
            val interactor = buildInteractor(resourceSlug = PasswordDescriptionTotp.slug, mapping = mapping)

            val result = interactor.upgradeToV5().single()

            assertIs<CannotUpdateWithCurrentConfig>(result)
        }

    @Test
    fun `upgradeToV5 Totp looks up v5-totp-standalone slug in account mapping`() =
        runTest {
            val mapping = mappingExcluding(V5TotpStandalone)
            val interactor = buildInteractor(resourceSlug = Totp.slug, mapping = mapping)

            val result = interactor.upgradeToV5().single()

            assertIs<CannotUpdateWithCurrentConfig>(result)
        }

    @Test
    fun `upgradeToV5 returns CannotUpdateWithCurrentConfig when account mapping is empty`() =
        runTest {
            val interactor = buildInteractor(resourceSlug = PasswordString.slug, mapping = emptyMap())

            val result = interactor.upgradeToV5().single()

            assertIs<CannotUpdateWithCurrentConfig>(result)
        }

    @Test
    fun `upgradeToV5WithConfirmedPermissions returns CannotUpdateWithCurrentConfig when resource is already v5`() =
        runTest {
            val interactor = buildInteractor(resourceSlug = V5Default.slug, mapping = emptyMap())

            val result = interactor.upgradeToV5WithConfirmedPermissions(emptyList()).single()

            assertIs<CannotUpdateWithCurrentConfig>(result)
        }

    @Test
    fun `upgrade re-encrypts the secret for all v4 types except password string`() {
        assertThat(buildInteractor(PasswordString.slug, emptyMap()).doesUpgradeToV5ReEncryptSecret()).isFalse()
        assertThat(buildInteractor(PasswordAndDescription.slug, emptyMap()).doesUpgradeToV5ReEncryptSecret()).isTrue()
        assertThat(buildInteractor(PasswordDescriptionTotp.slug, emptyMap()).doesUpgradeToV5ReEncryptSecret()).isTrue()
        assertThat(buildInteractor(Totp.slug, emptyMap()).doesUpgradeToV5ReEncryptSecret()).isTrue()
        assertThat(buildInteractor(V5Default.slug, emptyMap()).doesUpgradeToV5ReEncryptSecret()).isFalse()
    }

    private fun mappingExcluding(excluded: ContentType): Map<UUID, String> =
        listOf(V5PasswordString, V5Default, V5DefaultWithTotp, V5TotpStandalone)
            .filter { it != excluded }
            .associate { UUID.randomUUID() to it.slug }

    private fun buildInteractor(
        resourceSlug: String,
        mapping: Map<UUID, String>,
    ): ResourceUpdateActionsInteractor {
        val mappingProvider = mock<ResourceTypeIdToSlugMappingProvider>()
        mappingProvider.stub {
            on { provideMappingForSelectedAccount() }.thenReturn(mapping)
        }
        return ResourceUpdateActionsInteractor(
            existingResource = resourceModel(slug = resourceSlug),
            secretPropertiesActionsInteractor = mock(),
            updateResourceInteractor = mock<UpdateResourceInteractor>(),
            resourceTypesUpdateGraph = mock<ResourceTypesUpdatesAdjacencyGraph>(),
            updateLocalResourceUseCase = mock<UpdateLocalResourceUseCase>(),
            getLocalCurrentUserUseCase = mock<GetLocalCurrentUserUseCase>(),
            metadataPrivateKeysInteractor = mock<MetadataPrivateKeysInteractor>(),
            getLocalFolderPermissionsUseCase = mock<GetLocalFolderPermissionsUseCase>(),
            getLocalResourcePermissionsUseCase = mock<GetLocalResourcePermissionsUseCase>(),
            getMetadataKeysSettingsUseCase = mock<GetMetadataKeysSettingsUseCase>(),
            getMetadataKeysUseCase = mock<GetLocalMetadataKeysUseCase>(),
            resourceTypeIdToSlugMappingProvider = mappingProvider,
            createPermissionsSnapshotInteractor = mock<CreatePermissionsSnapshotInteractor>(),
            getPermissionsSnapshotUseCase = mock<GetPermissionsSnapshotUseCase>(),
            resourceShareInteractor = mock<ResourceShareInteractor>(),
            confirmedRecipientsPublicKeysResolver = mock<ConfirmedRecipientsPublicKeysResolver>(),
            upsertLocalSecretsUseCase = mock<UpsertLocalSecretsUseCase>(),
            removeLocalSecretUseCase = mock<RemoveLocalSecretUseCase>(),
        )
    }

    private fun resourceModel(slug: String): ResourceUiModel =
        ResourceUiModel(
            resourceId = "resourceId",
            resourceTypeId = UUID.randomUUID().toString(),
            slug = slug,
            folderId = null,
            permission = OWNER,
            favouriteId = null,
            modified = ZonedDateTime.now(),
            expiry = null,
            metadataKeyId = null,
            metadataKeyType = PERSONAL,
            metadataJsonModel = MetadataJsonModel("""{"name": "Test"}"""),
        )
}
