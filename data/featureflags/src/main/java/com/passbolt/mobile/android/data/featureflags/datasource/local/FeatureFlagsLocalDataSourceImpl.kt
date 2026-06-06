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

package net.svaroh.passly.data.featureflags.datasource.local

import android.content.SharedPreferences
import androidx.core.content.edit
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.data.featureflags.datasource.local.StorageConstants.FOLDERS_KEY
import net.svaroh.passly.data.featureflags.datasource.local.StorageConstants.PASSWORD_EXPIRY_KEY
import net.svaroh.passly.data.featureflags.datasource.local.StorageConstants.PASSWORD_POLICIES_KEY
import net.svaroh.passly.data.featureflags.datasource.local.StorageConstants.PASSWORD_POLICIES_UPDATE_KEY
import net.svaroh.passly.data.featureflags.datasource.local.StorageConstants.PREVIEW_PASSWORD_KEY
import net.svaroh.passly.data.featureflags.datasource.local.StorageConstants.PRIVACY_POLICY_KEY
import net.svaroh.passly.data.featureflags.datasource.local.StorageConstants.RBAC_KEY
import net.svaroh.passly.data.featureflags.datasource.local.StorageConstants.TAGS_KEY
import net.svaroh.passly.data.featureflags.datasource.local.StorageConstants.TERMS_AND_CONDITIONS_KEY
import net.svaroh.passly.data.featureflags.datasource.local.StorageConstants.TOTP_KEY
import net.svaroh.passly.data.featureflags.datasource.local.StorageConstants.V5_METADATA
import net.svaroh.passly.encryptedstorage.EncryptedSharedPreferencesFactory
import net.svaroh.passly.featureflags.FeatureFlagsLocalDataSource
import net.svaroh.passly.featureflags.model.FeatureFlags

internal class FeatureFlagsLocalDataSourceImpl(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory,
) : FeatureFlagsLocalDataSource {
    override suspend fun getFeatureFlags(userId: String): DomainResult<FeatureFlags> {
        val defaults = FeatureFlags.defaults()
        return sharedPreferences(userId).let {
            DomainResult.Finished(
                FeatureFlags(
                    privacyPolicyUrl = it.getString(PRIVACY_POLICY_KEY, defaults.privacyPolicyUrl),
                    termsAndConditionsUrl = it.getString(TERMS_AND_CONDITIONS_KEY, defaults.termsAndConditionsUrl),
                    isPreviewPasswordAvailable = it.getBoolean(PREVIEW_PASSWORD_KEY, defaults.isPreviewPasswordAvailable),
                    areFoldersAvailable = it.getBoolean(FOLDERS_KEY, defaults.areFoldersAvailable),
                    areTagsAvailable = it.getBoolean(TAGS_KEY, defaults.areTagsAvailable),
                    isTotpAvailable = it.getBoolean(TOTP_KEY, defaults.isTotpAvailable),
                    isRbacAvailable = it.getBoolean(RBAC_KEY, defaults.isRbacAvailable),
                    isPasswordExpiryAvailable = it.getBoolean(PASSWORD_EXPIRY_KEY, defaults.isPasswordExpiryAvailable),
                    arePasswordPoliciesAvailable = it.getBoolean(PASSWORD_POLICIES_KEY, defaults.arePasswordPoliciesAvailable),
                    canUpdatePasswordPolicies = it.getBoolean(PASSWORD_POLICIES_UPDATE_KEY, defaults.canUpdatePasswordPolicies),
                    isV5MetadataAvailable = it.getBoolean(V5_METADATA, defaults.isV5MetadataAvailable),
                ),
            )
        }
    }

    override suspend fun setFeatureFlags(
        userId: String,
        featureFlags: FeatureFlags,
    ) {
        sharedPreferences(userId).edit {
            putString(PRIVACY_POLICY_KEY, featureFlags.privacyPolicyUrl)
            putString(TERMS_AND_CONDITIONS_KEY, featureFlags.termsAndConditionsUrl)
            putBoolean(PREVIEW_PASSWORD_KEY, featureFlags.isPreviewPasswordAvailable)
            putBoolean(FOLDERS_KEY, featureFlags.areFoldersAvailable)
            putBoolean(TAGS_KEY, featureFlags.areTagsAvailable)
            putBoolean(TOTP_KEY, featureFlags.isTotpAvailable)
            putBoolean(RBAC_KEY, featureFlags.isRbacAvailable)
            putBoolean(PASSWORD_EXPIRY_KEY, featureFlags.isPasswordExpiryAvailable)
            putBoolean(PASSWORD_POLICIES_KEY, featureFlags.arePasswordPoliciesAvailable)
            putBoolean(PASSWORD_POLICIES_UPDATE_KEY, featureFlags.canUpdatePasswordPolicies)
            putBoolean(V5_METADATA, featureFlags.isV5MetadataAvailable)
        }
    }

    private fun sharedPreferences(userId: String): SharedPreferences =
        encryptedSharedPreferencesFactory.get("${FeatureFlagsFileName(userId).name}.xml")
}
