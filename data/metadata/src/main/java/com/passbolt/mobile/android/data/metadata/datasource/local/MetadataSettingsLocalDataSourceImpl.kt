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

package net.svaroh.passly.data.metadata.datasource.local

import android.content.SharedPreferences
import androidx.core.content.edit
import net.svaroh.passly.data.metadata.datasource.local.MetadataTypesStorageConstants.ALLOW_CREATION_OF_V4_FOLDERS
import net.svaroh.passly.data.metadata.datasource.local.MetadataTypesStorageConstants.ALLOW_CREATION_OF_V4_RESOURCES
import net.svaroh.passly.data.metadata.datasource.local.MetadataTypesStorageConstants.ALLOW_CREATION_OF_V4_TAGS
import net.svaroh.passly.data.metadata.datasource.local.MetadataTypesStorageConstants.ALLOW_CREATION_OF_V5_FOLDERS
import net.svaroh.passly.data.metadata.datasource.local.MetadataTypesStorageConstants.ALLOW_CREATION_OF_V5_RESOURCES
import net.svaroh.passly.data.metadata.datasource.local.MetadataTypesStorageConstants.ALLOW_CREATION_OF_V5_TAGS
import net.svaroh.passly.data.metadata.datasource.local.MetadataTypesStorageConstants.ALLOW_USAGE_OF_PERSONAL_KEYS
import net.svaroh.passly.data.metadata.datasource.local.MetadataTypesStorageConstants.ALLOW_V4_V5_UPGRADE
import net.svaroh.passly.data.metadata.datasource.local.MetadataTypesStorageConstants.ALLOW_V5_V4_DOWNGRADE
import net.svaroh.passly.data.metadata.datasource.local.MetadataTypesStorageConstants.DEFAULT_FOLDER_TYPE
import net.svaroh.passly.data.metadata.datasource.local.MetadataTypesStorageConstants.DEFAULT_METADATA_TYPE
import net.svaroh.passly.data.metadata.datasource.local.MetadataTypesStorageConstants.DEFAULT_TAG_TYPE
import net.svaroh.passly.data.metadata.datasource.local.MetadataTypesStorageConstants.TRUSTED_MD_KEY_CREATED
import net.svaroh.passly.data.metadata.datasource.local.MetadataTypesStorageConstants.TRUSTED_MD_KEY_CREATED_BY
import net.svaroh.passly.data.metadata.datasource.local.MetadataTypesStorageConstants.TRUSTED_MD_KEY_ID
import net.svaroh.passly.data.metadata.datasource.local.MetadataTypesStorageConstants.TRUSTED_MD_KEY_KEY_DATA
import net.svaroh.passly.data.metadata.datasource.local.MetadataTypesStorageConstants.TRUSTED_MD_KEY_KEY_PGP_MESSAGE
import net.svaroh.passly.data.metadata.datasource.local.MetadataTypesStorageConstants.TRUSTED_MD_KEY_MODIFIED
import net.svaroh.passly.data.metadata.datasource.local.MetadataTypesStorageConstants.TRUSTED_MD_KEY_MODIFIED_BY
import net.svaroh.passly.data.metadata.datasource.local.MetadataTypesStorageConstants.TRUSTED_MD_KEY_PASSPHRASE
import net.svaroh.passly.data.metadata.datasource.local.MetadataTypesStorageConstants.TRUSTED_MD_KEY_SIGNED_NAME
import net.svaroh.passly.data.metadata.datasource.local.MetadataTypesStorageConstants.TRUSTED_MD_KEY_SIGNED_USERNAME
import net.svaroh.passly.data.metadata.datasource.local.MetadataTypesStorageConstants.TRUSTED_MD_KEY_SIGNING_KEY_FINGERPRINT
import net.svaroh.passly.data.metadata.datasource.local.MetadataTypesStorageConstants.TRUSTED_MD_KEY_USER_ID
import net.svaroh.passly.data.metadata.datasource.local.MetadataTypesStorageConstants.TRUSTED_MD_SIGNATURE_CREATION_TIMESTAMP
import net.svaroh.passly.data.metadata.datasource.local.MetadataTypesStorageConstants.ZERO_KNOWLEDGE_KEY_SHARE
import net.svaroh.passly.domain.metadata.datasource.MetadataSettingsLocalDataSource
import net.svaroh.passly.domain.metadata.model.MetadataKeysSettings
import net.svaroh.passly.domain.metadata.model.MetadataType
import net.svaroh.passly.domain.metadata.model.MetadataTypesSettings
import net.svaroh.passly.domain.metadata.model.TrustedMetadataKey
import net.svaroh.passly.encryptedstorage.EncryptedSharedPreferencesFactory
import timber.log.Timber
import java.time.ZonedDateTime
import java.util.UUID

