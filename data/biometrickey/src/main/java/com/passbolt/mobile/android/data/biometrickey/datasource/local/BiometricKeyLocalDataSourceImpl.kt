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

package com.passbolt.mobile.android.data.biometrickey.datasource.local

import android.util.Base64
import androidx.core.content.edit
import com.passbolt.mobile.android.data.biometrickey.BiometricKeyIvFileName
import com.passbolt.mobile.android.domain.biometrickey.BiometricKeyLocalDataSource
import com.passbolt.mobile.android.domain.biometrickey.model.BiometricKey
import com.passbolt.mobile.android.encryptedstorage.EncryptedSharedPreferencesFactory
import com.passbolt.mobile.android.encryptedstorage.biometric.BiometricCrypto
import com.passbolt.mobile.android.encryptedstorage.biometric.KeyStoreWrapper

internal class BiometricKeyLocalDataSourceImpl(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory,
    private val keyStoreWrapper: KeyStoreWrapper,
) : BiometricKeyLocalDataSource {
    override fun getBiometricKey(userId: String): BiometricKey {
        val fileName = BiometricKeyIvFileName(userId)
        val encodedIv =
            encryptedSharedPreferencesFactory
                .get(fileName.name)
                .getString(IV_KEY, "")
        require(!encodedIv.isNullOrBlank())
        return BiometricKey(Base64.decode(encodedIv, Base64.DEFAULT))
    }

    override fun saveBiometricKey(
        userId: String,
        biometricKey: BiometricKey,
    ) {
        val fileName = BiometricKeyIvFileName(userId)
        encryptedSharedPreferencesFactory.get(fileName.name).edit {
            val encodedIv = Base64.encodeToString(biometricKey.iv, Base64.DEFAULT)
            putString(IV_KEY, encodedIv)
        }
    }

    override fun removeBiometricKey() {
        keyStoreWrapper.removeKey(BiometricCrypto.BIOMETRIC_KEY_ALIAS)
    }

    private companion object {
        private const val IV_KEY = "IV"
    }
}
