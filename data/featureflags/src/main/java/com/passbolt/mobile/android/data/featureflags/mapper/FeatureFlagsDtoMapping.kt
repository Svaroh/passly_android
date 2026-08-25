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

package com.passbolt.mobile.android.data.featureflags.mapper

import com.passbolt.mobile.android.dto.response.SettingsResponseDto
import com.passbolt.mobile.android.featureflags.model.FeatureFlags

internal fun SettingsResponseDto.toDomain(): FeatureFlags {
    val defaults = FeatureFlags.defaults()
    return passboltSettings.let {
        FeatureFlags(
            privacyPolicyUrl = it.legalSettings.privacyPolicyUrl.url,
            termsAndConditionsUrl = it.legalSettings.termsAndConditionsUrl.url,
            isPreviewPasswordAvailable = it.plugins.previewPassword?.enabled ?: defaults.isPreviewPasswordAvailable,
            areFoldersAvailable = it.plugins.folders?.enabled ?: defaults.areFoldersAvailable,
            areTagsAvailable = it.plugins.tags?.enabled ?: defaults.areTagsAvailable,
            isTotpAvailable = it.plugins.totpResourceTypes?.enabled ?: defaults.isTotpAvailable,
            isRbacAvailable = it.plugins.rbacs?.enabled ?: defaults.isRbacAvailable,
            isPasswordExpiryAvailable = it.plugins.passwordExpiry?.enabled ?: defaults.isPasswordExpiryAvailable,
            arePasswordPoliciesAvailable = it.plugins.passwordPolicies?.enabled ?: defaults.arePasswordPoliciesAvailable,
            canUpdatePasswordPolicies = it.plugins.passwordPoliciesUpdate?.enabled ?: defaults.canUpdatePasswordPolicies,
            isV5MetadataAvailable = it.plugins.metadata?.enabled ?: defaults.isV5MetadataAvailable,
        )
    }
}