internal class MetadataSettingsLocalDataSourceImpl(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory,
) : MetadataSettingsLocalDataSource {
    private fun settingsPreferences(userId: String): SharedPreferences =
        encryptedSharedPreferencesFactory.get("${MetadataSettingsFileName(userId).name}.xml")

    private fun trustedKeyPreferences(userId: String): SharedPreferences =
        encryptedSharedPreferencesFactory.get("${TrustedMetadataKeyFileName(userId).name}.xml")

    override suspend fun getMetadataKeysSettings(userId: String): MetadataKeysSettings =
        settingsPreferences(userId).let {
            MetadataKeysSettings(
                allowUsageOfPersonalKeys = it.getBoolean(ALLOW_USAGE_OF_PERSONAL_KEYS, true),
                zeroKnowledgeKeyShare = it.getBoolean(ZERO_KNOWLEDGE_KEY_SHARE, false),
            )
        }

    override suspend fun saveMetadataKeysSettings(
        metadataKeysSettings: MetadataKeysSettings,
        userId: String,
    ) {
        settingsPreferences(userId).edit {
            putBoolean(ALLOW_USAGE_OF_PERSONAL_KEYS, metadataKeysSettings.allowUsageOfPersonalKeys)
            putBoolean(ZERO_KNOWLEDGE_KEY_SHARE, metadataKeysSettings.zeroKnowledgeKeyShare)
        }
    }

    override suspend fun getMetadataTypesSettings(userId: String): MetadataTypesSettings =
        settingsPreferences(userId).let {
            MetadataTypesSettings(
                defaultMetadataType = MetadataType.valueOf(it.getString(DEFAULT_METADATA_TYPE, MetadataType.V4.name)!!),
                defaultFolderType = MetadataType.valueOf(it.getString(DEFAULT_FOLDER_TYPE, MetadataType.V4.name)!!),
                defaultTagType = MetadataType.valueOf(it.getString(DEFAULT_TAG_TYPE, MetadataType.V4.name)!!),
                allowCreationOfV5Resources = it.getBoolean(ALLOW_CREATION_OF_V5_RESOURCES, false),
                allowCreationOfV5Folders = it.getBoolean(ALLOW_CREATION_OF_V5_FOLDERS, false),
                allowCreationOfV5Tags = it.getBoolean(ALLOW_CREATION_OF_V5_TAGS, false),
                allowCreationOfV4Resources = it.getBoolean(ALLOW_CREATION_OF_V4_RESOURCES, false),
                allowCreationOfV4Folders = it.getBoolean(ALLOW_CREATION_OF_V4_FOLDERS, true),
                allowCreationOfV4Tags = it.getBoolean(ALLOW_CREATION_OF_V4_TAGS, true),
                allowV4V5Upgrade = it.getBoolean(ALLOW_V4_V5_UPGRADE, false),
                allowV5V4Downgrade = it.getBoolean(ALLOW_V5_V4_DOWNGRADE, false),
            )
        }

    override suspend fun saveMetadataTypesSettings(
        metadataTypesSettings: MetadataTypesSettings,
        userId: String,
    ) {
        settingsPreferences(userId).edit {
            putString(DEFAULT_METADATA_TYPE, metadataTypesSettings.defaultMetadataType.name)
            putString(DEFAULT_FOLDER_TYPE, metadataTypesSettings.defaultFolderType.name)
            putString(DEFAULT_TAG_TYPE, metadataTypesSettings.defaultTagType.name)
            putBoolean(ALLOW_CREATION_OF_V5_RESOURCES, metadataTypesSettings.allowCreationOfV5Resources)
            putBoolean(ALLOW_CREATION_OF_V5_FOLDERS, metadataTypesSettings.allowCreationOfV5Folders)
            putBoolean(ALLOW_CREATION_OF_V5_TAGS, metadataTypesSettings.allowCreationOfV5Tags)
            putBoolean(ALLOW_CREATION_OF_V4_RESOURCES, metadataTypesSettings.allowCreationOfV4Resources)
            putBoolean(ALLOW_CREATION_OF_V4_FOLDERS, metadataTypesSettings.allowCreationOfV4Folders)
            putBoolean(ALLOW_CREATION_OF_V4_TAGS, metadataTypesSettings.allowCreationOfV4Tags)
            putBoolean(ALLOW_V4_V5_UPGRADE, metadataTypesSettings.allowV4V5Upgrade)
            putBoolean(ALLOW_V5_V4_DOWNGRADE, metadataTypesSettings.allowV5V4Downgrade)
        }
    }

    @Suppress("LongMethod")
    override suspend fun getTrustedMetadataKey(userId: String): TrustedMetadataKey? {
        val preferences = trustedKeyPreferences(userId)

        return if (preferences.contains(TRUSTED_MD_KEY_KEY_PGP_MESSAGE)) {
            try {
                TrustedMetadataKey(
                    id = UUID.fromString(preferences.getString(TRUSTED_MD_KEY_ID, "") ?: ""),
                    userId = UUID.fromString(preferences.getString(TRUSTED_MD_KEY_USER_ID, "")),
                    keyData = preferences.getString(TRUSTED_MD_KEY_KEY_DATA, "") ?: "",
                    passphrase = preferences.getString(TRUSTED_MD_KEY_PASSPHRASE, "") ?: "",
                    created = ZonedDateTime.parse(preferences.getString(TRUSTED_MD_KEY_CREATED, "") ?: ""),
                    createdBy =
                        try {
                            UUID.fromString(preferences.getString(TRUSTED_MD_KEY_CREATED_BY, ""))
                        } catch (e: Exception) {
                            null
                        },
                    modified = ZonedDateTime.parse(preferences.getString(TRUSTED_MD_KEY_MODIFIED, "") ?: ""),
                    modifiedBy =
                        try {
                            UUID.fromString(preferences.getString(TRUSTED_MD_KEY_MODIFIED_BY, ""))
                        } catch (e: Exception) {
                            null
                        },
                    keyPgpMessage = preferences.getString(TRUSTED_MD_KEY_KEY_PGP_MESSAGE, "") ?: "",
                    signingKeyFingerprint = preferences.getString(TRUSTED_MD_KEY_SIGNING_KEY_FINGERPRINT, "") ?: "",
                    signatureCreationTimestampSeconds = preferences.getLong(TRUSTED_MD_SIGNATURE_CREATION_TIMESTAMP, 0L),
                    signedUsername = preferences.getString(TRUSTED_MD_KEY_SIGNED_USERNAME, "") ?: "",
                    signedName = preferences.getString(TRUSTED_MD_KEY_SIGNED_NAME, "") ?: "",
                )
            } catch (e: Exception) {
                Timber.e(e, "There was an error while getting the trusted metadata key")
                null
            }
        } else {
            null
        }
    }

    override suspend fun saveTrustedMetadataKey(
        trustedMetadataKey: TrustedMetadataKey,
        userId: String,
    ) {
        trustedKeyPreferences(userId).edit {
            putString(TRUSTED_MD_KEY_ID, trustedMetadataKey.id.toString())
            putString(TRUSTED_MD_KEY_USER_ID, trustedMetadataKey.userId.toString())
            putString(TRUSTED_MD_KEY_KEY_DATA, trustedMetadataKey.keyData)
            putString(TRUSTED_MD_KEY_PASSPHRASE, trustedMetadataKey.passphrase)
            putString(TRUSTED_MD_KEY_CREATED, trustedMetadataKey.created.toString())
            putString(TRUSTED_MD_KEY_CREATED_BY, trustedMetadataKey.createdBy.toString())
            putString(TRUSTED_MD_KEY_MODIFIED, trustedMetadataKey.modified.toString())
            putString(TRUSTED_MD_KEY_MODIFIED_BY, trustedMetadataKey.modifiedBy.toString())
            putString(TRUSTED_MD_KEY_KEY_PGP_MESSAGE, trustedMetadataKey.keyPgpMessage)
            putString(TRUSTED_MD_KEY_SIGNING_KEY_FINGERPRINT, trustedMetadataKey.signingKeyFingerprint)
            putString(TRUSTED_MD_KEY_SIGNED_USERNAME, trustedMetadataKey.signedUsername)
            putLong(TRUSTED_MD_SIGNATURE_CREATION_TIMESTAMP, trustedMetadataKey.signatureCreationTimestampSeconds)
            putString(TRUSTED_MD_KEY_SIGNED_NAME, trustedMetadataKey.signedName)
        }
    }

    override suspend fun deleteTrustedMetadataKey(userId: String) {
        trustedKeyPreferences(userId).edit {
            clear()
        }
    }
}
