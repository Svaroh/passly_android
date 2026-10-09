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

import net.svaroh.passly.benchmark.pagesize.appstate.LocalResourceStore
import net.svaroh.passly.domain.resources.usecase.ResourceInteractor

class PageSizeSweep(
    private val measuredResourceRefresh: MeasuredResourceRefresh,
    private val localResourceStore: LocalResourceStore,
    private val recorder: BenchmarkResultRecorder,
    private val oomHandler: OomSurvivingUncaughtHandler,
    private val runBudget: RunBudget,
) {
    fun run(
        pageSizes: List<Int>,
        totalResources: Int,
    ) {
        recorder.start()
        val ordered = pageSizes.sortedDescending()
        var lastCompletedDurationMs: Double? = null
        var fastestCompletedPageSize: Int? = null
        var fastestDurationMs = Double.MAX_VALUE
        var consecutiveTimeFailures = 0
        for (position in ordered.indices) {
            val pageSize = ordered[position]
            if (!hasBudgetForRow(lastCompletedDurationMs)) {
                skipRemaining(ordered, position, SKIPPED_RUN_BUDGET)
                return
            }
            val row = measureRow(pageSize, totalResources)
            recorder.append(row)
            val durationMs = row.durationMs
            if (row.outcome == RowOutcome.OK && durationMs != null) {
                lastCompletedDurationMs = durationMs
                if (durationMs < fastestDurationMs) {
                    fastestDurationMs = durationMs
                    fastestCompletedPageSize = pageSize
                }
            }
            consecutiveTimeFailures = if (row.outcome == RowOutcome.TIME_FAILURE) consecutiveTimeFailures + 1 else 0
            // duration only climbs as the page shrinks BELOW the throughput optimum, so above it a slow row proves nothing
            val optimumPageSize = fastestCompletedPageSize
            if (optimumPageSize != null && pageSize <= optimumPageSize && consecutiveTimeFailures >= MAX_CONSECUTIVE_TIME_FAILURES) {
                skipRemaining(ordered, position + 1, SKIPPED_AFTER_TIME_FAILURES)
                return
            }
        }
    }

    private fun hasBudgetForRow(lastCompletedDurationMs: Double?): Boolean {
        if (runBudget.remainingMs() < MIN_ROW_BUDGET_MS) return false
        val estimateMs = lastCompletedDurationMs?.times(NEXT_ROW_SLOWDOWN)?.toLong() ?: return true
        return runBudget.hasRoomFor(estimateMs)
    }

    private fun skipRemaining(
        ordered: List<Int>,
        from: Int,
        note: String,
    ) = (from until ordered.size).forEach { position ->
        recorder.append(
            SweepRow(
                pageSize = ordered[position],
                sample = null,
                durationMs = null,
                pagesProcessed = null,
                note = note,
                outcome = RowOutcome.SKIPPED,
            ),
        )
    }

    private fun measureRow(
        pageSize: Int,
        totalResources: Int,
    ): SweepRow {
        val rowCeilingMs = minOf(MAX_ROW_MS, runBudget.remainingMs())
        val attempt =
            runCatching {
                val measurement = measuredResourceRefresh.measure(pageSize, totalResources, rowCeilingMs)
                measurement to localResourceStore.count()
            }
        // consumed once per row: an OOM seen anywhere outranks whatever the row's own coroutine reported
        val oomSeen = oomHandler.consumeOomSeen()
        return attempt.fold(
            onSuccess = { (measurement, persisted) -> completedRow(pageSize, measurement, persisted, totalResources, oomSeen) },
            onFailure = { throwable -> abandonedRow(pageSize, throwable, oomSeen) },
        )
    }

    private fun completedRow(
        pageSize: Int,
        measurement: RefreshMeasurement,
        persisted: Int,
        totalResources: Int,
        oomSeen: Boolean,
    ): SweepRow {
        val (note, outcome) =
            when {
                oomSeen -> PAGE_TOO_LARGE to RowOutcome.MEMORY_CEILING
                measurement.output !is ResourceInteractor.Output.Success -> FETCH_FAILED to RowOutcome.FIXTURE_FAILURE
                persisted != totalResources -> "$DECRYPT_DROPPED($persisted)" to RowOutcome.FIXTURE_FAILURE
                else -> OK to RowOutcome.OK
            }
        return SweepRow(
            pageSize = pageSize,
            sample = measurement.sample,
            durationMs = measurement.durationMs,
            pagesProcessed = measurement.pagesProcessed,
            note = note,
            outcome = outcome,
        )
    }

    private fun abandonedRow(
        pageSize: Int,
        throwable: Throwable,
        oomSeen: Boolean,
    ): SweepRow {
        System.gc()
        val (note, outcome) =
            when {
                oomSeen || throwable is PageTooLargeException || throwable.isOrCausedByOutOfMemory() ->
                    PAGE_TOO_LARGE to RowOutcome.MEMORY_CEILING
                throwable is RowStalledException -> STALLED to RowOutcome.TIME_FAILURE
                throwable is RowCeilingExceededException -> ROW_CEILING to RowOutcome.TIME_FAILURE
                else -> throwable.javaClass.simpleName to RowOutcome.FIXTURE_FAILURE
            }
        return SweepRow(
            pageSize = pageSize,
            sample = measuredResourceRefresh.lastSample,
            durationMs = null,
            pagesProcessed = measuredResourceRefresh.lastPagesProcessed,
            note = note,
            outcome = outcome,
        )
    }

    private companion object {
        private const val OK = "ok"
        private const val PAGE_TOO_LARGE = "page-too-large"
        private const val STALLED = "stalled"
        private const val ROW_CEILING = "row-ceiling"
        private const val FETCH_FAILED = "fetch-failed"
        private const val DECRYPT_DROPPED = "decrypt-dropped"
        private const val SKIPPED_RUN_BUDGET = "skipped-run-budget"
        private const val SKIPPED_AFTER_TIME_FAILURES = "skipped-after-time-failures"
        private const val MAX_CONSECUTIVE_TIME_FAILURES = 2
        private const val MAX_ROW_MS = 600_000L
        private const val MIN_ROW_BUDGET_MS = 60_000L
        private const val NEXT_ROW_SLOWDOWN = 1.6
    }
}

data class SweepRow(
    val pageSize: Int,
    val sample: HeapSampler.Sample?,
    val durationMs: Double?,
    val pagesProcessed: Int?,
    val note: String,
    val outcome: RowOutcome,
)

enum class RowOutcome {
    OK,
    MEMORY_CEILING,
    TIME_FAILURE,
    FIXTURE_FAILURE,
    SKIPPED,
}
