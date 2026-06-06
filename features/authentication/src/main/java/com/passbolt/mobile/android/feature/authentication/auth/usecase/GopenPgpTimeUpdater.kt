package net.svaroh.passly.feature.authentication.auth.usecase

import androidx.annotation.VisibleForTesting
import net.svaroh.passly.common.time.TimeProvider
import net.svaroh.passly.gopenpgp.OpenPgp
import timber.log.Timber
import kotlin.math.abs

class GopenPgpTimeUpdater(
    private val openPgp: OpenPgp,
    private val timeProvider: TimeProvider,
) {
    fun updateTimeIfNeeded(
        serverTimeSeconds: Long,
        getTimeRequestDurationSeconds: Long,
    ): Result {
        val deviceTimeSeconds = timeProvider.getCurrentEpochSeconds()
        val timeDeltaSeconds = serverTimeSeconds - deviceTimeSeconds - getTimeRequestDurationSeconds

        return if (abs(timeDeltaSeconds) <= TIME_DELTA_FOR_LOCAL_SYNC_SECS) {
            Timber.d("Local time sync needed. Adjusted: $timeDeltaSeconds")
            openPgp.setTimeOffsetSeconds(timeDeltaSeconds)
            Result.TIME_SYNCED
        } else {
            Timber.d("Time delta to big for sync: $timeDeltaSeconds. Showing error.")
            Result.TIME_DELTA_TOO_BIG_FOR_SYNC
        }
    }

    enum class Result {
        TIME_SYNCED,
        TIME_DELTA_TOO_BIG_FOR_SYNC,
    }

    companion object {
        @VisibleForTesting
        const val TIME_DELTA_FOR_LOCAL_SYNC_SECS = 10
    }
}
