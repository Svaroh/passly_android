package net.svaroh.passly.core.fulldatarefresh

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.svaroh.passly.common.datarefresh.DataRefreshStatus
import net.svaroh.passly.common.datarefresh.DataRefreshTrackingFlow
import net.svaroh.passly.common.usecase.UserIdInput
import net.svaroh.passly.commontest.TestCoroutineLaunchContext
import net.svaroh.passly.core.mvp.authentication.SessionRefreshTrackingFlow
import net.svaroh.passly.core.navigation.AppForegroundListener
import net.svaroh.passly.core.passphrasememorycache.PassphraseMemoryCache
import net.svaroh.passly.core.sync.SyncEngine
import net.svaroh.passly.core.sync.SyncResult
import net.svaroh.passly.core.sync.SyncStats
import net.svaroh.passly.core.sync.SyncTrigger
import net.svaroh.passly.database.usecase.HasLocalReplicaUseCase
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.GetSessionExpiryUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.RefreshSessionUseCase
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class FullDataRefreshExecutorTest : KoinTest {

    private val mockPassphraseMemoryCache = mock<PassphraseMemoryCache>()
    private val mockGetSessionExpiryUseCase = mock<GetSessionExpiryUseCase>()
    private val mockRefreshSessionUseCase = mock<RefreshSessionUseCase>()
    private val mockAppForegroundListener = mock<AppForegroundListener>()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger(Level.ERROR)
        modules(
            module {
                single { mockGetSessionExpiryUseCase }
                single { mockPassphraseMemoryCache }
                single { mockRefreshSessionUseCase }
                single { mockAppForegroundListener }
                singleOf(::SessionRefreshTrackingFlow)
            },
        )
    }

    private val testCoroutineLaunchContext = TestCoroutineLaunchContext()
    private val mockHomeDataInteractor = mock<HomeDataInteractor>()
    private val dataRefreshTrackingFlow = DataRefreshTrackingFlow()
    private val mockSyncEngine = mock<SyncEngine>()
    private val mockHasLocalReplicaUseCase = mock<HasLocalReplicaUseCase>()
    private val mockGetSelectedAccountUseCase = mock<GetSelectedAccountUseCase>()

    private val executor by lazy {
        FullDataRefreshExecutor(
            homeDataInteractor = mockHomeDataInteractor,
            dataRefreshTrackingFlow = dataRefreshTrackingFlow,
            coroutineLaunchContext = testCoroutineLaunchContext,
            syncEngine = mockSyncEngine,
            hasLocalReplicaUseCase = mockHasLocalReplicaUseCase,
            getSelectedAccountUseCase = mockGetSelectedAccountUseCase,
        )
    }

    @Test
    fun `when local data exists - only fast delta sync is executed, full refresh is NOT called`() =
        runTest(testCoroutineLaunchContext.ui) {
            val userId = "user-123"
            whenever(mockGetSelectedAccountUseCase.execute(Unit))
                .thenReturn(GetSelectedAccountUseCase.Output(userId))
            whenever(mockHasLocalReplicaUseCase.execute(UserIdInput(userId)))
                .thenReturn(HasLocalReplicaUseCase.Output(hasLocalReplica = true))
            whenever(mockSyncEngine.sync(SyncTrigger.MANUAL))
                .thenReturn(SyncResult.Success(SyncStats(upserted = 1)))

            executor.performFullDataRefresh(isUserInitiated = true)

            // Verify delta sync was called
            verify(mockSyncEngine).sync(SyncTrigger.MANUAL)
            // Verify full refresh was strictly NOT called!
            verify(mockHomeDataInteractor, never()).refreshAllHomeScreenData(any())

            assertThat(dataRefreshTrackingFlow.dataRefreshStatusFlow.value)
                .isEqualTo(DataRefreshStatus.Idle.FinishedWithSuccess)
        }

    @Test
    fun `when local data is absent - full initial data refresh is executed`() =
        runTest {
            val userId = "user-123"
            whenever(mockAppForegroundListener.isForeground()) doReturn true
            whenever(mockGetSessionExpiryUseCase.execute(Unit)).thenReturn(
                GetSessionExpiryUseCase.Output.JwtWillExpire(ZonedDateTime.now().plusSeconds(60L)),
            )
            whenever(mockPassphraseMemoryCache.getSessionDurationSeconds()).thenReturn(60L)
            whenever(mockGetSelectedAccountUseCase.execute(Unit))
                .thenReturn(GetSelectedAccountUseCase.Output(userId))
            whenever(mockHasLocalReplicaUseCase.execute(UserIdInput(userId)))
                .thenReturn(HasLocalReplicaUseCase.Output(hasLocalReplica = false))
            whenever(mockHomeDataInteractor.refreshAllHomeScreenData(any()))
                .thenReturn(HomeDataInteractor.Output.Success)
            whenever(mockSyncEngine.sync(SyncTrigger.MANUAL))
                .thenReturn(SyncResult.Success(SyncStats.EMPTY))

            executor.performFullDataRefresh(isUserInitiated = true)

            // Verify full data refresh was called for bootstrapping
            verify(mockHomeDataInteractor).refreshAllHomeScreenData(any())

            assertThat(dataRefreshTrackingFlow.dataRefreshStatusFlow.value)
                .isEqualTo(DataRefreshStatus.Idle.FinishedWithSuccess)
        }
}
