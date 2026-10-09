package net.svaroh.passly.core.fulldatarefresh

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

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
class RefreshProgressTrackerTest {
    @Test
    fun `completed steps should emit equal progress segments`() =
        runTest {
            val emittedProgress = mutableListOf<Float>()
            val counter = RefreshProgressTracker(totalSteps = 4) { emittedProgress.add(it) }

            counter.onStepCompleted()
            counter.onStepCompleted()

            assertThat(emittedProgress).containsExactly(0.25f, 0.5f).inOrder()
        }

    @Test
    fun `skipped steps should advance progress by skipped count`() =
        runTest {
            val emittedProgress = mutableListOf<Float>()
            val counter = RefreshProgressTracker(totalSteps = 4) { emittedProgress.add(it) }

            counter.onStepsSkipped(count = 2)

            assertThat(emittedProgress).containsExactly(0.5f)
        }

    @Test
    fun `downloaded pages should fill current step segment gradually`() =
        runTest {
            val emittedProgress = mutableListOf<Float>()
            val counter = RefreshProgressTracker(totalSteps = 2) { emittedProgress.add(it) }

            counter.onStepPageDownloaded(downloadedPages = 1, totalPages = 4)
            counter.onStepPageDownloaded(downloadedPages = 2, totalPages = 4)
            counter.onStepCompleted()

            assertThat(emittedProgress).containsExactly(0.125f, 0.25f, 0.5f).inOrder()
        }

    @Test
    fun `step completion after pages should reset step fraction`() =
        runTest {
            val emittedProgress = mutableListOf<Float>()
            val counter = RefreshProgressTracker(totalSteps = 2) { emittedProgress.add(it) }

            counter.onStepPageDownloaded(downloadedPages = 4, totalPages = 4)
            counter.onStepCompleted()
            counter.onStepCompleted()

            assertThat(emittedProgress).containsExactly(0.5f, 0.5f, 1f).inOrder()
        }

    @Test
    fun `zero total pages should count as fully downloaded step`() =
        runTest {
            val emittedProgress = mutableListOf<Float>()
            val counter = RefreshProgressTracker(totalSteps = 2) { emittedProgress.add(it) }

            counter.onStepPageDownloaded(downloadedPages = 1, totalPages = 0)

            assertThat(emittedProgress).containsExactly(0.5f)
        }

    @Test
    fun `progress should not exceed one`() =
        runTest {
            val emittedProgress = mutableListOf<Float>()
            val counter = RefreshProgressTracker(totalSteps = 2) { emittedProgress.add(it) }

            counter.onStepCompleted()
            counter.onStepCompleted()
            counter.onStepCompleted()

            assertThat(emittedProgress).containsExactly(0.5f, 1f, 1f).inOrder()
        }
}
