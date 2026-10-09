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

import android.os.Debug

class HeapSampler(
    private val samplePeriodMs: Long = DEFAULT_SAMPLE_PERIOD_MS,
) {
    private val runtime = Runtime.getRuntime()

    @Volatile
    private var sampling = false
    private var pollerThread: Thread? = null

    @Volatile
    private var peakJvmUsedBytes = 0L

    @Volatile
    private var peakNativeAllocBytes = 0L

    @Volatile
    var lastSample: Sample? = null
        private set

    fun <T> measure(block: () -> T): Pair<T, Sample> {
        start()
        try {
            val result = block()
            return result to stop()
        } finally {
            if (sampling) stop()
        }
    }

    private fun start() {
        System.gc()
        peakJvmUsedBytes = 0L
        peakNativeAllocBytes = 0L
        lastSample = null
        sampling = true
        pollerThread =
            Thread {
                while (sampling) {
                    sample()
                    try {
                        Thread.sleep(samplePeriodMs)
                    } catch (interrupted: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                }
            }.apply {
                isDaemon = true
                name = "heap-sampler"
                start()
            }
    }

    private fun stop(): Sample {
        sampling = false
        pollerThread?.interrupt()
        pollerThread?.join(POLLER_JOIN_TIMEOUT_MS)
        pollerThread = null
        sample()
        return Sample(
            peakJvmUsedBytes = peakJvmUsedBytes,
            peakNativeAllocBytes = peakNativeAllocBytes,
            maxHeapBytes = runtime.maxMemory(),
        ).also { lastSample = it }
    }

    private fun sample() {
        val jvmUsed = runtime.totalMemory() - runtime.freeMemory()
        if (jvmUsed > peakJvmUsedBytes) peakJvmUsedBytes = jvmUsed
        val nativeAlloc = Debug.getNativeHeapAllocatedSize()
        if (nativeAlloc > peakNativeAllocBytes) peakNativeAllocBytes = nativeAlloc
    }

    data class Sample(
        val peakJvmUsedBytes: Long,
        val peakNativeAllocBytes: Long,
        val maxHeapBytes: Long,
    ) {
        val jvmHeapRatio: Double
            get() = peakJvmUsedBytes.toDouble() / maxHeapBytes.toDouble()

        val peakJvmUsedMb: Long get() = peakJvmUsedBytes / BYTES_IN_MB
        val peakNativeAllocMb: Long get() = peakNativeAllocBytes / BYTES_IN_MB
        val maxHeapMb: Long get() = maxHeapBytes / BYTES_IN_MB
    }

    private companion object {
        private const val DEFAULT_SAMPLE_PERIOD_MS = 5L
        private const val POLLER_JOIN_TIMEOUT_MS = 1_000L
        private const val BYTES_IN_MB = 1024L * 1024L
    }
}
