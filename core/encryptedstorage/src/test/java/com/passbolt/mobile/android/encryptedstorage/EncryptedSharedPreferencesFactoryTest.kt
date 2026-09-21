package com.passbolt.mobile.android.encryptedstorage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
import androidx.security.crypto.MasterKey
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.verification.VerificationMode
import java.security.GeneralSecurityException

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
class EncryptedSharedPreferencesFactoryTest {
    private val context = mock<Context>()
    private val masterKey = mock<MasterKey>()
    private lateinit var encryptedSharedPreferences: MockedStatic<EncryptedSharedPreferences>
    private lateinit var factory: EncryptedSharedPreferencesFactory

    @Before
    fun setUp() {
        encryptedSharedPreferences = mockStatic(EncryptedSharedPreferences::class.java)
        factory = EncryptedSharedPreferencesFactory(context, masterKey)
    }

    @After
    fun tearDown() {
        encryptedSharedPreferences.close()
    }

    @Test
    fun `get should open a file once and reuse the instance`() {
        val prefs = mock<SharedPreferences>()
        stubCreate(FILE_NAME).thenReturn(prefs)

        val first = factory.get(FILE_NAME)
        val second = factory.get(FILE_NAME)

        assertThat(first).isSameInstanceAs(prefs)
        assertThat(second).isSameInstanceAs(prefs)
        verifyCreate(FILE_NAME, times(1))
    }

    @Test
    fun `get should keep a separate instance per file name`() {
        val prefs = mock<SharedPreferences>()
        val otherPrefs = mock<SharedPreferences>()
        stubCreate(FILE_NAME).thenReturn(prefs)
        stubCreate(OTHER_FILE_NAME).thenReturn(otherPrefs)

        assertThat(factory.get(FILE_NAME)).isSameInstanceAs(prefs)
        assertThat(factory.get(OTHER_FILE_NAME)).isSameInstanceAs(otherPrefs)
    }

    @Test
    fun `get should not cache a failed open`() {
        val prefs = mock<SharedPreferences>()
        stubCreate(FILE_NAME)
            .thenThrow(GeneralSecurityException())
            .thenReturn(prefs)

        val failure = runCatching { factory.get(FILE_NAME) }
        val retry = factory.get(FILE_NAME)

        assertThat(failure.exceptionOrNull()).isInstanceOf(GeneralSecurityException::class.java)
        assertThat(retry).isSameInstanceAs(prefs)
        verifyCreate(FILE_NAME, times(2))
    }

    private fun stubCreate(fileName: String) =
        encryptedSharedPreferences.`when`<SharedPreferences> {
            EncryptedSharedPreferences.create(context, fileName, masterKey, AES256_SIV, AES256_GCM)
        }

    private fun verifyCreate(
        fileName: String,
        mode: VerificationMode,
    ) {
        encryptedSharedPreferences.verify(
            { EncryptedSharedPreferences.create(context, fileName, masterKey, AES256_SIV, AES256_GCM) },
            mode,
        )
    }

    private companion object {
        private const val FILE_NAME = "file.xml"
        private const val OTHER_FILE_NAME = "other_file.xml"
    }
}
