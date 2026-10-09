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

import net.svaroh.passly.common.usecase.UseCase
import net.svaroh.passly.domain.preferences.pagesize.DevicePerformanceFingerprintProvider
import net.svaroh.passly.domain.preferences.pagesize.DeviceTier.HIGH
import net.svaroh.passly.domain.preferences.pagesize.DeviceTier.LOW
import net.svaroh.passly.domain.preferences.pagesize.DeviceTier.MEDIUM
import net.svaroh.passly.domain.preferences.pagesize.DeviceTierClassifier
import timber.log.Timber

class GetAutomaticPageSizeUseCase(
    private val devicePerformanceFingerprintProvider: DevicePerformanceFingerprintProvider,
    private val deviceTierClassifier: DeviceTierClassifier,
) : UseCase<Unit, GetAutomaticPageSizeUseCase.Output> {
    override fun execute(input: Unit): Output {
        val fingerprint = devicePerformanceFingerprintProvider.provideFingerprint()
        val deviceTier = deviceTierClassifier.classify(fingerprint)
        Timber.d("Device performance fingerprint: %s; classified device tier: %s", fingerprint, deviceTier)
        return when (deviceTier) {
            LOW -> Output(LOW_PAGE_SIZE, LOW_RECOMMENDED_LIMIT)
            MEDIUM -> Output(MEDIUM_PAGE_SIZE, MEDIUM_RECOMMENDED_LIMIT)
            HIGH -> Output(HIGH_PAGE_SIZE, HIGH_RECOMMENDED_LIMIT)
        }
    }

    data class Output(
        val defaultPageSize: Int,
        val recommendedLimit: Int,
    )

    private companion object {
        private const val LOW_PAGE_SIZE = 1_000
        private const val LOW_RECOMMENDED_LIMIT = 2_000
        private const val MEDIUM_PAGE_SIZE = 2_000
        private const val MEDIUM_RECOMMENDED_LIMIT = 3_000
        private const val HIGH_PAGE_SIZE = 3_000
        private const val HIGH_RECOMMENDED_LIMIT = 5_000
    }
}
