package net.svaroh.passly.metadata.usecase

import net.svaroh.passly.common.usecase.AsyncUseCase
import net.svaroh.passly.encryptedstorage.EncryptedSharedPreferencesFactory
import net.svaroh.passly.metadata.usecase.MetadataTypesStorageConstants.ALLOW_CREATION_OF_V4_FOLDERS
import net.svaroh.passly.metadata.usecase.MetadataTypesStorageConstants.ALLOW_CREATION_OF_V4_RESOURCES
import net.svaroh.passly.metadata.usecase.MetadataTypesStorageConstants.ALLOW_CREATION_OF_V4_TAGS
import net.svaroh.passly.metadata.usecase.MetadataTypesStorageConstants.ALLOW_CREATION_OF_V5_FOLDERS
import net.svaroh.passly.metadata.usecase.MetadataTypesStorageConstants.ALLOW_CREATION_OF_V5_RESOURCES
import net.svaroh.passly.metadata.usecase.MetadataTypesStorageConstants.ALLOW_CREATION_OF_V5_TAGS
import net.svaroh.passly.metadata.usecase.MetadataTypesStorageConstants.ALLOW_V4_V5_UPGRADE
import net.svaroh.passly.metadata.usecase.MetadataTypesStorageConstants.ALLOW_V5_V4_DOWNGRADE
import net.svaroh.passly.metadata.usecase.MetadataTypesStorageConstants.DEFAULT_FOLDER_TYPE
import net.svaroh.passly.metadata.usecase.MetadataTypesStorageConstants.DEFAULT_METADATA_TYPE
import net.svaroh.passly.metadata.usecase.MetadataTypesStorageConstants.DEFAULT_TAG_TYPE
import net.svaroh.passly.ui.MetadataTypeModel
import net.svaroh.passly.ui.MetadataTypesSettingsModel

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
class GetMetadataTypesSettingsUseCase(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory,
) : AsyncUseCase<Unit, GetMetadataTypesSettingsUseCase.Output>,
    net.svaroh.passly.core.accounts.usecase.SelectedAccountUseCase {
    override suspend fun execute(input: Unit): Output {
        val fileName = MetadataSettingsFileName(selectedAccountId).name
        encryptedSharedPreferencesFactory.get("$fileName.xml").let {
            return Output(
                MetadataTypesSettingsModel(
                    defaultMetadataType =
                        MetadataTypeModel.valueOf(
                            it.getString(DEFAULT_METADATA_TYPE, MetadataTypeModel.V4.name)!!,
                        ),
                    defaultFolderType =
                        MetadataTypeModel.valueOf(
                            it.getString(DEFAULT_FOLDER_TYPE, MetadataTypeModel.V4.name)!!,
                        ),
                    defaultTagType =
                        MetadataTypeModel.valueOf(
                            it.getString(DEFAULT_TAG_TYPE, MetadataTypeModel.V4.name)!!,
                        ),
                    allowCreationOfV5Resources = it.getBoolean(ALLOW_CREATION_OF_V5_RESOURCES, false),
                    allowCreationOfV5Folders = it.getBoolean(ALLOW_CREATION_OF_V5_FOLDERS, false),
                    allowCreationOfV5Tags = it.getBoolean(ALLOW_CREATION_OF_V5_TAGS, false),
                    allowCreationOfV4Resources = it.getBoolean(ALLOW_CREATION_OF_V4_RESOURCES, false),
                    allowCreationOfV4Folders = it.getBoolean(ALLOW_CREATION_OF_V4_FOLDERS, true),
                    allowCreationOfV4Tags = it.getBoolean(ALLOW_CREATION_OF_V4_TAGS, true),
                    allowV4V5Upgrade = it.getBoolean(ALLOW_V4_V5_UPGRADE, false),
                    allowV5V4Downgrade = it.getBoolean(ALLOW_V5_V4_DOWNGRADE, false),
                ),
            )
        }
    }

    data class Output(
        val metadataTypesSettingsModel: MetadataTypesSettingsModel,
    )
}
