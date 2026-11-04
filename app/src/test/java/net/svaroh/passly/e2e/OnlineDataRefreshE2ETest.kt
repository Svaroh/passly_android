/**
 * Passly - Open source password manager for teams
 * Copyright (c) 2026 Svaroh
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Svaroh
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://passly.svaroh.net Passly
 * @since v1.0
 */
package net.svaroh.passly.e2e

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.svaroh.passly.core.fulldatarefresh.HomeDataInteractor
import net.svaroh.passly.core.fulldatarefresh.RefreshProgressTracker
import net.svaroh.passly.core.fulldatarefresh.RefreshProgressTrackerFactory
import net.svaroh.passly.core.idlingresource.ResourcesFullRefreshIdlingResource
import net.svaroh.passly.core.mvp.authentication.AuthenticationState
import net.svaroh.passly.database.snapshot.ResourcesSnapshot
import net.svaroh.passly.domain.folders.usecase.FoldersInteractor
import net.svaroh.passly.domain.groups.usecase.GroupsInteractor
import net.svaroh.passly.domain.metadata.interactor.MetadataKeysInteractor
import net.svaroh.passly.domain.metadata.interactor.MetadataKeysSettingsInteractor
import net.svaroh.passly.domain.metadata.interactor.MetadataPrivateKeysInteractor
import net.svaroh.passly.domain.metadata.interactor.MetadataSessionKeysInteractor
import net.svaroh.passly.domain.metadata.interactor.MetadataTypesSettingsInteractor
import net.svaroh.passly.domain.resources.usecase.ResourceInteractor
import net.svaroh.passly.domain.resourcetypes.usecase.ResourceTypesInteractor
import net.svaroh.passly.domain.users.usecase.UsersInteractor
import net.svaroh.passly.entity.featureflags.FeatureFlagsModel
import net.svaroh.passly.featureflags.usecase.GetFeatureFlagsUseCase
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class OnlineDataRefreshE2ETest {

    private fun createFeatureFlags(isV5MetadataAvailable: Boolean) = FeatureFlagsModel(
        privacyPolicyUrl = null,
        termsAndConditionsUrl = null,
        isPreviewPasswordAvailable = true,
        areFoldersAvailable = true,
        areTagsAvailable = true,
        isTotpAvailable = true,
        isRbacAvailable = true,
        isPasswordExpiryAvailable = true,
        arePasswordPoliciesAvailable = true,
        canUpdatePasswordPolicies = true,
        isV5MetadataAvailable = isV5MetadataAvailable,
    )

    @Test
    fun when_network_is_active_full_data_refresh_fetches_all_domains_and_reports_progress() =
        runTest {
            val mockFoldersInteractor = mock<FoldersInteractor>()
            val mockResourcesInteractor = mock<ResourceInteractor>()
            val mockGroupsInteractor = mock<GroupsInteractor>()
            val mockUsersInteractor = mock<UsersInteractor>()
            val mockResourceTypesInteractor = mock<ResourceTypesInteractor>()
            val mockMetadataKeysInteractor = mock<MetadataKeysInteractor>()
            val mockMetadataPrivateKeysInteractor = mock<MetadataPrivateKeysInteractor>()
            val mockMetadataSessionKeysInteractor = mock<MetadataSessionKeysInteractor>()
            val mockMetadataTypesSettingsInteractor = mock<MetadataTypesSettingsInteractor>()
            val mockMetadataKeysSettingsInteractor = mock<MetadataKeysSettingsInteractor>()
            val mockFeatureFlagsUseCase = mock<GetFeatureFlagsUseCase>()
            val mockIdlingResource = mock<ResourcesFullRefreshIdlingResource>()
            val mockSnapshot = mock<ResourcesSnapshot>()

            // Прапорці: V5 metadata доступна
            val featureFlags = createFeatureFlags(isV5MetadataAvailable = true)
            whenever(mockFeatureFlagsUseCase.execute(Unit)).thenReturn(
                GetFeatureFlagsUseCase.Output(featureFlags),
            )

            whenever(mockMetadataTypesSettingsInteractor.fetchAndSaveMetadataTypesSettings())
                .thenReturn(MetadataTypesSettingsInteractor.Output.Success)
            whenever(mockMetadataKeysSettingsInteractor.fetchAndSaveMetadataKeysSettings())
                .thenReturn(MetadataKeysSettingsInteractor.Output.Success)
            whenever(mockMetadataKeysInteractor.fetchAndSaveMetadataKeys())
                .thenReturn(MetadataKeysInteractor.Output.Success)
            whenever(mockMetadataSessionKeysInteractor.fetchMetadataSessionKeys())
                .thenReturn(MetadataSessionKeysInteractor.Output.Success)
            whenever(mockMetadataSessionKeysInteractor.saveMetadataSessionKeysCache())
                .thenReturn(MetadataSessionKeysInteractor.Output.Success)
            whenever(mockResourceTypesInteractor.fetchAndSaveResourceTypes())
                .thenReturn(ResourceTypesInteractor.Output.Success)
            whenever(mockUsersInteractor.fetchAndSaveUsers())
                .thenReturn(UsersInteractor.Output.Success)
            whenever(mockGroupsInteractor.fetchAndSaveGroups())
                .thenReturn(GroupsInteractor.Output.Success)

            whenever(mockFoldersInteractor.fetchAndSaveFolders(any())).thenReturn(
                FoldersInteractor.Output.Success,
            )
            whenever(mockResourcesInteractor.fetchAndSaveResources(any())).thenReturn(
                ResourceInteractor.Output.Success,
            )

            val progressPoints = mutableListOf<Float>()
            val trackerFactory = RefreshProgressTrackerFactory()

            val interactor = HomeDataInteractor(
                foldersInteractor = mockFoldersInteractor,
                resourcesInteractor = mockResourcesInteractor,
                groupsInteractor = mockGroupsInteractor,
                usersInteractor = mockUsersInteractor,
                resourceTypesInteractor = mockResourceTypesInteractor,
                metadataKeysInteractor = mockMetadataKeysInteractor,
                metadataPrivateKeysInteractor = mockMetadataPrivateKeysInteractor,
                metadataSessionKeysInteractor = mockMetadataSessionKeysInteractor,
                metadataTypesSettingsInteractor = mockMetadataTypesSettingsInteractor,
                metadataKeysSettingsInteractor = mockMetadataKeysSettingsInteractor,
                featureFlagsUseCase = mockFeatureFlagsUseCase,
                resourcesFullRefreshIdlingResource = mockIdlingResource,
                resourcesSnapshot = mockSnapshot,
                refreshProgressTrackerFactory = trackerFactory,
            )

            val result = interactor.refreshAllHomeScreenData(
                onProgress = { progressPoints.add(it) },
            )

            // Перевіряємо успішність результату
            assertIs<HomeDataInteractor.Output.Success>(result)
            assertThat(result.authenticationState).isEqualTo(AuthenticationState.Authenticated)

            // Перевіряємо, що снапшот та IdlingResource були коректно задіяні
            verify(mockSnapshot).populateForCurrentAccount()
            verify(mockSnapshot).clear()
            verify(mockIdlingResource).setIdle(false)
            verify(mockIdlingResource).setIdle(true)

            // Перевіряємо прогрес
            assertThat(progressPoints).isNotEmpty()
            assertThat(progressPoints.last()).isEqualTo(1.0f)
        }

    @Test
    fun when_feature_flags_have_v5_metadata_disabled_metadata_steps_are_safely_skipped() =
        runTest {
            val mockFoldersInteractor = mock<FoldersInteractor>()
            val mockResourcesInteractor = mock<ResourceInteractor>()
            val mockGroupsInteractor = mock<GroupsInteractor>()
            val mockUsersInteractor = mock<UsersInteractor>()
            val mockResourceTypesInteractor = mock<ResourceTypesInteractor>()
            val mockMetadataKeysInteractor = mock<MetadataKeysInteractor>()
            val mockMetadataPrivateKeysInteractor = mock<MetadataPrivateKeysInteractor>()
            val mockMetadataSessionKeysInteractor = mock<MetadataSessionKeysInteractor>()
            val mockMetadataTypesSettingsInteractor = mock<MetadataTypesSettingsInteractor>()
            val mockMetadataKeysSettingsInteractor = mock<MetadataKeysSettingsInteractor>()
            val mockFeatureFlagsUseCase = mock<GetFeatureFlagsUseCase>()
            val mockIdlingResource = mock<ResourcesFullRefreshIdlingResource>()
            val mockSnapshot = mock<ResourcesSnapshot>()

            // V5 metadata вимкнена
            val featureFlags = createFeatureFlags(isV5MetadataAvailable = false)
            whenever(mockFeatureFlagsUseCase.execute(Unit)).thenReturn(
                GetFeatureFlagsUseCase.Output(featureFlags),
            )

            whenever(mockResourceTypesInteractor.fetchAndSaveResourceTypes())
                .thenReturn(ResourceTypesInteractor.Output.Success)
            whenever(mockUsersInteractor.fetchAndSaveUsers())
                .thenReturn(UsersInteractor.Output.Success)
            whenever(mockGroupsInteractor.fetchAndSaveGroups())
                .thenReturn(GroupsInteractor.Output.Success)
            whenever(mockFoldersInteractor.fetchAndSaveFolders(any())).thenReturn(
                FoldersInteractor.Output.Success,
            )
            whenever(mockResourcesInteractor.fetchAndSaveResources(any())).thenReturn(
                ResourceInteractor.Output.Success,
            )

            val trackerFactory = RefreshProgressTrackerFactory()
            val progressPoints = mutableListOf<Float>()

            val interactor = HomeDataInteractor(
                foldersInteractor = mockFoldersInteractor,
                resourcesInteractor = mockResourcesInteractor,
                groupsInteractor = mockGroupsInteractor,
                usersInteractor = mockUsersInteractor,
                resourceTypesInteractor = mockResourceTypesInteractor,
                metadataKeysInteractor = mockMetadataKeysInteractor,
                metadataPrivateKeysInteractor = mockMetadataPrivateKeysInteractor,
                metadataSessionKeysInteractor = mockMetadataSessionKeysInteractor,
                metadataTypesSettingsInteractor = mockMetadataTypesSettingsInteractor,
                metadataKeysSettingsInteractor = mockMetadataKeysSettingsInteractor,
                featureFlagsUseCase = mockFeatureFlagsUseCase,
                resourcesFullRefreshIdlingResource = mockIdlingResource,
                resourcesSnapshot = mockSnapshot,
                refreshProgressTrackerFactory = trackerFactory,
            )

            val result = interactor.refreshAllHomeScreenData(
                onProgress = { progressPoints.add(it) },
            )

            assertIs<HomeDataInteractor.Output.Success>(result)
            assertThat(progressPoints.last()).isEqualTo(1.0f)
        }

    @Test
    fun when_a_domain_interactor_reports_authentication_expired_full_refresh_fails_with_expired_state() =
        runTest {
            val mockFoldersInteractor = mock<FoldersInteractor>()
            val mockResourcesInteractor = mock<ResourceInteractor>()
            val mockGroupsInteractor = mock<GroupsInteractor>()
            val mockUsersInteractor = mock<UsersInteractor>()
            val mockResourceTypesInteractor = mock<ResourceTypesInteractor>()
            val mockMetadataKeysInteractor = mock<MetadataKeysInteractor>()
            val mockMetadataPrivateKeysInteractor = mock<MetadataPrivateKeysInteractor>()
            val mockMetadataSessionKeysInteractor = mock<MetadataSessionKeysInteractor>()
            val mockMetadataTypesSettingsInteractor = mock<MetadataTypesSettingsInteractor>()
            val mockMetadataKeysSettingsInteractor = mock<MetadataKeysSettingsInteractor>()
            val mockFeatureFlagsUseCase = mock<GetFeatureFlagsUseCase>()
            val mockIdlingResource = mock<ResourcesFullRefreshIdlingResource>()
            val mockSnapshot = mock<ResourcesSnapshot>()

            val featureFlags = createFeatureFlags(isV5MetadataAvailable = false)
            whenever(mockFeatureFlagsUseCase.execute(Unit)).thenReturn(
                GetFeatureFlagsUseCase.Output(featureFlags),
            )

            // Симулюємо закінчення терміну дії токена сесії під час оновлення ресурсів
            val unauthenticatedSession = AuthenticationState.Unauthenticated(
                AuthenticationState.Unauthenticated.Reason.Session,
            )

            whenever(mockResourceTypesInteractor.fetchAndSaveResourceTypes())
                .thenReturn(ResourceTypesInteractor.Output.Success)
            whenever(mockUsersInteractor.fetchAndSaveUsers())
                .thenReturn(UsersInteractor.Output.Success)
            whenever(mockGroupsInteractor.fetchAndSaveGroups())
                .thenReturn(GroupsInteractor.Output.Success)
            whenever(mockFoldersInteractor.fetchAndSaveFolders(any()))
                .thenReturn(FoldersInteractor.Output.Success)
            whenever(mockResourcesInteractor.fetchAndSaveResources(any()))
                .thenReturn(ResourceInteractor.Output.Failure(unauthenticatedSession))

            val trackerFactory = RefreshProgressTrackerFactory()

            val interactor = HomeDataInteractor(
                foldersInteractor = mockFoldersInteractor,
                resourcesInteractor = mockResourcesInteractor,
                groupsInteractor = mockGroupsInteractor,
                usersInteractor = mockUsersInteractor,
                resourceTypesInteractor = mockResourceTypesInteractor,
                metadataKeysInteractor = mockMetadataKeysInteractor,
                metadataPrivateKeysInteractor = mockMetadataPrivateKeysInteractor,
                metadataSessionKeysInteractor = mockMetadataSessionKeysInteractor,
                metadataTypesSettingsInteractor = mockMetadataTypesSettingsInteractor,
                metadataKeysSettingsInteractor = mockMetadataKeysSettingsInteractor,
                featureFlagsUseCase = mockFeatureFlagsUseCase,
                resourcesFullRefreshIdlingResource = mockIdlingResource,
                resourcesSnapshot = mockSnapshot,
                refreshProgressTrackerFactory = trackerFactory,
            )

            val result = interactor.refreshAllHomeScreenData()

            assertIs<HomeDataInteractor.Output.Failure>(result)
            val authState = result.authenticationState
            assertIs<AuthenticationState.Unauthenticated>(authState)
            assertThat(authState.reason).isEqualTo(AuthenticationState.Unauthenticated.Reason.Session)
        }

    @Test
    fun refresh_progress_tracker_computes_fractional_and_step_progress_accurately() =
        runTest {
            val progressValues = mutableListOf<Float>()
            val tracker = RefreshProgressTracker(totalSteps = 4) { progressValues.add(it) }

            // Крок 1: завершено
            tracker.onStepCompleted()
            assertThat(progressValues.last()).isEqualTo(0.25f)

            // Крок 2: завантаження сторінок (1 з 2) -> (1 + 0.5) / 4 = 0.375
            tracker.onStepPageDownloaded(downloadedPages = 1, totalPages = 2)
            assertThat(progressValues.last()).isEqualTo(0.375f)

            // Крок 2: завершено повністю -> 2 / 4 = 0.5
            tracker.onStepCompleted()
            assertThat(progressValues.last()).isEqualTo(0.5f)

            // Кроки 3 і 4: пропущено (наприклад, V5 метадані вимкнені) -> (2 + 2) / 4 = 1.0
            tracker.onStepsSkipped(count = 2)
            assertThat(progressValues.last()).isEqualTo(1.0f)
        }
}
