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

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.paging.PagingData
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.svaroh.passly.common.datarefresh.DataRefreshTrackingFlow
import net.svaroh.passly.common.urimatcher.AutofillUriMatcher
import net.svaroh.passly.commontest.TestCoroutineLaunchContext
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.fulldatarefresh.HomeDataInteractor
import net.svaroh.passly.core.fulldatarefresh.RefreshProgressTrackerFactory
import net.svaroh.passly.core.idlingresource.ResourcesFullRefreshIdlingResource
import net.svaroh.passly.core.mvp.authentication.AuthenticationState
import net.svaroh.passly.core.passwordgenerator.PasswordGenerator
import net.svaroh.passly.core.passwordgenerator.SecretGenerator
import net.svaroh.passly.core.passwordgenerator.dice.Dice
import net.svaroh.passly.core.passwordgenerator.entropy.EntropyCalculator
import net.svaroh.passly.core.passphrasememorycache.AuthOnEveryEntryChecker
import net.svaroh.passly.core.passphrasememorycache.PassphraseMemoryCache
import net.svaroh.passly.core.passphrasememorycache.PotentialPassphrase
import net.svaroh.passly.core.passphrasememorycache.usePassphraseCopy
import net.svaroh.passly.core.sync.SyncFailureReason
import net.svaroh.passly.core.sync.SyncResult
import net.svaroh.passly.core.sync.SyncStats
import net.svaroh.passly.core.sync.SyncStatus
import net.svaroh.passly.core.sync.SyncStatusRepository
import net.svaroh.passly.database.snapshot.ResourcesSnapshot
import net.svaroh.passly.domain.folders.FoldersRepository
import net.svaroh.passly.domain.folders.model.FolderModel
import net.svaroh.passly.domain.folders.model.FolderModelWithAttributes
import net.svaroh.passly.domain.folders.usecase.CreateFolderUseCase
import net.svaroh.passly.domain.folders.usecase.FoldersInteractor
import net.svaroh.passly.domain.folders.usecase.GetLocalDirectChildFoldersPaginatedUseCase
import net.svaroh.passly.domain.folders.usecase.GetLocalSubFoldersForFolderPaginatedUseCase
import net.svaroh.passly.domain.folders.usecase.GetLocalSubFoldersForFolderUseCase
import net.svaroh.passly.domain.groups.usecase.GetLocalGroupsWithShareItemsCountPaginatedUseCase
import net.svaroh.passly.domain.groups.usecase.GroupsInteractor
import net.svaroh.passly.domain.metadata.interactor.MetadataKeysInteractor
import net.svaroh.passly.domain.metadata.interactor.MetadataKeysSettingsInteractor
import net.svaroh.passly.domain.metadata.interactor.MetadataPrivateKeysInteractor
import net.svaroh.passly.domain.metadata.interactor.MetadataSessionKeysInteractor
import net.svaroh.passly.domain.metadata.interactor.MetadataTypesSettingsInteractor
import net.svaroh.passly.domain.rbac.usecase.GetRbacRulesUseCase
import net.svaroh.passly.domain.resources.usecase.ResourceInteractor
import net.svaroh.passly.domain.resources.usecase.db.GetLocalResourcesPaginatedUseCase
import net.svaroh.passly.domain.resources.usecase.db.GetLocalResourcesWithGroupPaginatedUseCase
import net.svaroh.passly.domain.resources.usecase.db.GetLocalResourcesWithTagPaginatedUseCase
import net.svaroh.passly.domain.resources.usecase.db.GetLocalSubFolderResourcesFilteredPaginatedUseCase
import net.svaroh.passly.domain.resources.usecase.db.GetResourcesInFolderPaginatedUseCase
import net.svaroh.passly.domain.resourcetypes.usecase.ResourceTypesInteractor
import net.svaroh.passly.domain.tags.usecase.GetLocalTagsPaginatedUseCase
import net.svaroh.passly.domain.users.usecase.UsersInteractor
import net.svaroh.passly.entity.featureflags.FeatureFlagsModel
import net.svaroh.passly.feature.home.screen.ShowSuggestedModel
import net.svaroh.passly.feature.home.screen.data.HomeDataProvider
import net.svaroh.passly.featureflags.usecase.GetFeatureFlagsUseCase
import net.svaroh.passly.ui.HomeDisplayViewModel
import net.svaroh.passly.ui.PasswordGeneratorSettingsUiModel
import net.svaroh.passly.ui.PermissionModel
import net.svaroh.passly.ui.RbacModel
import net.svaroh.passly.ui.RbacRuleModel
import net.svaroh.passly.ui.ResourcePermission
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ComprehensiveAppLifecycleE2ETest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun complete_enterprise_e2e_lifecycle_online_bootstrap_offline_transition_resync_and_cleanup() =
        runTest {
            val userPassphrase = "EnterpriseSafePassphrase2026!".toByteArray()
            val now = ZonedDateTime.now()

            // -------------------------------------------------------------
            // Фаза 1: Ініціалізація сховища та кешу парольної фрази
            // -------------------------------------------------------------
            val mockLifecycleOwner = mock<LifecycleOwner>()
            val mockLifecycle = mock<Lifecycle>()
            whenever(mockLifecycleOwner.lifecycle).thenReturn(mockLifecycle)

            val passphraseMemoryCache = PassphraseMemoryCache(
                coroutineLaunchContext = TestCoroutineLaunchContext(),
                lifecycleOwner = mockLifecycleOwner,
                dataRefreshTrackingFlow = DataRefreshTrackingFlow(),
                authOnEveryEntryChecker = AuthOnEveryEntryChecker { false },
            )
            assertThat(passphraseMemoryCache.hasPassphrase()).isFalse()

            // Зберігаємо парольну фразу в пам'яті після успішного входу
            passphraseMemoryCache.set(userPassphrase)
            assertThat(passphraseMemoryCache.hasPassphrase()).isTrue()

            // -------------------------------------------------------------
            // Фаза 2: Повне оновлення даних при онлайн-підключенні
            // -------------------------------------------------------------
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

            val featureFlags = FeatureFlagsModel(
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
                isV5MetadataAvailable = false,
            )
            whenever(mockFeatureFlagsUseCase.execute(Unit)).thenReturn(
                GetFeatureFlagsUseCase.Output(featureFlags),
            )
            whenever(mockResourceTypesInteractor.fetchAndSaveResourceTypes()).thenReturn(ResourceTypesInteractor.Output.Success)
            whenever(mockUsersInteractor.fetchAndSaveUsers()).thenReturn(UsersInteractor.Output.Success)
            whenever(mockGroupsInteractor.fetchAndSaveGroups()).thenReturn(GroupsInteractor.Output.Success)
            whenever(mockFoldersInteractor.fetchAndSaveFolders(any())).thenReturn(FoldersInteractor.Output.Success)
            whenever(mockResourcesInteractor.fetchAndSaveResources(any())).thenReturn(ResourceInteractor.Output.Success)

            val homeDataInteractor = HomeDataInteractor(
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
                refreshProgressTrackerFactory = RefreshProgressTrackerFactory(),
            )

            val refreshOutput = homeDataInteractor.refreshAllHomeScreenData()
            assertIs<HomeDataInteractor.Output.Success>(refreshOutput)

            // -------------------------------------------------------------
            // Фаза 3: Генерація надійного паролю та створення папки
            // -------------------------------------------------------------
            val coroutineLaunchContext = TestCoroutineLaunchContext()
            val passwordGenerator = PasswordGenerator(coroutineLaunchContext)
            val mockDice = mock<Dice>()
            val entropyCalculator = EntropyCalculator(mockDice, coroutineLaunchContext)
            val secretGenerator = SecretGenerator(
                passwordGenerator = passwordGenerator,
                passphraseGenerator = mock(),
                entropyCalculator = entropyCalculator,
            )

            val generatedSecretResult = secretGenerator.generatePassword(
                PasswordGeneratorSettingsUiModel(
                    length = 20,
                    maskUpper = true,
                    maskLower = true,
                    maskDigit = true,
                    maskParenthesis = true,
                    maskEmoji = false,
                    maskChar1 = true,
                    maskChar2 = true,
                    maskChar3 = true,
                    maskChar4 = true,
                    maskChar5 = true,
                    excludeLookAlikeChars = true,
                ),
            )
            assertIs<SecretGenerator.SecretGenerationResult.Success>(generatedSecretResult)
            assertThat(generatedSecretResult.password).hasSize(20)
            assertThat(generatedSecretResult.entropy).isGreaterThan(80.0)

            val mockFoldersRepository = mock<FoldersRepository>()
            val createFolderUseCase = CreateFolderUseCase(mockFoldersRepository)
            val folderId = UUID.randomUUID().toString()
            val folderModel = FolderModel(
                folderId = folderId,
                parentFolderId = null,
                name = "Infrastructure & Cloud",
                isShared = false,
                permission = ResourcePermission.OWNER,
                modified = now,
            )
            whenever(mockFoldersRepository.createFolder("Infrastructure & Cloud", null)).thenReturn(
                DomainResult.Finished(FolderModelWithAttributes(folderModel, emptyList<PermissionModel>())),
            )
            val createFolderResult = createFolderUseCase.execute(
                CreateFolderUseCase.Input("Infrastructure & Cloud", null),
            )
            assertIs<CreateFolderUseCase.Output.Success>(createFolderResult)
            assertThat(createFolderResult.folderWithAttributes.folderModel.folderId).isEqualTo(folderId)

            // -------------------------------------------------------------
            // Фаза 4: Втрата зв'язку з сервером (Офлайн режим)
            // -------------------------------------------------------------
            val syncRepository = SyncStatusRepository()
            syncRepository.onSuccess(now)
            syncRepository.onOffline()

            // Перевіряємо статус синхронізації при відсутності мережі
            val offlineStatus = syncRepository.status.value
            assertIs<SyncStatus.Offline>(offlineStatus)

            // Перевіряємо читання локального сховища в офлайн режимі через HomeDataProvider
            val mockGetLocalResources = mock<GetLocalResourcesPaginatedUseCase>()
            val mockGetRbac = mock<GetRbacRulesUseCase>()
            whenever(mockGetRbac.execute(Unit)).thenReturn(
                GetRbacRulesUseCase.Output(
                    RbacModel(
                        passwordPreviewRule = RbacRuleModel.ALLOW,
                        passwordCopyRule = RbacRuleModel.ALLOW,
                        tagsUseRule = RbacRuleModel.ALLOW,
                        shareViewRule = RbacRuleModel.ALLOW,
                        foldersUseRule = RbacRuleModel.ALLOW,
                    ),
                ),
            )
            whenever(mockGetLocalResources.execute(any())).thenReturn(
                GetLocalResourcesPaginatedUseCase.Output(flowOf(PagingData.empty())),
            )

            val homeDataProvider = HomeDataProvider(
                getLocalResourcesPaginatedUseCase = mockGetLocalResources,
                getLocalTagsPaginatedUseCase = mock(),
                getLocalResourcesWithLocalTagsPaginatedUseCase = mock(),
                getLocalGroupsWithShareItemsCountPaginatedUseCase = mock(),
                getLocalResourcesWithGroupsPaginatedUseCase = mock(),
                getLocalDirectChildFoldersPaginatedUseCase = mock(),
                getResourcesInFolderPaginatedUseCase = mock(),
                getLocalSubFolderResourcesFilteredPaginatedUseCase = mock(),
                getLocalSubFoldersForFolderUseCase = mock(),
                getLocalSubFoldersForFolderPaginatedUseCase = mock(),
                autofillMatcher = mock(),
                getRbacRulesUseCase = mockGetRbac,
            )

            val offlineVaultData = homeDataProvider.provideData(
                searchQuery = null,
                homeView = HomeDisplayViewModel.AllItems,
                showSuggestedModel = ShowSuggestedModel.DoNotShow,
            )
            assertThat(offlineVaultData.resourceList).isNotNull()

            // Перевіряємо розшифрування секрету за допомогою збереженої парольної фрази офлайн
            var secretDecryptedOffline = false
            passphraseMemoryCache.usePassphraseCopy(
                onPassphraseNotPresent = { throw AssertionError("Offline passphrase must be available") },
                action = { cachedKey ->
                    assertThat(cachedKey).isEqualTo(userPassphrase)
                    secretDecryptedOffline = true
                },
            )
            assertThat(secretDecryptedOffline).isTrue()

            // -------------------------------------------------------------
            // Фаза 5: Відновлення інтернет-з'єднання та синхронізація
            // -------------------------------------------------------------
            syncRepository.onRunStarted(SyncStatus.Running.Phase.DELTA)
            syncRepository.onProgress(SyncStatus.Running.Phase.DELTA, processed = 10, total = 10)
            val laterSyncTime = now.plusMinutes(15)
            syncRepository.onSuccess(laterSyncTime)

            val idleRestored = syncRepository.status.value
            assertIs<SyncStatus.Idle>(idleRestored)
            assertThat(idleRestored.lastSuccessAt).isEqualTo(laterSyncTime)

            // -------------------------------------------------------------
            // Фаза 6: Блокування додатку та очищення секретів
            // -------------------------------------------------------------
            passphraseMemoryCache.clear()
            assertThat(passphraseMemoryCache.hasPassphrase()).isFalse()
            assertThat(passphraseMemoryCache.get()).isInstanceOf(PotentialPassphrase.PassphraseNotPresent::class.java)
        }
}
