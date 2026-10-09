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

import net.svaroh.passly.benchmark.pagesize.appstate.LocalResourceStore
import net.svaroh.passly.domain.resources.usecase.ResourceInteractor

class PipelineWarmUp(
    private val measuredResourceRefresh: MeasuredResourceRefresh,
    private val localResourceStore: LocalResourceStore,
) {
    fun verifyDecryptsAndPersists(
        size: Int,
        diagnostics: () -> String,
    ) {
        val measurement = measuredResourceRefresh.measure(pageSize = size, totalResources = size)
        check(measurement.output is ResourceInteractor.Output.Success) {
            "Warm-up fetch did not succeed (${measurement.output}). ${diagnostics()} " +
                "Check account / passphrase / resource-type / schema seeding."
        }
        val persisted = localResourceStore.count()
        check(persisted == size) {
            "Warm-up persisted $persisted of $size resources - the fixture is being dropped during decrypt/validation. " +
                "Fix the fixture before trusting the sweep."
        }
    }
}
