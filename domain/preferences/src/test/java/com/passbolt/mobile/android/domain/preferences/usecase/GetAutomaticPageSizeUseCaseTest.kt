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

package net.svaroh.passly.domain.preferences.usecase

import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.domain.preferences.pagesize.DevicePerformanceFingerprint
import net.svaroh.passly.domain.preferences.pagesize.DevicePerformanceFingerprintProvider
import net.svaroh.passly.domain.preferences.pagesize.DeviceTierClassifier
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GetAutomaticPageSizeUseCaseTest {
    private val devicePerformanceFingerprintProvider = mock<DevicePerformanceFingerprintProvider>()
    private val useCase = GetAutomaticPageSizeUseCase(devicePerformanceFingerprintProvider, DeviceTierClassifier())

    @Test
    fun `low tier device should map to low page sizes`() {
        whenever(devicePerformanceFingerprintProvider.provideFingerprint()) doReturn
            DevicePerformanceFingerprint(isLowRamDevice = false, totalMemMb = 2_815, memoryClassMb = 192)

        val output = useCase.execute(Unit)

        assertThat(output).isEqualTo(
            GetAutomaticPageSizeUseCase.Output(defaultPageSize = 1_000, recommendedLimit = 2_000),
        )
    }

    @Test
    fun `medium tier device should map to medium page sizes`() {
        whenever(devicePerformanceFingerprintProvider.provideFingerprint()) doReturn
            DevicePerformanceFingerprint(isLowRamDevice = false, totalMemMb = 7_571, memoryClassMb = 256)

        val output = useCase.execute(Unit)

        assertThat(output).isEqualTo(
            GetAutomaticPageSizeUseCase.Output(defaultPageSize = 2_000, recommendedLimit = 3_000),
        )
    }

    @Test
    fun `high tier device should map to high page sizes`() {
        whenever(devicePerformanceFingerprintProvider.provideFingerprint()) doReturn
            DevicePerformanceFingerprint(isLowRamDevice = false, totalMemMb = 15_575, memoryClassMb = 256)

        val output = useCase.execute(Unit)

        assertThat(output).isEqualTo(
            GetAutomaticPageSizeUseCase.Output(defaultPageSize = 3_000, recommendedLimit = 5_000),
        )
    }
}
