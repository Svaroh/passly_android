/**
 * Passly - Open source password manager for teams
 * Copyright (c) 2026 Svaroh
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Svaroh
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://passly.svaroh.net Passly
 * @since v1.0
 */
package net.svaroh.passly.core.sync

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Duration

class SyncBackoffTest {
    @Test
    fun `backoff follows the documented ladder`() {
        val ladder = (1..6).map { SyncBackoff.delayAfter(it) }

        assertThat(ladder)
            .containsExactly(
                Duration.ofSeconds(30),
                Duration.ofMinutes(1),
                Duration.ofMinutes(5),
                Duration.ofMinutes(15),
                Duration.ofHours(1),
                Duration.ofHours(6),
            ).inOrder()
    }

    @Test
    fun `backoff saturates instead of giving up`() {
        assertThat(SyncBackoff.delayAfter(7)).isEqualTo(Duration.ofHours(6))
        assertThat(SyncBackoff.delayAfter(1_000)).isEqualTo(Duration.ofHours(6))
    }

    @Test
    fun `a user initiated run is never delayed`() {
        assertThat(SyncBackoff.delayAfter(5, SyncTrigger.MANUAL)).isEqualTo(Duration.ZERO)
        assertThat(SyncBackoff.delayAfter(5, SyncTrigger.BACKGROUND)).isEqualTo(Duration.ofHours(1))
    }
}
