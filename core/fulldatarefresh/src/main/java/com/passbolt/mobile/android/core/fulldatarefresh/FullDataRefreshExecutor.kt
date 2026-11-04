package net.svaroh.passly.core.fulldatarefresh

import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.Idle.FinishedWithFailure
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.Idle.FinishedWithSuccess
import net.svaroh.passly.common.datarefresh.DataRefreshStatus.InProgress
import net.svaroh.passly.common.datarefresh.DataRefreshTrackingFlow
import net.svaroh.passly.common.usecase.UserIdInput
import net.svaroh.passly.core.fulldatarefresh.HomeDataInteractor.Output.Failure
import net.svaroh.passly.core.fulldatarefresh.HomeDataInteractor.Output.Success
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.core.sync.SyncEngine
import net.svaroh.passly.core.sync.SyncResult
import net.svaroh.passly.core.sync.SyncTrigger
import net.svaroh.passly.database.usecase.HasLocalReplicaUseCase
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import net.svaroh.passly.feature.authentication.session.runAuthenticatedOperation
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

/**
 * Executes data refresh requests according to the offline-first autonomy policy:
 * - Full initial data refresh is ONLY executed when local replica data is completely absent.
 * - Whenever local data is present, lightweight incremental delta synchronization is performed via SyncEngine.
 */
class FullDataRefreshExecutor(
    private val homeDataInteractor: HomeDataInteractor,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
    private val coroutineLaunchContext: CoroutineLaunchContext,
    private val syncEngine: SyncEngine,
    private val hasLocalReplicaUseCase: HasLocalReplicaUseCase,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
) {
    suspend fun performFullDataRefresh(isUserInitiated: Boolean = false) {
        Timber.d("Data refresh initiated (isUserInitiated=$isUserInitiated)")
        if (!dataRefreshTrackingFlow.isInProgress()) {
            dataRefreshTrackingFlow.startTracking(isUserInitiated)

            val selectedAccount = getSelectedAccountUseCase.execute(Unit).selectedAccount
            val hasData = if (selectedAccount != null) {
                hasLocalReplicaUseCase.execute(UserIdInput(selectedAccount)).hasLocalReplica
            } else {
                false
            }

            if (hasData) {
                Timber.d("Local data present: performing fast incremental delta sync")
                dataRefreshTrackingFlow.updateStatus(InProgress(progress = 0.5f))
                val syncResult = syncEngine.sync(SyncTrigger.MANUAL)
                when (syncResult) {
                    is SyncResult.Success, is SyncResult.Skipped -> {
                        dataRefreshTrackingFlow.updateStatus(InProgress(progress = 1f))
                        delay(FULL_PROGRESS_DISPLAY_MILLIS.milliseconds)
                        dataRefreshTrackingFlow.updateStatus(FinishedWithSuccess)
                    }
                    is SyncResult.Failure -> {
                        Timber.w("Delta sync returned failure during refresh: ${syncResult.reason}")
                        dataRefreshTrackingFlow.updateStatus(FinishedWithFailure)
                    }
                }
            } else {
                Timber.i("No local data found: performing full initial bootstrap / data refresh")
                val output =
                    runAuthenticatedOperation(canPromptForAuthentication = isUserInitiated) {
                        withContext(coroutineLaunchContext.default) {
                            homeDataInteractor.refreshAllHomeScreenData { progress ->
                                dataRefreshTrackingFlow.updateStatus(InProgress(progress))
                            }
                        }
                    }

                when (output) {
                    is Success -> {
                        // After full initial refresh, trigger sync to establish initial watermark/cursor
                        syncEngine.sync(SyncTrigger.MANUAL)
                        dataRefreshTrackingFlow.updateStatus(InProgress(progress = 1f))
                        delay(FULL_PROGRESS_DISPLAY_MILLIS.milliseconds)
                        dataRefreshTrackingFlow.updateStatus(FinishedWithSuccess)
                    }
                    is Failure -> dataRefreshTrackingFlow.updateStatus(FinishedWithFailure)
                }
            }
        }
    }

    private companion object {
        private const val FULL_PROGRESS_DISPLAY_MILLIS = 300L
    }
}
