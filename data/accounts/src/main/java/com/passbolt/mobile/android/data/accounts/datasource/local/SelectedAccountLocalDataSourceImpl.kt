package net.svaroh.passly.data.accounts.datasource.local

import androidx.core.content.edit
import net.svaroh.passly.domain.accounts.datasource.SelectedAccountLocalDataSource
import net.svaroh.passly.encryptedstorage.EncryptedSharedPreferencesFactory

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
internal class SelectedAccountLocalDataSourceImpl(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory,
) : SelectedAccountLocalDataSource {
    override fun getSelectedAccount(): String? {
        val sharedPreferences = encryptedSharedPreferencesFactory.get("${Constants.SELECTED_ACCOUNT_ALIAS}.xml")
        return sharedPreferences.getString(Constants.SELECTED_ACCOUNT_KEY, null)
    }

    override fun saveSelectedAccount(userId: String) {
        val sharedPreferences = encryptedSharedPreferencesFactory.get("${Constants.SELECTED_ACCOUNT_ALIAS}.xml")
        sharedPreferences.edit {
            putString(Constants.SELECTED_ACCOUNT_KEY, userId)
        }
    }

    override fun removeSelectedAccount() {
        val sharedPreferences = encryptedSharedPreferencesFactory.get("${Constants.SELECTED_ACCOUNT_ALIAS}.xml")
        sharedPreferences.edit {
            remove(Constants.SELECTED_ACCOUNT_KEY)
        }
    }

    override fun getCurrentApiUrl(): String? {
        val sharedPreferences = encryptedSharedPreferencesFactory.get("${Constants.CURRENT_URL_ALIAS}.xml")
        return sharedPreferences.getString(Constants.CURRENT_URL_KEY, null)
    }

    override fun saveCurrentApiUrl(currentUrl: String) {
        val sharedPreferences = encryptedSharedPreferencesFactory.get("${Constants.CURRENT_URL_ALIAS}.xml")
        sharedPreferences.edit {
            putString(Constants.CURRENT_URL_KEY, currentUrl)
        }
    }
}
