package net.svaroh.passly.data.auth.datasource.local

import androidx.core.content.edit
import net.svaroh.passly.domain.auth.datasource.AuthLocalDataSource
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
internal class AuthLocalDataSourceImpl(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory,
) : AuthLocalDataSource {
    override fun getServerRsaKey(userId: String): String? {
        val sharedPreferences = encryptedSharedPreferencesFactory.get(fileName(userId))
        return sharedPreferences.getString(SERVER_RSA_KEY_KEY, null)
    }

    override fun saveServerRsaKey(
        userId: String,
        rsaKey: String,
    ) {
        val sharedPreferences = encryptedSharedPreferencesFactory.get(fileName(userId))
        sharedPreferences.edit {
            putString(SERVER_RSA_KEY_KEY, rsaKey)
        }
    }

    override fun removeServerRsaKey(userId: String) {
        val sharedPreferences = encryptedSharedPreferencesFactory.get(fileName(userId))
        sharedPreferences.edit {
            remove(SERVER_RSA_KEY_KEY)
        }
    }

    private fun fileName(userId: String) = "${ServerRsaKeyFileName(userId).name}.xml"
}
