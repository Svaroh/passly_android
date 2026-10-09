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

package net.svaroh.passly.core.accounts.usecase

import android.security.keystore.KeyPermanentlyInvalidatedException
import net.svaroh.passly.domain.biometrickey.BiometricKeyRepository
import net.svaroh.passly.encryptedstorage.biometric.BiometricCipher
import net.svaroh.passly.encryptedstorage.biometric.BiometricCrypto.Companion.BIOMETRIC_KEY_ALIAS
import net.svaroh.passly.encryptedstorage.biometric.KeyStoreWrapper
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec

class BiometricCipherImpl(
    private val keyStoreWrapper: KeyStoreWrapper,
    private val biometricKeyRepository: BiometricKeyRepository,
) : BiometricCipher {
    override fun getBiometricEncryptCipher(): Cipher =
        try {
            createBiometricEncryptCipher()
        } catch (exception: Exception) {
            keyStoreWrapper.removeKey(BIOMETRIC_KEY_ALIAS)
            createBiometricEncryptCipher()
        }

    private fun createBiometricEncryptCipher(): Cipher =
        newSymmetricCipher().apply {
            val biometricKey = keyStoreWrapper.getOrCreateSymmetricKey(BIOMETRIC_KEY_ALIAS)
            init(Cipher.ENCRYPT_MODE, biometricKey)
        }

    override fun getBiometricDecryptCipher(userId: String): Cipher =
        try {
            newSymmetricCipher().apply {
                val key =
                    keyStoreWrapper.getSymmetricKey(BIOMETRIC_KEY_ALIAS)
                        ?: throw SecurityException("Unable to decrypt: No keys found")
                val iv = biometricKeyRepository.getBiometricKey(userId).iv
                init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
            }
        } catch (exception: KeyPermanentlyInvalidatedException) {
            keyStoreWrapper.removeKey(BIOMETRIC_KEY_ALIAS)
            throw exception
        } catch (exception: Exception) {
            keyStoreWrapper.removeKey(BIOMETRIC_KEY_ALIAS)
            throw KeyPermanentlyInvalidatedException("Biometric key is incompatible", exception)
        }

    companion object {
        private const val TRANSFORMATION_SYMMETRIC = "AES/CBC/PKCS7Padding"

        private fun newSymmetricCipher() = Cipher.getInstance(TRANSFORMATION_SYMMETRIC)
    }
}
