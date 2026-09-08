package com.passbolt.mobile.android.data.auth.datasource.local

import android.content.SharedPreferences
import com.passbolt.mobile.android.encryptedstorage.EncryptedSharedPreferencesFactory
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

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
class SessionLocalDataSourceImplTest {
    private val editor = mock<SharedPreferences.Editor>()
    private val sharedPreferences =
        mock<SharedPreferences> {
            on { edit() } doReturn editor
        }
    private val encryptedSharedPreferencesFactory =
        mock<EncryptedSharedPreferencesFactory> {
            on { get(any()) } doReturn sharedPreferences
        }

    private val dataSource = SessionLocalDataSourceImpl(encryptedSharedPreferencesFactory)

    @Test
    fun `saveSession should save only access and refresh token keys`() {
        whenever(editor.putString(any(), any())) doReturn editor

        dataSource.saveSession(USER_ID, ACCESS_TOKEN, REFRESH_TOKEN)

        verify(editor).putString(Constants.ACCESS_TOKEN_KEY, ACCESS_TOKEN)
        verify(editor).putString(Constants.REFRESH_TOKEN_KEY, REFRESH_TOKEN)
        verify(editor, never()).putString(Constants.MFA_TOKEN_KEY, MFA_TOKEN)
    }

    @Test
    fun `saveMfaToken should save only the mfa token key`() {
        whenever(editor.putString(any(), any())) doReturn editor

        dataSource.saveMfaToken(USER_ID, MFA_TOKEN)

        verify(editor).putString(Constants.MFA_TOKEN_KEY, MFA_TOKEN)
        verify(editor, never()).putString(Constants.ACCESS_TOKEN_KEY, ACCESS_TOKEN)
        verify(editor, never()).putString(Constants.REFRESH_TOKEN_KEY, REFRESH_TOKEN)
    }

    @Test
    fun `removeSession should remove only access and refresh token keys and keep the mfa token`() {
        whenever(editor.remove(any())) doReturn editor

        dataSource.removeSession(USER_ID)

        verify(editor).remove(Constants.ACCESS_TOKEN_KEY)
        verify(editor).remove(Constants.REFRESH_TOKEN_KEY)
        verify(editor, never()).remove(Constants.MFA_TOKEN_KEY)
    }

    @Test
    fun `removeMfaToken should remove only the mfa token key`() {
        whenever(editor.remove(any())) doReturn editor

        dataSource.removeMfaToken(USER_ID)

        verify(editor).remove(Constants.MFA_TOKEN_KEY)
        verify(editor, never()).remove(Constants.ACCESS_TOKEN_KEY)
        verify(editor, never()).remove(Constants.REFRESH_TOKEN_KEY)
    }

    private companion object {
        private const val USER_ID = "user-id"
        private const val ACCESS_TOKEN = "access-token"
        private const val REFRESH_TOKEN = "refresh-token"
        private const val MFA_TOKEN = "mfa-token"
    }
}
