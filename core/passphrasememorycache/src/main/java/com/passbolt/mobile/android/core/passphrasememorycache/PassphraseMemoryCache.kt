package com.passbolt.mobile.android.core.passphrasememorycache

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.passbolt.mobile.android.common.coroutinetimer.timerFlow
import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.common.extension.erase
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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

class PassphraseMemoryCache(
    coroutineLaunchContext: CoroutineLaunchContext,
    private val lifecycleOwner: LifecycleOwner,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
    private val authOnEveryEntryChecker: AuthOnEveryEntryChecker,
) : DefaultLifecycleObserver {
    private val stateLock = ReentrantLock()
    private var state: State = State.Empty

    private val timerFlow = timerFlow(TIMER_REPEAT_TIMES, TIMER_TICK_MILLIS)

    private val timerJob = SupervisorJob()
    private val timerScope = CoroutineScope(timerJob + coroutineLaunchContext.ui)

    // lifecycle observer remove/add methods need to be called on Main thread (even in Android Tests
    // TestDispatcher must not be injected here - Main dispatcher is obligatory
    private val lifecycleObserverJob = SupervisorJob()
    private val lifecycleObserverScope = CoroutineScope(lifecycleObserverJob + Dispatchers.Main)

    fun set(passphrase: ByteArray) {
        stateLock.withLock {
            clear()
            initializeObservers()
            state = State.Cached(passphrase = passphrase.copyOf(), currentTimerMillis = TIMER_TICK_MILLIS)
        }
        Timber.d("[Session] Passphrase cached")
    }

    fun get() =
        stateLock.withLock {
            when (val current = state) {
                is State.Cached -> PotentialPassphrase.Passphrase(current.passphrase.copyOf())
                is State.Empty -> PotentialPassphrase.PassphraseNotPresent()
            }
        }

    @Suppress("MagicNumber") // second has 1000 millis
    fun getSessionDurationSeconds() =
        stateLock.withLock {
            (state as? State.Cached)?.let {
                (CACHE_EXPIRATION_MILLIS - it.currentTimerMillis) / 1000
            }
        }

    fun hasPassphrase() = stateLock.withLock { state is State.Cached }

    private fun initializeObservers() {
        lifecycleObserverScope.launch {
            lifecycleOwner.lifecycle.addObserver(this@PassphraseMemoryCache)
        }
        timerScope.launch {
            timerFlow.collect { onTimerTick(it) }
            scheduleClear()
        }
    }

    private fun onTimerTick(tick: Long) {
        stateLock.withLock {
            (state as? State.Cached)?.currentTimerMillis = tick * TIMER_TICK_MILLIS
        }
    }

    fun clear() {
        stateLock.withLock {
            (state as? State.Cached)?.passphrase?.erase()
            state = State.Empty
            lifecycleObserverScope.launch {
                lifecycleOwner.lifecycle.removeObserver(this@PassphraseMemoryCache)
            }
            timerScope.coroutineContext.cancelChildren()
            lifecycleObserverScope.coroutineContext.cancelChildren()
        }
        Timber.d("[Session] Passphrase cache cleared")
    }

    private suspend fun scheduleClear() {
        Timber.d("[Session] Scheduling passphrase cache clear")
        dataRefreshTrackingFlow.awaitIdle()
        clear()
    }

    override fun onStop(owner: LifecycleOwner) {
        Timber.d("[Session] App went background")
        if (authOnEveryEntryChecker.isRequired()) {
            timerScope.launch { scheduleClear() }
        }
    }

    private sealed class State {
        data object Empty : State()

        class Cached(
            val passphrase: ByteArray,
            var currentTimerMillis: Long,
        ) : State()
    }

    companion object {
        @VisibleForTesting
        const val CACHE_EXPIRATION_MILLIS = 1_000 * 60 * 5L

        private const val TIMER_TICK_MILLIS = 5_000L
        private const val TIMER_REPEAT_TIMES = CACHE_EXPIRATION_MILLIS / TIMER_TICK_MILLIS
    }
}

inline fun <T> PassphraseMemoryCache.usePassphraseCopy(
    onPassphraseNotPresent: () -> T,
    action: (passphraseCopy: ByteArray) -> T,
): T =
    when (val potentialPassphrase = get()) {
        is PotentialPassphrase.Passphrase ->
            try {
                action(potentialPassphrase.passphrase)
            } finally {
                potentialPassphrase.passphrase.erase()
            }
        is PotentialPassphrase.PassphraseNotPresent -> onPassphraseNotPresent()
    }
