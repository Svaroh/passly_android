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

package net.svaroh.passly.core.security.flagsecure

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.core.security.flagsecure.WindowFlagAction.APPLY_FLAG
import net.svaroh.passly.core.security.flagsecure.WindowFlagAction.CLEAR_FLAG
import net.svaroh.passly.core.security.flagsecure.WindowFlagAction.NO_ACTION_NEEDED
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class FlagSecureRegistryTest {
    private val activity = mock<Activity>()
    private val otherActivity = mock<Activity>()

    private lateinit var registry: FlagSecureRegistry

    @Before
    fun setUp() {
        registry = FlagSecureRegistry()
    }

    @Test
    fun `first add applies and last remove clears`() {
        assertThat(registry.addActivity(activity)).isEqualTo(APPLY_FLAG)
        assertThat(registry.removeActivity(activity)).isEqualTo(CLEAR_FLAG)
    }

    @Test
    fun `overlapping adds clear only on the last remove`() {
        assertThat(registry.addActivity(activity)).isEqualTo(APPLY_FLAG)
        assertThat(registry.addActivity(activity)).isEqualTo(NO_ACTION_NEEDED)
        assertThat(registry.removeActivity(activity)).isEqualTo(NO_ACTION_NEEDED)
        assertThat(registry.removeActivity(activity)).isEqualTo(CLEAR_FLAG)
    }

    @Test
    fun `secure to secure to back to non secure applies once and clears once`() {
        val results =
            listOf(
                registry.addActivity(activity),
                registry.addActivity(activity),
                registry.removeActivity(activity),
                registry.addActivity(activity),
                registry.removeActivity(activity),
                registry.removeActivity(activity),
            )

        assertThat(results)
            .containsExactly(
                APPLY_FLAG,
                NO_ACTION_NEEDED,
                NO_ACTION_NEEDED,
                NO_ACTION_NEEDED,
                NO_ACTION_NEEDED,
                CLEAR_FLAG,
            ).inOrder()
    }

    @Test
    fun `re-add after full remove applies again`() {
        registry.addActivity(activity)
        registry.removeActivity(activity)

        assertThat(registry.addActivity(activity)).isEqualTo(APPLY_FLAG)
    }

    @Test
    fun `remove without add is ignored`() {
        assertThat(registry.removeActivity(activity)).isEqualTo(NO_ACTION_NEEDED)
    }

    @Test
    fun `extra remove does not go negative`() {
        registry.addActivity(activity)
        registry.removeActivity(activity)

        assertThat(registry.removeActivity(activity)).isEqualTo(NO_ACTION_NEEDED)
        assertThat(registry.addActivity(activity)).isEqualTo(APPLY_FLAG)
    }

    @Test
    fun `activities are counted independently`() {
        assertThat(registry.addActivity(activity)).isEqualTo(APPLY_FLAG)
        assertThat(registry.addActivity(otherActivity)).isEqualTo(APPLY_FLAG)
        assertThat(registry.removeActivity(activity)).isEqualTo(CLEAR_FLAG)
        assertThat(registry.removeActivity(otherActivity)).isEqualTo(CLEAR_FLAG)
    }
}
