/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2026 Passbolt SA
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

package net.svaroh.passly.benchmark.pagesize.sweep

import net.svaroh.passly.benchmark.pagesize.appstate.DecryptionPassphrase
import net.svaroh.passly.benchmark.pagesize.appstate.LocalResourceStore
import net.svaroh.passly.benchmark.pagesize.fixture.ResourceCorpus
import net.svaroh.passly.domain.preferences.GlobalPreferencesUpdate
import net.svaroh.passly.domain.preferences.usecase.UpdateGlobalPreferencesUseCase
import net.svaroh.passly.domain.resources.usecase.ResourceInteractor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import java.util.Locale
import java.util.UUID

class MeasuredResourceRefresh(
    private val resourceInteractor: ResourceInteractor,
    private val updateGlobalPreferencesUseCase: UpdateGlobalPreferencesUseCase,
    private val corpus: ResourceCorpus,
    private val localResourceStore: LocalResourceStore,
    private val decryptionPassphrase: DecryptionPassphrase,
    private val heapSampler: HeapSampler,
    private val seedSessionKeys: (List<UUID>) -> Unit,
    private val oomHandler: OomSurvivingUncaughtHandler,
    private val watchdog: RowWatchdog,
) {
    val lastPagesProcessed: Int get() = watchdog.pagesProcessed

    val lastSample: HeapSampler.Sample? get() = heapSampler.lastSample

    fun measure(
        pageSize: Int,
        totalResources: Int,
        rowCeilingMs: Long = DEFAULT_ROW_CEILING_MS,
    ): RefreshMeasurement {
        corpus.ids = List(totalResources) { UUID.randomUUID() }
        seedSessionKeys(corpus.ids)
        updateGlobalPreferencesUseCase.execute(GlobalPreferencesUpdate(apiFetchPageSize = pageSize))
        localResourceStore.clear()
        decryptionPassphrase.keepAlive()

        // pre-allocated so cancelling on OOM needs no allocation on an already-exhausted heap
        val oomCause = PageTooLargeException()
        val stallCause = RowStalledException()
        val ceilingCause = RowCeilingExceededException()
        val startNanos = System.nanoTime()
        val (output, sample) =
            heapSampler.measure {
                runBlocking {
                    // OOM swallowed on the parse thread, abandon this row now instead
                    oomHandler.onOutOfMemory { cancel(oomCause) }
                    // a merely slow row must never be cancelled - only one that stops making page progress, or overruns the run
                    watchdog.start(
                        rowCeilingMs = rowCeilingMs,
                        onStalled = { cancel(stallCause) },
                        onCeilingExceeded = { cancel(ceilingCause) },
                    )
                    try {
                        resourceInteractor.fetchAndSaveResources { processedPages, totalPages ->
                            watchdog.recordProgress(processedPages)
                            logProgress(pageSize, totalResources, processedPages, totalPages, startNanos)
                        }
                    } finally {
                        watchdog.stop()
                        oomHandler.onOutOfMemory(null)
                    }
                }
            }
        val durationMs = (System.nanoTime() - startNanos) / NANOS_IN_MS
        return RefreshMeasurement(
            output = output,
            sample = sample,
            durationMs = durationMs,
            pagesProcessed = watchdog.pagesProcessed,
        )
    }

    private fun logProgress(
        pageSize: Int,
        totalResources: Int,
        processedPages: Int,
        totalPages: Int,
        startNanos: Long,
    ) {
        val elapsedMs = (System.nanoTime() - startNanos) / NANOS_IN_MS
        val processedResources = minOf(processedPages.toLong() * pageSize, totalResources.toLong())
        logBenchmark(
            "progress pageSize=$pageSize page=$processedPages/$totalPages " +
                "elapsedS=${format(elapsedMs / MS_IN_S)} msPerResource=${format(elapsedMs / processedResources)}",
        )
    }

    private fun format(value: Double): String = String.format(Locale.US, "%.1f", value)

    private companion object {
        private const val DEFAULT_ROW_CEILING_MS = 600_000L
        private const val NANOS_IN_MS = 1_000_000.0
        private const val MS_IN_S = 1_000.0
    }
}

data class RefreshMeasurement(
    val output: ResourceInteractor.Output,
    val sample: HeapSampler.Sample,
    val durationMs: Double,
    val pagesProcessed: Int,
)

class PageTooLargeException : CancellationException("Page too large - OutOfMemoryError swallowed, row abandoned")

class RowStalledException : CancellationException("No page completed within the stall window - row abandoned")

class RowCeilingExceededException : CancellationException("Row exceeded its time ceiling - row abandoned")
