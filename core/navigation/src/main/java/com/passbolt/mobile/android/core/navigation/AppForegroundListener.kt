package net.svaroh.passly.core.navigation

import android.app.Activity
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AppForegroundListener : StartedStoppedCallback() {
    private var startedActivities = 0
    private var isConfigurationChanging = false
    private var _appWentForegroundFlow =
        MutableSharedFlow<Activity>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    private var _appWentBackgroundFlow =
        MutableSharedFlow<Unit>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    val appWentForegroundFlow =
        _appWentForegroundFlow
            .asSharedFlow()

    val appWentBackgroundFlow =
        _appWentBackgroundFlow
            .asSharedFlow()

    fun isForeground(): Boolean = startedActivities > 0

    override fun onActivityStarted(activity: Activity) {
        if (++startedActivities == 1) {
            if (isConfigurationChanging) {
                isConfigurationChanging = false
            } else {
                _appWentForegroundFlow.tryEmit(activity)
            }
        }
    }

    override fun onActivityStopped(activity: Activity) {
        --startedActivities
        if (activity.isChangingConfigurations) {
            isConfigurationChanging = true
        } else if (startedActivities == 0) {
            _appWentBackgroundFlow.tryEmit(Unit)
        }
    }
}
