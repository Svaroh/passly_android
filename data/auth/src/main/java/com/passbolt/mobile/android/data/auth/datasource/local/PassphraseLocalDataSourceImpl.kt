package net.svaroh.passly.data.auth.datasource.local

import android.content.Context
import android.security.keystore.UserNotAuthenticatedException
import net.svaroh.passly.common.extension.erase
import net.svaroh.passly.core.passphrasememorycache.PotentialPassphrase
import net.svaroh.passly.domain.auth.datasource.PassphraseLocalDataSource
import net.svaroh.passly.encryptedstorage.EncryptedFileBaseDirectory
import net.svaroh.passly.encryptedstorage.biometric.BiometricCrypto
import timber.log.Timber
import java.io.File
import javax.crypto.Cipher

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
internal class PassphraseLocalDataSourceImpl(
    private val biometricCrypto: BiometricCrypto,
    private val appContext: Context,
) : PassphraseLocalDataSource {
    override fun getPassphrase(
        userId: String,
        authenticatedCipher: Cipher,
    ): PotentialPassphrase {
        try {
            passphraseFile(userId).readText().let {
                return if (it.isNotEmpty()) {
                    val decrypted = biometricCrypto.decryptData(it, authenticatedCipher)
                    PotentialPassphrase.Passphrase(decrypted)
                } else {
                    PotentialPassphrase.PassphraseNotPresent()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "There was an error while getting the passphrase")
            throw e
        }
    }

    @Throws(UserNotAuthenticatedException::class)
    override fun savePassphrase(
        userId: String,
        passphrase: ByteArray,
        authenticatedCipher: Cipher,
    ) {
        Timber.d("Saving passphrase file.")
        val passphraseCopy = passphrase.copyOf()
        try {
            passphraseFile(userId).outputStream().use {
                val encrypted = biometricCrypto.encryptData(passphraseCopy, authenticatedCipher)
                it.write(encrypted)
            }
        } catch (e: Exception) {
            Timber.e(e, "There was an error while saving the passphrase")
            throw e
        } finally {
            passphraseCopy.erase()
        }
    }

    override fun removePassphrase(userId: String) {
        val passphraseFile = passphraseFile(userId)
        if (passphraseFile.exists()) {
            val deleted = passphraseFile.delete()
            Timber.e("Deleted passphrase file: $deleted")
        }
    }

    override fun removeAllPassphrases(userIds: List<String>) {
        userIds
            .map { passphraseFile(it) }
            .forEach { passphraseFile ->
                if (passphraseFile.exists()) {
                    Timber.d("Passphrase file ${passphraseFile.name} scheduled deletion")
                    passphraseFile.delete()
                }
            }
    }

    override fun passphraseFileExists(userId: String): Boolean = passphraseFile(userId).exists()

    private fun passphraseFile(userId: String) =
        File(
            EncryptedFileBaseDirectory(appContext).baseDirectory,
            PassphraseFileName(userId).name,
        )
}
