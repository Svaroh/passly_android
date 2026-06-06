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

import net.svaroh.passly.benchmark.pagesize.fixture.MetadataProfile
import java.io.File
import java.util.Locale

class CsvBenchmarkResultRecorder(
    private val file: File,
    private val fingerprint: BenchmarkDeviceFingerprint,
    private val profile: MetadataProfile,
    private val metadataPlaintextBytes: Int,
    private val poolSize: Int,
    private val totalResourcesPerRun: Int,
    private val sessionKeyMode: String,
    private val runBudgetMs: Long,
) : BenchmarkResultRecorder {
    private val report = StringBuilder()

    override fun start() {
        val header = buildHeader()
        report.append(header)
        // echo live so completed rows are visible in logcat even if a later row takes the process/device down
        logBenchmarkBlock(header)
        runCatching { file.writeText(header) }
            .onFailure { println("Could not open benchmark CSV: ${it.message}") }
    }

    override fun append(row: SweepRow) {
        val formattedRow = formatRow(row)
        report.append(formattedRow)
        logBenchmarkBlock(formattedRow)
        runCatching { file.appendText(formattedRow) }
            .onFailure { println("Could not append benchmark row: ${it.message}") }
    }

    override fun snapshot(): String = report.toString()

    private fun buildHeader(): String =
        buildString {
            appendLine(
                "# device=${fingerprint.manufacturer} ${fingerprint.model} sdk=${fingerprint.sdkInt} " +
                    "totalMemMb=${fingerprint.totalMemMb} memoryClassMb=${fingerprint.memoryClassMb} " +
                    "largeMemoryClassMb=${fingerprint.largeMemoryClassMb} maxHeapMb=${fingerprint.maxHeapMb} " +
                    "isLowRam=${fingerprint.isLowRamDevice} cores=${fingerprint.availableProcessors} " +
                    "mpc=${fingerprint.mediaPerformanceClass} abi=${fingerprint.primaryAbi} " +
                    "predictedTier=${fingerprint.predictedTier}",
            )
            appendLine(
                "# profile=${profile.name} metadataPlaintextBytes=$metadataPlaintextBytes " +
                    "poolSize=$poolSize totalResourcesPerRun=$totalResourcesPerRun sessionKeys=$sessionKeyMode " +
                    "runBudgetS=${runBudgetMs / MS_IN_S} sweep=descending",
            )
            appendLine("pageSize,peakJvmUsedMb,jvmHeapRatio,peakNativeAllocMb,maxHeapMb,durationMs,msPerResource,pages,note")
        }

    private fun formatRow(row: SweepRow): String {
        val sample = row.sample
        return listOf(
            row.pageSize.toString(),
            sample?.peakJvmUsedMb?.toString().orEmpty(),
            sample?.let { String.format(Locale.US, "%.3f", it.jvmHeapRatio) }.orEmpty(),
            sample?.peakNativeAllocMb?.toString().orEmpty(),
            sample?.maxHeapMb?.toString().orEmpty(),
            row.durationMs?.let { String.format(Locale.US, "%.1f", it) }.orEmpty(),
            row.durationMs?.let { String.format(Locale.US, "%.2f", it / totalResourcesPerRun) }.orEmpty(),
            row.pagesProcessed?.toString().orEmpty(),
            row.note,
        ).joinToString(separator = ",", postfix = "\n")
    }

    private companion object {
        private const val MS_IN_S = 1_000L
    }
}
