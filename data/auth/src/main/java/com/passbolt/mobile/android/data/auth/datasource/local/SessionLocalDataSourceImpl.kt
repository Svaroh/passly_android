package com.passbolt.mobile.android.data.auth.datasource.local

import androidx.core.content.edit
import com.passbolt.mobile.android.domain.auth.datasource.SessionLocalDataSource
import com.passbolt.mobile.android.domain.auth.model.Session
import com.passbolt.mobile.android.encryptedstorage.EncryptedSharedPreferencesFactory
import timber.log.Timber

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
internal class SessionLocalDataSourceImpl(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory,
) : SessionLocalDataSource {
    override fun getSession(userId: String): Session {
        try {
            val sharedPreferences = encryptedSharedPreferencesFactory.get(fileName(userId))
            return Session(
                accessToken = sharedPreferences.getString(Constants.ACCESS_TOKEN_KEY, null),
                refreshToken = sharedPreferences.getString(Constants.REFRESH_TOKEN_KEY, null),
                mfaToken = sharedPreferences.getString(Constants.MFA_TOKEN_KEY, null),
            )
        } catch (e: Exception) {
            Timber.e(e, "There was an error while getting the session")
            throw e
        }
    }

    override fun saveSession(
        userId: String,
        accessToken: String,
        refreshToken: String,
        mfaToken: String?,
    ) {
        Timber.d("Saving session.")
        try {
            val sharedPreferences = encryptedSharedPreferencesFactory.get(fileName(userId))
            sharedPreferences.edit {
                putString(Constants.ACCESS_TOKEN_KEY, accessToken)
                putString(Constants.REFRESH_TOKEN_KEY, refreshToken)
                putString(Constants.MFA_TOKEN_KEY, mfaToken)
            }
        } catch (e: Exception) {
            Timber.e(e, "There was an error while saving the session")
            throw e
        }
    }

    override fun saveMfaToken(
        userId: String,
        mfaToken: String?,
    ) {
        Timber.d("Saving MFA token.")
        try {
            val sharedPreferences = encryptedSharedPreferencesFactory.get(fileName(userId))
            sharedPreferences.edit {
                putString(Constants.MFA_TOKEN_KEY, mfaToken)
            }
        } catch (e: Exception) {
            Timber.e(e, "There was an error while saving the MFA token")
            throw e
        }
    }

    override fun removeSession(userId: String) {
        Timber.d("Removing session.")
        try {
            val sharedPreferences = encryptedSharedPreferencesFactory.get(fileName(userId))
            sharedPreferences.edit {
                remove(Constants.ACCESS_TOKEN_KEY)
                remove(Constants.REFRESH_TOKEN_KEY)
            }
        } catch (e: Exception) {
            Timber.e(e, "There was an error while removing the session")
            throw e
        }
    }

    private fun fileName(userId: String) = "${SessionFileName(userId).name}.xml"
}
