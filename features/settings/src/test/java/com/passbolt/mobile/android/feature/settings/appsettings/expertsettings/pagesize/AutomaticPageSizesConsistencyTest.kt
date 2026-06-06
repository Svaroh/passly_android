package net.svaroh.passly.feature.settings.appsettings.expertsettings.pagesize

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
import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.domain.preferences.PreferencesDefaults
import net.svaroh.passly.domain.preferences.pagesize.DevicePerformanceFingerprint
import net.svaroh.passly.domain.preferences.pagesize.DevicePerformanceFingerprintProvider
import net.svaroh.passly.domain.preferences.pagesize.DeviceTierClassifier
import net.svaroh.passly.domain.preferences.usecase.GetAutomaticPageSizeUseCase
import net.svaroh.passly.feature.settings.screen.appsettings.expertsettings.pagesize.ALLOWED_PAGE_SIZES
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AutomaticPageSizesConsistencyTest {
    private val devicePerformanceFingerprintProvider = mock<DevicePerformanceFingerprintProvider>()
    private val getAutomaticPageSizeUseCase =
        GetAutomaticPageSizeUseCase(devicePerformanceFingerprintProvider, DeviceTierClassifier())

    private val tierFingerprints =
        listOf(
            DevicePerformanceFingerprint(isLowRamDevice = true, totalMemMb = 2_000, memoryClassMb = 128),
            DevicePerformanceFingerprint(isLowRamDevice = false, totalMemMb = 8_000, memoryClassMb = 256),
            DevicePerformanceFingerprint(isLowRamDevice = false, totalMemMb = 16_000, memoryClassMb = 512),
        )

    @Test
    fun `automatic page sizes of all tiers should be allowed slider values`() {
        tierFingerprints.forEach { fingerprint ->
            whenever(devicePerformanceFingerprintProvider.provideFingerprint()) doReturn fingerprint

            val output = getAutomaticPageSizeUseCase.execute(Unit)

            assertThat(ALLOWED_PAGE_SIZES).contains(output.defaultPageSize)
            assertThat(ALLOWED_PAGE_SIZES).contains(output.recommendedLimit)
            assertThat(output.recommendedLimit).isAtLeast(output.defaultPageSize)
        }
    }

    @Test
    fun `fallback default page size should be an allowed slider value`() {
        assertThat(ALLOWED_PAGE_SIZES).contains(PreferencesDefaults.API_FETCH_PAGE_SIZE)
    }
}
