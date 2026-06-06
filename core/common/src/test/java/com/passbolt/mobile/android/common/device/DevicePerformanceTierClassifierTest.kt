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

package net.svaroh.passly.common.device

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import net.svaroh.passly.common.device.DevicePerformanceTier.HIGH
import net.svaroh.passly.common.device.DevicePerformanceTier.LOW
import net.svaroh.passly.common.device.DevicePerformanceTier.MEDIUM
import org.junit.Test

class DevicePerformanceTierClassifierTest {
    @Test
    fun `isLowRamDevice forces LOW even with abundant RAM and heap`() {
        assertThat(classifyDevicePerformanceTier(fingerprint(isLowRamDevice = true, totalMemoryMb = 16_000, memoryClassMb = 512)))
            .isEqualTo(LOW)
    }

    @Test
    fun `total RAM below the LOW threshold is LOW`() {
        assertThat(classifyDevicePerformanceTier(fingerprint(totalMemoryMb = 5_600, memoryClassMb = 256)))
            .isEqualTo(LOW)
    }

    @Test
    fun `tiny heap ceiling forces LOW even with abundant RAM (guardrail)`() {
        assertThat(classifyDevicePerformanceTier(fingerprint(totalMemoryMb = 12_000, memoryClassMb = 96)))
            .isEqualTo(LOW)
    }

    @Test
    fun `mid RAM with an ordinary heap is MEDIUM`() {
        assertThat(classifyDevicePerformanceTier(fingerprint(totalMemoryMb = 7_400, memoryClassMb = 256)))
            .isEqualTo(MEDIUM)
    }

    @Test
    fun `high RAM with a small heap ceiling is pulled down to MEDIUM (guardrail)`() {
        assertThat(classifyDevicePerformanceTier(fingerprint(totalMemoryMb = 11_500, memoryClassMb = 192)))
            .isEqualTo(MEDIUM)
    }

    @Test
    fun `high RAM and a large heap ceiling is HIGH`() {
        assertThat(classifyDevicePerformanceTier(fingerprint(totalMemoryMb = 11_500, memoryClassMb = 512)))
            .isEqualTo(HIGH)
    }

    @Test
    fun `HIGH requires both signals - enough RAM but a sub-threshold heap is not HIGH`() {
        assertThat(classifyDevicePerformanceTier(fingerprint(totalMemoryMb = 12_000, memoryClassMb = 255)))
            .isEqualTo(MEDIUM)
    }

    @Test
    fun `total RAM one MB below LOW_MAX is LOW, at LOW_MAX is MEDIUM`() {
        assertThat(classifyDevicePerformanceTier(fingerprint(totalMemoryMb = LOW_MAX_TOTAL_MEMORY_MB - 1, memoryClassMb = 256)))
            .isEqualTo(LOW)
        assertThat(classifyDevicePerformanceTier(fingerprint(totalMemoryMb = LOW_MAX_TOTAL_MEMORY_MB, memoryClassMb = 256)))
            .isEqualTo(MEDIUM)
    }

    @Test
    fun `heap exactly at LOW_MAX is LOW, one MB above is not forced LOW`() {
        assertThat(classifyDevicePerformanceTier(fingerprint(totalMemoryMb = 12_000, memoryClassMb = LOW_MAX_MEMORY_CLASS_MB)))
            .isEqualTo(LOW)
        assertThat(classifyDevicePerformanceTier(fingerprint(totalMemoryMb = 12_000, memoryClassMb = LOW_MAX_MEMORY_CLASS_MB + 1)))
            .isEqualTo(MEDIUM)
    }

    @Test
    fun `total RAM one MB below HIGH_MIN is MEDIUM, at HIGH_MIN with enough heap is HIGH`() {
        assertThat(classifyDevicePerformanceTier(fingerprint(totalMemoryMb = HIGH_MIN_TOTAL_MEMORY_MB - 1, memoryClassMb = 256)))
            .isEqualTo(MEDIUM)
        assertThat(classifyDevicePerformanceTier(fingerprint(totalMemoryMb = HIGH_MIN_TOTAL_MEMORY_MB, memoryClassMb = 256)))
            .isEqualTo(HIGH)
    }

    @Test
    fun `heap one MB below HIGH_MIN is MEDIUM, exactly at HIGH_MIN with enough RAM is HIGH`() {
        assertThat(classifyDevicePerformanceTier(fingerprint(totalMemoryMb = 12_000, memoryClassMb = HIGH_MIN_MEMORY_CLASS_MB - 1)))
            .isEqualTo(MEDIUM)
        assertThat(classifyDevicePerformanceTier(fingerprint(totalMemoryMb = 12_000, memoryClassMb = HIGH_MIN_MEMORY_CLASS_MB)))
            .isEqualTo(HIGH)
    }

    @Test
    fun `representative 2026 devices land on the expected tier`() {
        listOf(
            Case("Pixel 9 Pro (16 GB)", fingerprint(totalMemoryMb = 15_000, memoryClassMb = 256), HIGH),
            Case("Galaxy S24 Ultra (12 GB)", fingerprint(totalMemoryMb = 11_200, memoryClassMb = 256), HIGH),
            Case("OnePlus 12 (12 GB)", fingerprint(totalMemoryMb = 11_300, memoryClassMb = 512), HIGH),
            Case("Galaxy S24 base (8 GB)", fingerprint(totalMemoryMb = 7_400, memoryClassMb = 256), MEDIUM),
            Case("Pixel 7a (8 GB)", fingerprint(totalMemoryMb = 7_300, memoryClassMb = 256), MEDIUM),
            Case("Galaxy A54 (8 GB)", fingerprint(totalMemoryMb = 7_400, memoryClassMb = 256), MEDIUM),
            Case("Pixel 6a (6 GB)", fingerprint(totalMemoryMb = 5_600, memoryClassMb = 192), LOW),
            Case("Galaxy A35 5G (6 GB)", fingerprint(totalMemoryMb = 5_600, memoryClassMb = 192), LOW),
            Case("Moto G entry (4 GB)", fingerprint(totalMemoryMb = 3_600, memoryClassMb = 128), LOW),
            Case(
                "Android Go entry (3 GB)",
                fingerprint(isLowRamDevice = true, totalMemoryMb = 2_700, memoryClassMb = 96),
                LOW,
            ),
        ).forEach { (name, fingerprint, expected) ->
            assertWithMessage(name).that(classifyDevicePerformanceTier(fingerprint)).isEqualTo(expected)
        }
    }

    private fun fingerprint(
        isLowRamDevice: Boolean = false,
        totalMemoryMb: Int,
        memoryClassMb: Int,
    ) = DevicePerformanceFingerprint(
        isLowRamDevice = isLowRamDevice,
        totalMemoryMb = totalMemoryMb,
        memoryClassMb = memoryClassMb,
    )

    private data class Case(
        val name: String,
        val fingerprint: DevicePerformanceFingerprint,
        val expected: DevicePerformanceTier,
    )
}
