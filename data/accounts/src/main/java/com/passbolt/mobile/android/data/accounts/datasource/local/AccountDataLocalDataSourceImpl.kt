package com.passbolt.mobile.android.data.accounts.datasource.local

import androidx.core.content.edit
import com.passbolt.mobile.android.data.accounts.datasource.local.Constants.AVATAR_URL_KEY
import com.passbolt.mobile.android.data.accounts.datasource.local.Constants.EMAIL_KEY
import com.passbolt.mobile.android.data.accounts.datasource.local.Constants.ROLE_KEY
import com.passbolt.mobile.android.data.accounts.datasource.local.Constants.SERVER_ID_KEY
import com.passbolt.mobile.android.data.accounts.datasource.local.Constants.URL_KEY
import com.passbolt.mobile.android.data.accounts.datasource.local.Constants.USER_FIRST_NAME_KEY
import com.passbolt.mobile.android.data.accounts.datasource.local.Constants.USER_LABEL_KEY
import com.passbolt.mobile.android.data.accounts.datasource.local.Constants.USER_LAST_NAME_KEY
import com.passbolt.mobile.android.domain.accounts.datasource.AccountDataLocalDataSource
import com.passbolt.mobile.android.domain.accounts.model.AccountData
import com.passbolt.mobile.android.domain.accounts.model.AccountDataUpdate
import com.passbolt.mobile.android.encryptedstorage.EncryptedSharedPreferencesFactory

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
internal class AccountDataLocalDataSourceImpl(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory,
) : AccountDataLocalDataSource {
    override fun getAccountData(userId: String): AccountData {
        val sharedPreferences = encryptedSharedPreferencesFactory.get(accountDataFileName(userId))

        return AccountData(
            firstName = sharedPreferences.getString(USER_FIRST_NAME_KEY, null),
            lastName = sharedPreferences.getString(USER_LAST_NAME_KEY, null),
            email = sharedPreferences.getString(EMAIL_KEY, null),
            avatarUrl = sharedPreferences.getString(AVATAR_URL_KEY, null),
            url = sharedPreferences.getString(URL_KEY, "").orEmpty(),
            serverId = sharedPreferences.getString(SERVER_ID_KEY, ""),
            label = sharedPreferences.getString(USER_LABEL_KEY, null),
            role = sharedPreferences.getString(ROLE_KEY, null),
        )
    }

    override fun updateAccountData(update: AccountDataUpdate) {
        val sharedPreferences = encryptedSharedPreferencesFactory.get(accountDataFileName(update.userId))
        sharedPreferences.edit {
            with(update) {
                firstName?.let { putString(USER_FIRST_NAME_KEY, it) }
                lastName?.let { putString(USER_LAST_NAME_KEY, it) }
                label?.let { putString(USER_LABEL_KEY, it) }
                email?.let { putString(EMAIL_KEY, it) }
                avatarUrl?.let { putString(AVATAR_URL_KEY, it) }
                url?.let { putString(URL_KEY, it) }
                serverId?.let { putString(SERVER_ID_KEY, it) }
                role?.let { putString(ROLE_KEY, it) }
            }
        }
    }

    override fun removeAccountData(userId: String) {
        encryptedSharedPreferencesFactory.get(accountDataFileName(userId)).edit { clear() }
        encryptedSharedPreferencesFactory.get(serverFingerprintFileName(userId)).edit { clear() }
    }

    override fun saveServerFingerprint(
        userId: String,
        fingerprint: String,
    ) {
        val sharedPreferences = encryptedSharedPreferencesFactory.get(serverFingerprintFileName(userId))
        sharedPreferences.edit {
            putString(Constants.SERVER_FINGERPRINT_KEY, fingerprint)
        }
    }

    override fun isServerFingerprintCorrect(
        userId: String,
        fingerprint: String,
    ): Boolean {
        val sharedPreferences = encryptedSharedPreferencesFactory.get(serverFingerprintFileName(userId))
        val serverFingerprint = sharedPreferences.getString(Constants.SERVER_FINGERPRINT_KEY, null)

        return serverFingerprint.isNullOrEmpty() || serverFingerprint.equals(fingerprint, ignoreCase = true)
    }

    private fun accountDataFileName(userId: String) = "${AccountDataFileName(userId).name}.xml"

    private fun serverFingerprintFileName(userId: String) = "${ServerFingerprintFileName(userId).name}.xml"
}
