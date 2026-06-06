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

package net.svaroh.passly.benchmark.pagesize.appstate

import net.svaroh.passly.domain.resourcetypes.ResourceTypesLocalDataSource
import net.svaroh.passly.domain.resourcetypes.model.ResourceType
import net.svaroh.passly.domain.resourcetypes.usecase.ResourceTypeIdToSlugMappingProvider
import kotlinx.coroutines.runBlocking
import java.util.UUID

class ResourceTypeSeeder(
    private val resourceTypesLocalDataSource: ResourceTypesLocalDataSource,
    private val mappingProvider: ResourceTypeIdToSlugMappingProvider,
) {
    fun seedV5Default(
        userId: String,
        resourceTypeId: UUID,
    ) = runBlocking {
        resourceTypesLocalDataSource.setResourceTypes(
            userId,
            listOf(
                ResourceType(
                    id = resourceTypeId,
                    slug = V5_DEFAULT_SLUG,
                    name = V5_DEFAULT_SLUG,
                    deleted = null,
                ),
            ),
        )
        mappingProvider.invalidateSelectedUserMapping()
    }

    private companion object {
        private const val V5_DEFAULT_SLUG = "v5-default"
    }
}
