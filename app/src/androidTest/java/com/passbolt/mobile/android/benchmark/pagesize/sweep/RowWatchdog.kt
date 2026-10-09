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

class RowWatchdog(
    private val stallTimeoutMs: Long = DEFAULT_STALL_TIMEOUT_MS,
    private val tickMs: Long = DEFAULT_TICK_MS,
) {
    @Volatile
    private var running = false

    @Volatile
    private var lastProgressNanos = 0L

    @Volatile
    var pagesProcessed = 0
        private set

    private var thread: Thread? = null

    fun start(
        rowCeilingMs: Long,
        onStalled: () -> Unit,
        onCeilingExceeded: () -> Unit,
    ) {
        val startNanos = System.nanoTime()
        lastProgressNanos = startNanos
        pagesProcessed = 0
        running = true
        thread =
            Thread { watch(startNanos, rowCeilingMs, onStalled, onCeilingExceeded) }
                .apply {
                    isDaemon = true
                    name = THREAD_NAME
                    start()
                }
    }

    fun recordProgress(pages: Int) {
        pagesProcessed = pages
        lastProgressNanos = System.nanoTime()
    }

    fun stop() {
        running = false
        thread?.interrupt()
        thread = null
    }

    // own thread, not a coroutine: a page that pins the runBlocking event loop must not also silence the watchdog
    private fun watch(
        startNanos: Long,
        rowCeilingMs: Long,
        onStalled: () -> Unit,
        onCeilingExceeded: () -> Unit,
    ) {
        while (running) {
            try {
                Thread.sleep(tickMs)
            } catch (interrupted: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            }
            if (!running) return
            val now = System.nanoTime()
            when {
                // before the first page lands there is nothing to compare against - a slow first page is not a stall
                pagesProcessed > 0 && now - lastProgressNanos > stallTimeoutMs * NANOS_IN_MS -> {
                    running = false
                    onStalled()
                }
                now - startNanos > rowCeilingMs * NANOS_IN_MS -> {
                    running = false
                    onCeilingExceeded()
                }
            }
        }
    }

    private companion object {
        private const val DEFAULT_STALL_TIMEOUT_MS = 300_000L
        private const val DEFAULT_TICK_MS = 1_000L
        private const val NANOS_IN_MS = 1_000_000L
        private const val THREAD_NAME = "row-watchdog"
    }
}
