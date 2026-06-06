/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2021 Passbolt SA
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

package net.svaroh.passly.resourcemoremenu.usecase

import net.svaroh.passly.common.usecase.AsyncUseCase
import net.svaroh.passly.core.rbac.usecase.GetRbacRulesUseCase
import net.svaroh.passly.core.resources.usecase.db.GetLocalResourceUseCase
import net.svaroh.passly.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import net.svaroh.passly.supportedresourceTypes.ContentType
import net.svaroh.passly.ui.RbacRuleModel.ALLOW
import net.svaroh.passly.ui.ResourceMoreMenuModel
import net.svaroh.passly.ui.ResourceMoreMenuModel.DescriptionOption.HAS_METADATA_DESCRIPTION
import net.svaroh.passly.ui.ResourceMoreMenuModel.DescriptionOption.HAS_NOTE
import net.svaroh.passly.ui.ResourceMoreMenuModel.FavouriteOption.ADD_TO_FAVOURITES
import net.svaroh.passly.ui.ResourceMoreMenuModel.FavouriteOption.REMOVE_FROM_FAVOURITES
import net.svaroh.passly.ui.ResourcePermission
import net.svaroh.passly.ui.isFavourite
import java.util.UUID

class CreateResourceMoreMenuModelUseCase(
    private val getLocalResourceUseCase: GetLocalResourceUseCase,
    private val getRbacRulesUseCase: GetRbacRulesUseCase,
    private val idToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider,
) : AsyncUseCase<CreateResourceMoreMenuModelUseCase.Input, CreateResourceMoreMenuModelUseCase.Output> {
    override suspend fun execute(input: Input): Output {
        val resource = getLocalResourceUseCase.execute(GetLocalResourceUseCase.Input(input.resourceId)).resource
        val rbacModel = getRbacRulesUseCase.execute(Unit).rbacModel
        val isCopyRbacAllowed = rbacModel.passwordCopyRule == ALLOW
        val isShareRbacAllowed = rbacModel.shareViewRule == ALLOW
        val slug = idToSlugMappingProvider.provideMappingForSelectedAccount()[UUID.fromString(resource.resourceTypeId)]
        val contentType = ContentType.fromSlug(slug!!)

        return Output(
            ResourceMoreMenuModel(
                title = resource.metadataJsonModel.name,
                canCopy = isCopyRbacAllowed,
                canDelete = resource.permission in WRITE_PERMISSIONS,
                canEdit = resource.permission in WRITE_PERMISSIONS,
                canShare = isShareRbacAllowed && resource.permission == ResourcePermission.OWNER,
                favouriteOption =
                    if (resource.isFavourite()) {
                        REMOVE_FROM_FAVOURITES
                    } else {
                        ADD_TO_FAVOURITES
                    },
                descriptionOptions =
                    buildList {
                        if (contentType.hasMetadataDescription()) {
                            add(HAS_METADATA_DESCRIPTION)
                        }
                        if (contentType.hasNote()) {
                            add(HAS_NOTE)
                        }
                    },
            ),
        )
    }

    data class Input(
        val resourceId: String,
    )

    data class Output(
        val resourceMenuModel: ResourceMoreMenuModel,
    )

    private companion object {
        private val WRITE_PERMISSIONS =
            setOf(
                ResourcePermission.OWNER,
                ResourcePermission.UPDATE,
            )
    }
}
