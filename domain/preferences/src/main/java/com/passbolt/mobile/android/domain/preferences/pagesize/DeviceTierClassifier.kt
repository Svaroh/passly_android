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

import net.svaroh.passly.domain.preferences.pagesize.DeviceTier.HIGH
import net.svaroh.passly.domain.preferences.pagesize.DeviceTier.LOW
import net.svaroh.passly.domain.preferences.pagesize.DeviceTier.MEDIUM

class DeviceTierClassifier {
    fun classify(fingerprint: DevicePerformanceFingerprint): DeviceTier =
        when {
            fingerprint.isLowRamDevice ||
                fingerprint.totalMemMb < LOW_MAX_TOTAL_MEM_MB ||
                fingerprint.memoryClassMb <= LOW_MAX_MEMORY_CLASS_MB -> LOW
            fingerprint.totalMemMb < HIGH_MIN_TOTAL_MEM_MB ||
                fingerprint.memoryClassMb < HIGH_MIN_MEMORY_CLASS_MB -> MEDIUM
            else -> HIGH
        }

    private companion object {
        private const val LOW_MAX_TOTAL_MEM_MB = 7_000L
        private const val LOW_MAX_MEMORY_CLASS_MB = 128
        private const val HIGH_MIN_TOTAL_MEM_MB = 10_000L
        private const val HIGH_MIN_MEMORY_CLASS_MB = 256
    }
}
