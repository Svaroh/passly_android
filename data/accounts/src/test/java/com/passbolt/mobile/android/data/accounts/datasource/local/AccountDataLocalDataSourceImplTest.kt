package net.svaroh.passly.data.accounts.datasource.local

import android.content.SharedPreferences
import net.svaroh.passly.encryptedstorage.EncryptedSharedPreferencesFactory
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
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
class AccountDataLocalDataSourceImplTest {
    private val accountDataEditor = mock<SharedPreferences.Editor>()
    private val serverFingerprintEditor = mock<SharedPreferences.Editor>()
    private val accountDataPreferences = mock<SharedPreferences>()
    private val serverFingerprintPreferences = mock<SharedPreferences>()
    private val encryptedSharedPreferencesFactory = mock<EncryptedSharedPreferencesFactory>()
    private lateinit var dataSource: AccountDataLocalDataSourceImpl

    @Before
    fun setUp() {
        whenever(accountDataPreferences.edit()).thenReturn(accountDataEditor)
        whenever(serverFingerprintPreferences.edit()).thenReturn(serverFingerprintEditor)
        whenever(accountDataEditor.clear()).thenReturn(accountDataEditor)
        whenever(serverFingerprintEditor.clear()).thenReturn(serverFingerprintEditor)
        whenever(encryptedSharedPreferencesFactory.get("accounts_$USER_ID.xml"))
            .thenReturn(accountDataPreferences)
        whenever(encryptedSharedPreferencesFactory.get("server_fingerprint_$USER_ID.xml"))
            .thenReturn(serverFingerprintPreferences)
        dataSource = AccountDataLocalDataSourceImpl(encryptedSharedPreferencesFactory)
    }

    @Test
    fun `removeAccountData should clear all account data preferences`() {
        dataSource.removeAccountData(USER_ID)

        verify(accountDataEditor).clear()
    }

    @Test
    fun `removeAccountData should clear server fingerprint preferences`() {
        dataSource.removeAccountData(USER_ID)

        verify(serverFingerprintEditor).clear()
    }

    private companion object {
        private const val USER_ID = "user-id"
    }
}
