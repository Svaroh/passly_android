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

package net.svaroh.passly

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class BackgroundGracePeriodTimer(
    coroutineLaunchContext: CoroutineLaunchContext,
) {
    private val scope = CoroutineScope(SupervisorJob() + coroutineLaunchContext.ui)
    private var gracePeriodJob: Job? = null
    private val withinGracePeriod = AtomicBoolean(false)

    fun start() {
        gracePeriodJob?.cancel()
        withinGracePeriod.set(true)
        Timber.d("[Session] Background authentication grace period started")
        gracePeriodJob =
            scope.launch {
                delay(GRACE_PERIOD_MILLIS)
                withinGracePeriod.set(false)
                gracePeriodJob = null
                Timber.d("[Session] Background authentication grace period expired")
            }
    }

    fun reset() {
        gracePeriodJob?.cancel()
        withinGracePeriod.set(false)
    }

    fun isWithinGracePeriod(): Boolean = withinGracePeriod.get()

    companion object {
        @VisibleForTesting
        const val GRACE_PERIOD_MILLIS = 1_000 * 60L
    }
}
