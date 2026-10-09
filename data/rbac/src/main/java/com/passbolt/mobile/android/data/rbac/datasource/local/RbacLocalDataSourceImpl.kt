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

package net.svaroh.passly.data.rbac.datasource.local

import android.content.SharedPreferences
import androidx.core.content.edit
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.domain.rbac.RbacLocalDataSource
import net.svaroh.passly.domain.rbac.model.Rbac
import net.svaroh.passly.domain.rbac.model.RbacRule
import net.svaroh.passly.encryptedstorage.EncryptedSharedPreferencesFactory

internal class RbacLocalDataSourceImpl(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory,
) : RbacLocalDataSource {
    override suspend fun getRbac(userId: String): DomainResult<Rbac> =
        sharedPreferences(userId).let {
            DomainResult.Finished(
                Rbac(
                    passwordPreviewRule = it.getRule(KEY_PREVIEW_PASSWORD),
                    passwordCopyRule = it.getRule(KEY_COPY_PASSWORD),
                    tagsUseRule = it.getRule(KEY_USE_TAGS),
                    foldersUseRule = it.getRule(KEY_USE_FOLDERS),
                    shareViewRule = it.getRule(KEY_VIEW_SHARE),
                ),
            )
        }

    override suspend fun setRbac(
        userId: String,
        rbac: Rbac,
    ) {
        sharedPreferences(userId).edit {
            putString(KEY_PREVIEW_PASSWORD, rbac.passwordPreviewRule.name)
            putString(KEY_COPY_PASSWORD, rbac.passwordCopyRule.name)
            putString(KEY_USE_TAGS, rbac.tagsUseRule.name)
            putString(KEY_USE_FOLDERS, rbac.foldersUseRule.name)
            putString(KEY_VIEW_SHARE, rbac.shareViewRule.name)
        }
    }

    private fun sharedPreferences(userId: String): SharedPreferences =
        encryptedSharedPreferencesFactory.get("${RbacRulesFileName(userId).name}.xml")

    private fun SharedPreferences.getRule(key: String): RbacRule = RbacRule.valueOf(getString(key, RbacRule.ALLOW.name)!!)
}
