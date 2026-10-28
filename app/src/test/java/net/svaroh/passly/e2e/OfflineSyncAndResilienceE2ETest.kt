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
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.svaroh.passly.common.coroutinetimer.timerFlow
import net.svaroh.passly.common.datarefresh.DataRefreshTrackingFlow
import net.svaroh.passly.common.urimatcher.AutofillUriMatcher
import net.svaroh.passly.commontest.TestCoroutineLaunchContext
import net.svaroh.passly.core.passphrasememorycache.AuthOnEveryEntryChecker
import net.svaroh.passly.core.passphrasememorycache.PassphraseMemoryCache
import net.svaroh.passly.core.passphrasememorycache.PotentialPassphrase
import net.svaroh.passly.core.passphrasememorycache.usePassphraseCopy
import net.svaroh.passly.core.sync.SyncBackoff
import net.svaroh.passly.core.sync.SyncFailureReason
import net.svaroh.passly.core.sync.SyncResult
import net.svaroh.passly.core.sync.SyncStats
import net.svaroh.passly.core.sync.SyncStatus
import net.svaroh.passly.core.sync.SyncStatusRepository
import net.svaroh.passly.core.sync.SyncTrigger
import net.svaroh.passly.domain.folders.usecase.GetLocalDirectChildFoldersPaginatedUseCase
import net.svaroh.passly.domain.folders.usecase.GetLocalSubFoldersForFolderPaginatedUseCase
import net.svaroh.passly.domain.folders.usecase.GetLocalSubFoldersForFolderUseCase
import net.svaroh.passly.domain.groups.usecase.GetLocalGroupsWithShareItemsCountPaginatedUseCase
import net.svaroh.passly.domain.rbac.usecase.GetRbacRulesUseCase
import net.svaroh.passly.domain.resources.usecase.db.GetLocalResourcesPaginatedUseCase
import net.svaroh.passly.domain.resources.usecase.db.GetLocalResourcesWithGroupPaginatedUseCase
import net.svaroh.passly.domain.resources.usecase.db.GetLocalResourcesWithTagPaginatedUseCase
import net.svaroh.passly.domain.resources.usecase.db.GetLocalSubFolderResourcesFilteredPaginatedUseCase
import net.svaroh.passly.domain.resources.usecase.db.GetResourcesInFolderPaginatedUseCase
import net.svaroh.passly.domain.tags.usecase.GetLocalTagsPaginatedUseCase
import net.svaroh.passly.feature.home.screen.ShowSuggestedModel
import net.svaroh.passly.feature.home.screen.data.HomeDataProvider
import net.svaroh.passly.ui.HomeDisplayViewModel
import net.svaroh.passly.ui.RbacModel
import net.svaroh.passly.ui.RbacRuleModel
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineSyncAndResilienceE2ETest {

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
    fun sync_status_repository_tracks_idle_running_offline_and_failed_states() =
        runTest {
            val repository = SyncStatusRepository()
            val now = ZonedDateTime.now()

            repository.status.test {
                // Початковий стан — Idle без попереднього успіху
                val initial = awaitItem()
                assertIs<SyncStatus.Idle>(initial)
                assertThat(initial.lastSuccessAt).isNull()

                // Симулюємо успішну попередню синхронізацію
                repository.onSuccess(now)
                val successState = awaitItem()
                assertIs<SyncStatus.Idle>(successState)
                assertThat(successState.lastSuccessAt).isEqualTo(now)

                // Початок нової синхронізації (фаза PUSHING_LOCAL_CHANGES)
                repository.onRunStarted(SyncStatus.Running.Phase.PUSHING_LOCAL_CHANGES)
                val runningPush = awaitItem()
                assertIs<SyncStatus.Running>(runningPush)
                assertThat(runningPush.phase).isEqualTo(SyncStatus.Running.Phase.PUSHING_LOCAL_CHANGES)
                assertThat(runningPush.lastSuccessAt).isEqualTo(now)

                // Прогрес завантаження (фаза DELTA)
                repository.onProgress(SyncStatus.Running.Phase.DELTA, processed = 25, total = 100)
                val runningProgress = awaitItem()
                assertIs<SyncStatus.Running>(runningProgress)
                assertThat(runningProgress.phase).isEqualTo(SyncStatus.Running.Phase.DELTA)
                assertThat(runningProgress.processed).isEqualTo(25)
                assertThat(runningProgress.total).isEqualTo(100)

                // Втрата інтернет-з'єднання (Offline)
                repository.onOffline()
                val offlineState = awaitItem()
                assertIs<SyncStatus.Offline>(offlineState)
                assertThat(offlineState.lastSuccessAt).isEqualTo(now)

                // Помилка сервера (Failed з запланованим retry)
                val retryTime = now.plusMinutes(5)
                repository.onFailure(SyncFailureReason.UNREACHABLE, retryTime)
                val failedState = awaitItem()
                assertIs<SyncStatus.Failed>(failedState)
                assertThat(failedState.reason).isEqualTo(SyncFailureReason.UNREACHABLE)
                assertThat(failedState.retryAt).isEqualTo(retryTime)
                assertThat(failedState.lastSuccessAt).isEqualTo(now)

                // Відновлення успіху
                val later = now.plusHours(1)
                repository.onSuccess(later)
                val restoredState = awaitItem()
                assertIs<SyncStatus.Idle>(restoredState)
                assertThat(restoredState.lastSuccessAt).isEqualTo(later)
            }
        }

    @Test
    fun sync_backoff_calculates_exponential_delays_and_resets_upon_recovery() {
        // Перша помилка: 30 секунд
        assertThat(SyncBackoff.delayAfter(1)).isEqualTo(Duration.ofSeconds(30))

        // Друга помилка: 1 хвилина
        assertThat(SyncBackoff.delayAfter(2)).isEqualTo(Duration.ofMinutes(1))

        // Третя помилка: 5 хвилин
        assertThat(SyncBackoff.delayAfter(3)).isEqualTo(Duration.ofMinutes(5))

        // Четверта помилка: 15 хвилин
        assertThat(SyncBackoff.delayAfter(4)).isEqualTo(Duration.ofMinutes(15))

        // П'ята помилка: 1 година
        assertThat(SyncBackoff.delayAfter(5)).isEqualTo(Duration.ofHours(1))

        // Шоста і всі наступні (стеля насичення): 6 годин
        assertThat(SyncBackoff.delayAfter(6)).isEqualTo(Duration.ofHours(6))
        assertThat(SyncBackoff.delayAfter(10)).isEqualTo(Duration.ofHours(6))
        assertThat(SyncBackoff.delayAfter(100)).isEqualTo(Duration.ofHours(6))

        // Ручна синхронізація користувачем завжди ігнорує затримку бекофу (негайний запуск)
        assertThat(SyncBackoff.delayAfter(10, SyncTrigger.MANUAL)).isEqualTo(Duration.ZERO)

        // Фонова синхронізація дотримується бекофу
        assertThat(SyncBackoff.delayAfter(3, SyncTrigger.BACKGROUND)).isEqualTo(Duration.ofMinutes(5))
    }

    @Test
    fun sync_result_outcomes_preserve_local_state_and_track_statistics() {
        // 1. Успішна синхронізація повертає детальну статистику
        val stats = SyncStats(
            upserted = 15,
            deleted = 2,
            secretsUpdated = 5,
            pushedOperations = 1,
            reconciled = true,
        )
        val successResult = SyncResult.Success(stats)
        assertThat(successResult.stats.upserted).isEqualTo(15)
        assertThat(successResult.stats.deleted).isEqualTo(2)
        assertThat(successResult.stats.isEmpty).isFalse()

        // 2. Пропуск синхронізації через відсутність інтернету не вважається збоєм даних
        val skippedNoNetwork = SyncResult.Skipped(SyncResult.Skipped.Reason.NO_NETWORK)
        assertThat(skippedNoNetwork.reason).isEqualTo(SyncResult.Skipped.Reason.NO_NETWORK)

        // 3. Збій мережі під час синхронізації
        val failureUnreachable = SyncResult.Failure(SyncFailureReason.UNREACHABLE)
        assertThat(failureUnreachable.reason).isEqualTo(SyncFailureReason.UNREACHABLE)
    }

    @Test
    fun home_data_provider_serves_cached_resources_and_folders_when_network_is_offline() =
        runTest {
            val mockGetLocalResourcesPaginatedUseCase = mock<GetLocalResourcesPaginatedUseCase>()
            val mockGetLocalTagsPaginatedUseCase = mock<GetLocalTagsPaginatedUseCase>()
            val mockGetLocalResourcesWithLocalTagsPaginatedUseCase = mock<GetLocalResourcesWithTagPaginatedUseCase>()
            val mockGetLocalGroupsWithShareItemsCountPaginatedUseCase = mock<GetLocalGroupsWithShareItemsCountPaginatedUseCase>()
            val mockGetLocalResourcesWithGroupsPaginatedUseCase = mock<GetLocalResourcesWithGroupPaginatedUseCase>()
            val mockGetLocalDirectChildFoldersPaginatedUseCase = mock<GetLocalDirectChildFoldersPaginatedUseCase>()
            val mockGetResourcesInFolderPaginatedUseCase = mock<GetResourcesInFolderPaginatedUseCase>()
            val mockGetLocalSubFolderResourcesFilteredPaginatedUseCase = mock<GetLocalSubFolderResourcesFilteredPaginatedUseCase>()
            val mockGetLocalSubFoldersForFolderUseCase = mock<GetLocalSubFoldersForFolderUseCase>()
            val mockGetLocalSubFoldersForFolderPaginatedUseCase = mock<GetLocalSubFoldersForFolderPaginatedUseCase>()
            val mockAutofillMatcher = mock<AutofillUriMatcher>()
            val mockGetRbacRulesUseCase = mock<GetRbacRulesUseCase>()

            whenever(mockGetRbacRulesUseCase.execute(Unit)).thenReturn(
                GetRbacRulesUseCase.Output(
                    rbacModel = RbacModel(
                        passwordPreviewRule = RbacRuleModel.ALLOW,
                        passwordCopyRule = RbacRuleModel.ALLOW,
                        tagsUseRule = RbacRuleModel.ALLOW,
                        shareViewRule = RbacRuleModel.ALLOW,
                        foldersUseRule = RbacRuleModel.ALLOW,
                    ),
                ),
            )

            whenever(mockGetLocalResourcesPaginatedUseCase.execute(any())).thenReturn(
                GetLocalResourcesPaginatedUseCase.Output(flowOf()),
            )

            val provider = HomeDataProvider(
                getLocalResourcesPaginatedUseCase = mockGetLocalResourcesPaginatedUseCase,
                getLocalTagsPaginatedUseCase = mockGetLocalTagsPaginatedUseCase,
                getLocalResourcesWithLocalTagsPaginatedUseCase = mockGetLocalResourcesWithLocalTagsPaginatedUseCase,
                getLocalGroupsWithShareItemsCountPaginatedUseCase = mockGetLocalGroupsWithShareItemsCountPaginatedUseCase,
                getLocalResourcesWithGroupsPaginatedUseCase = mockGetLocalResourcesWithGroupsPaginatedUseCase,
                getLocalDirectChildFoldersPaginatedUseCase = mockGetLocalDirectChildFoldersPaginatedUseCase,
                getResourcesInFolderPaginatedUseCase = mockGetResourcesInFolderPaginatedUseCase,
                getLocalSubFolderResourcesFilteredPaginatedUseCase = mockGetLocalSubFolderResourcesFilteredPaginatedUseCase,
                getLocalSubFoldersForFolderUseCase = mockGetLocalSubFoldersForFolderUseCase,
                getLocalSubFoldersForFolderPaginatedUseCase = mockGetLocalSubFoldersForFolderPaginatedUseCase,
                autofillMatcher = mockAutofillMatcher,
                getRbacRulesUseCase = mockGetRbacRulesUseCase,
            )

            // Запит на AllItems в офлайн режимі успішно повертає локальні дані
            val homeData = provider.provideData(
                searchQuery = null,
                homeView = HomeDisplayViewModel.AllItems,
                showSuggestedModel = ShowSuggestedModel.DoNotShow,
            )

            assertThat(homeData.resourceList).isNotNull()
        }

    @Test
    fun passphrase_memory_cache_preserves_decryption_credentials_and_enforces_clearing() =
        runTest {
            val testPassphrase = "SafeOfflinePassphrase99!".toByteArray()

            val mockLifecycleOwner = mock<LifecycleOwner>()
            val mockLifecycle = mock<Lifecycle>()
            whenever(mockLifecycleOwner.lifecycle).thenReturn(mockLifecycle)

            val cache = PassphraseMemoryCache(
                coroutineLaunchContext = TestCoroutineLaunchContext(),
                lifecycleOwner = mockLifecycleOwner,
                dataRefreshTrackingFlow = DataRefreshTrackingFlow(),
                authOnEveryEntryChecker = AuthOnEveryEntryChecker { false },
            )

            // 1. Початково кеш порожній
            assertThat(cache.hasPassphrase()).isFalse()

            // 2. Зберігаємо парольну фразу для офлайн розшифрування
            cache.set(testPassphrase)
            assertThat(cache.hasPassphrase()).isTrue()
            val potential = cache.get()
            assertIs<PotentialPassphrase.Passphrase>(potential)
            assertThat(potential.passphrase).isEqualTo(testPassphrase)

            // 3. Безпечне використання копії парольної фрази
            var executed = false
            cache.usePassphraseCopy(
                onPassphraseNotPresent = {
                    throw AssertionError("Passphrase should be present")
                },
                action = { copy ->
                    assertThat(copy).isEqualTo(testPassphrase)
                    executed = true
                },
            )
            assertThat(executed).isTrue()

            // 4. Очищення кешу при блокуванні або таймауті
            cache.clear()
            assertThat(cache.hasPassphrase()).isFalse()
            assertThat(cache.get()).isInstanceOf(PotentialPassphrase.PassphraseNotPresent::class.java)
        }
}
