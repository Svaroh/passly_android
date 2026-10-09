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

package net.svaroh.passly.domain.preferences.pagesize

import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.domain.preferences.pagesize.DeviceTier.HIGH
import net.svaroh.passly.domain.preferences.pagesize.DeviceTier.LOW
import net.svaroh.passly.domain.preferences.pagesize.DeviceTier.MEDIUM
import org.junit.Test

class DeviceTierClassifierTest {
    private val classifier = DeviceTierClassifier()

    @Test
    fun `low ram device flag should force low tier regardless of memory`() {
        val tier = classifier.classify(fingerprint(totalMemMb = 15_575, memoryClassMb = 512, isLowRamDevice = true))

        assertThat(tier).isEqualTo(LOW)
    }

    @Test
    fun `total memory below low threshold should classify as low`() {
        assertThat(classifier.classify(fingerprint(totalMemMb = 2_815, memoryClassMb = 192))).isEqualTo(LOW)
        assertThat(classifier.classify(fingerprint(totalMemMb = 6_999, memoryClassMb = 512))).isEqualTo(LOW)
    }

    @Test
    fun `memory class at or below low threshold should classify as low`() {
        assertThat(classifier.classify(fingerprint(totalMemMb = 12_000, memoryClassMb = 128))).isEqualTo(LOW)
    }

    @Test
    fun `total memory below high threshold should classify as medium`() {
        assertThat(classifier.classify(fingerprint(totalMemMb = 7_571, memoryClassMb = 256))).isEqualTo(MEDIUM)
        assertThat(classifier.classify(fingerprint(totalMemMb = 7_000, memoryClassMb = 256))).isEqualTo(MEDIUM)
        assertThat(classifier.classify(fingerprint(totalMemMb = 9_999, memoryClassMb = 256))).isEqualTo(MEDIUM)
    }

    @Test
    fun `memory class below high threshold should classify as medium`() {
        assertThat(classifier.classify(fingerprint(totalMemMb = 12_000, memoryClassMb = 129))).isEqualTo(MEDIUM)
        assertThat(classifier.classify(fingerprint(totalMemMb = 12_000, memoryClassMb = 255))).isEqualTo(MEDIUM)
    }

    @Test
    fun `high total memory and high memory class should classify as high`() {
        assertThat(classifier.classify(fingerprint(totalMemMb = 15_575, memoryClassMb = 256))).isEqualTo(HIGH)
        assertThat(classifier.classify(fingerprint(totalMemMb = 11_436, memoryClassMb = 256))).isEqualTo(HIGH)
        assertThat(classifier.classify(fingerprint(totalMemMb = 10_000, memoryClassMb = 256))).isEqualTo(HIGH)
    }

    private fun fingerprint(
        totalMemMb: Long,
        memoryClassMb: Int,
        isLowRamDevice: Boolean = false,
    ) = DevicePerformanceFingerprint(
        isLowRamDevice = isLowRamDevice,
        totalMemMb = totalMemMb,
        memoryClassMb = memoryClassMb,
    )
}
