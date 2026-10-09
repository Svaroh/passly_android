package net.svaroh.passly.core.passphrasememorycache

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import net.svaroh.passly.core.dummy.TestActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject

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

@ExperimentalCoroutinesApi
class PassphraseMemoryCacheTest : KoinTest {
    private val passphraseMemoryCache: PassphraseMemoryCache by inject()

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testPassphraseMemoryCacheModule)
        }

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(TestActivity::class.java)

    @Before
    fun setup() {
        testIsAuthRequiredOnEveryEntry = true
        val scenario = activityScenarioRule.scenario
        scenario.moveToState(Lifecycle.State.RESUMED)
    }

    @Test
    fun test_passphraseIsCachedForSetDuration() =
        runTest(testCoroutineLaunchContext.ui) {
            passphraseMemoryCache.set(TEST_PASSPHRASE)

            // advance time on the timer thread to just before cache expiration
            advanceTimeBy(PassphraseMemoryCache.CACHE_EXPIRATION_MILLIS - 1)

            assertThat(passphraseMemoryCache.get()).isInstanceOf(PotentialPassphrase.Passphrase::class.java)
            assertThat((passphraseMemoryCache.get() as PotentialPassphrase.Passphrase).passphrase)
                .isEqualTo(TEST_PASSPHRASE)
        }

    @Test
    fun test_cacheIsClearedAfterSetDuration() =
        runTest(testCoroutineLaunchContext.ui) {
            passphraseMemoryCache.set(TEST_PASSPHRASE)

            // advance time on the timer thread to just after cache expiration
            advanceTimeBy(PassphraseMemoryCache.CACHE_EXPIRATION_MILLIS + 1)

            assertThat(passphraseMemoryCache.get()).isInstanceOf(PotentialPassphrase.PassphraseNotPresent::class.java)
        }

    @Test
    fun test_CacheIsClearedAfterAppIsInBackground() =
        runBlocking {
            passphraseMemoryCache.set(TEST_PASSPHRASE)

            // start launcher app
            InstrumentationRegistry
                .getInstrumentation()
                .context
                .startActivity(launcherIntent())

            delay(LIFECYCLE_OBSERVATION_TIMEOUT_MILLIS)
            assertThat(passphraseMemoryCache.get()).isInstanceOf(PotentialPassphrase.PassphraseNotPresent::class.java)
        }

    @Test
    fun test_CacheIsClearedAfterAppIsDestroyed() =
        runBlocking {
            passphraseMemoryCache.set(TEST_PASSPHRASE)

            val scenario = activityScenarioRule.scenario
            scenario.moveToState(Lifecycle.State.DESTROYED)

            delay(LIFECYCLE_OBSERVATION_TIMEOUT_MILLIS)
            assertThat(passphraseMemoryCache.get()).isInstanceOf(PotentialPassphrase.PassphraseNotPresent::class.java)
        }

    @Test
    fun test_CacheIsNotClearedAfterAppIsInBackground_WhenAuthNotRequiredOnEveryEntry() =
        runBlocking {
            testIsAuthRequiredOnEveryEntry = false
            passphraseMemoryCache.set(TEST_PASSPHRASE)

            // start launcher app to send app to background
            InstrumentationRegistry
                .getInstrumentation()
                .context
                .startActivity(launcherIntent())

            delay(LIFECYCLE_OBSERVATION_TIMEOUT_MILLIS)
            assertThat(passphraseMemoryCache.get()).isInstanceOf(PotentialPassphrase.Passphrase::class.java)
        }

    @Test
    fun test_CacheIsClearedByTimerExpiry_WhenAuthNotRequiredOnEveryEntry() =
        runTest(testCoroutineLaunchContext.ui) {
            testIsAuthRequiredOnEveryEntry = false
            passphraseMemoryCache.set(TEST_PASSPHRASE)

            // 5-minute timer should still clear the cache regardless of the flag
            advanceTimeBy(PassphraseMemoryCache.CACHE_EXPIRATION_MILLIS + 1)

            assertThat(passphraseMemoryCache.get()).isInstanceOf(PotentialPassphrase.PassphraseNotPresent::class.java)
        }

    @Test
    fun test_CacheTimeoutIsRenewedAfterNewPassphraseValueIsSet() =
        runTest(testCoroutineLaunchContext.ui) {
            passphraseMemoryCache.set(TEST_PASSPHRASE)

            // advance time on the timer thread to just before cache expiration
            advanceTimeBy(PassphraseMemoryCache.CACHE_EXPIRATION_MILLIS - 1)

            // set new passphrase
            passphraseMemoryCache.set(TEST_PASSPHRASE)
            // advance time on the timer thread to just before cache expiration
            advanceTimeBy(PassphraseMemoryCache.CACHE_EXPIRATION_MILLIS - 1)

            assertThat(passphraseMemoryCache.get()).isInstanceOf(PotentialPassphrase.Passphrase::class.java)
        }

    @Test
    fun test_ConcurrentReadNeverReturnsPartiallyErasedPassphrase() =
        runBlocking {
            passphraseMemoryCache.set(LONG_PASSPHRASE)

            coroutineScope {
                val readers =
                    List(STRESS_READER_COUNT) {
                        launch(Dispatchers.Default) {
                            repeat(STRESS_ITERATIONS) {
                                (passphraseMemoryCache.get() as? PotentialPassphrase.Passphrase)?.let {
                                    assertThat(it.passphrase).isEqualTo(LONG_PASSPHRASE)
                                }
                            }
                        }
                    }
                val writer =
                    launch(Dispatchers.Default) {
                        repeat(STRESS_ITERATIONS) {
                            passphraseMemoryCache.clear()
                            passphraseMemoryCache.set(LONG_PASSPHRASE)
                        }
                    }

                (readers + writer).joinAll()
            }
        }

    private companion object {
        private val TEST_PASSPHRASE = "passphrase".toByteArray()
        private const val LIFECYCLE_OBSERVATION_TIMEOUT_MILLIS = 1_000L

        private const val STRESS_READER_COUNT = 4
        private const val STRESS_ITERATIONS = 2_000
        private const val LONG_PASSPHRASE_REPEATS = 400
        private val LONG_PASSPHRASE = "passphrase".repeat(LONG_PASSPHRASE_REPEATS).toByteArray()

        fun launcherIntent() =
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = FLAG_ACTIVITY_NEW_TASK
            }
    }
}
