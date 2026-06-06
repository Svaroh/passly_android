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

package net.svaroh.passly.createresourcemenu.usecase

import net.svaroh.passly.common.usecase.AsyncUseCase
import net.svaroh.passly.core.resourcetypes.usecase.db.ResourceTypeIdToSlugMappingProvider
import net.svaroh.passly.featureflags.usecase.GetFeatureFlagsUseCase
import net.svaroh.passly.metadata.usecase.GetMetadataTypesSettingsUseCase
import net.svaroh.passly.supportedresourceTypes.ContentType
import net.svaroh.passly.supportedresourceTypes.ContentType.PasswordAndDescription
import net.svaroh.passly.supportedresourceTypes.ContentType.Totp
import net.svaroh.passly.supportedresourceTypes.ContentType.V5Default
import net.svaroh.passly.supportedresourceTypes.ContentType.V5Note
import net.svaroh.passly.supportedresourceTypes.ContentType.V5TotpStandalone
import net.svaroh.passly.ui.CreateResourceMenuModel
import net.svaroh.passly.ui.HomeDisplayViewModel
import net.svaroh.passly.ui.HomeDisplayViewModel.Folders
import net.svaroh.passly.ui.MetadataTypeModel
import net.svaroh.passly.ui.MetadataTypeModel.V4
import net.svaroh.passly.ui.MetadataTypeModel.V5

class CreateCreateResourceMenuModelUseCase(
    private val getFeatureFlagsUseCase: GetFeatureFlagsUseCase,
    private val getMetadataTypesSettingsUseCase: GetMetadataTypesSettingsUseCase,
    private val idToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider,
) : AsyncUseCase<CreateCreateResourceMenuModelUseCase.Input, CreateCreateResourceMenuModelUseCase.Output> {
    override suspend fun execute(input: Input): Output {
        val defaultMetadataType =
            getMetadataTypesSettingsUseCase
                .execute(Unit)
                .metadataTypesSettingsModel
                .defaultMetadataType
        val supportedContentTypes =
            idToSlugMappingProvider
                .provideMappingForSelectedAccount()
                .values
                .map { ContentType.fromSlug(it) }
        val isTotpFeatureFlagEnabled = getFeatureFlagsUseCase.execute(Unit).featureFlags.isTotpAvailable
        val isFoldersViewSelected = input.homeDisplay is Folders

        return Output(
            CreateResourceMenuModel(
                isPasswordEnabled = isLeadingPasswordResourceSupported(defaultMetadataType, supportedContentTypes),
                isTotpEnabled =
                    isLeadingTotpResourceSupported(
                        defaultMetadataType,
                        supportedContentTypes,
                    ) &&
                        isTotpFeatureFlagEnabled,
                isNoteEnabled =
                    isLeadingNoteResourceSupported(
                        defaultMetadataType,
                        supportedContentTypes,
                    ),
                isFolderEnabled = isFoldersViewSelected,
            ),
        )
    }

    private fun isLeadingNoteResourceSupported(
        defaultMetadataType: MetadataTypeModel,
        supportedSlugs: List<ContentType>,
    ): Boolean =
        when (defaultMetadataType) {
            V4 -> false
            V5 -> supportedSlugs.contains(V5Note)
        }

    private fun isLeadingPasswordResourceSupported(
        defaultMetadataType: MetadataTypeModel,
        supportedSlugs: List<ContentType>,
    ): Boolean =
        when (defaultMetadataType) {
            V4 -> supportedSlugs.contains(PasswordAndDescription)
            V5 -> supportedSlugs.contains(V5Default)
        }

    private fun isLeadingTotpResourceSupported(
        defaultMetadataType: MetadataTypeModel,
        supportedSlugs: List<ContentType>,
    ): Boolean =
        when (defaultMetadataType) {
            V4 -> supportedSlugs.contains(Totp)
            V5 -> supportedSlugs.contains(V5TotpStandalone)
        }

    data class Input(
        // homeDisplay is null when the user is on home (totp tab)
        val homeDisplay: HomeDisplayViewModel?,
    )

    data class Output(
        val model: CreateResourceMenuModel,
    )
}
