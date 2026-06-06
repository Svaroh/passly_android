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

package net.svaroh.passly.data.privatekey.datasource.local

import android.content.Context
import net.svaroh.passly.data.privatekey.PrivateKeyFileName
import net.svaroh.passly.domain.privatekey.datasource.PrivateKeyLocalDataSource
import net.svaroh.passly.domain.privatekey.model.PrivateKey
import net.svaroh.passly.encryptedstorage.EncryptedFileBaseDirectory
import net.svaroh.passly.encryptedstorage.EncryptedFileFactory
import timber.log.Timber
import java.io.File
import java.io.IOException

internal class PrivateKeyLocalDataSourceImpl(
    private val appContext: Context,
    private val encryptedFileFactory: EncryptedFileFactory,
) : PrivateKeyLocalDataSource {
    override fun getPrivateKey(userId: String): PrivateKey? {
        val name = PrivateKeyFileName(userId).name
        return try {
            val encryptedFile = encryptedFileFactory.get(name)
            encryptedFile.openFileInput().use {
                val bytes = it.readBytes()
                PrivateKey(String(bytes))
            }
        } catch (exception: IOException) {
            Timber.e(exception)
            null
        }
    }

    override fun savePrivateKey(
        userId: String,
        privateKey: PrivateKey,
    ): Boolean {
        val name = PrivateKeyFileName(userId).name
        Timber.d("Saving private key.")

        val encryptedFile = encryptedFileFactory.get(name)
        return try {
            val bytes = privateKey.armoredKey.toByteArray()
            encryptedFile.openFileOutput().use {
                it.write(bytes)
            }
            true
        } catch (e: Exception) {
            Timber.e(e)
            false
        }
    }

    override fun removePrivateKey(userId: String) {
        val privateKeyFile =
            File(
                EncryptedFileBaseDirectory(appContext).baseDirectory,
                PrivateKeyFileName(userId).name,
            )
        if (privateKeyFile.exists()) {
            val deleted = privateKeyFile.delete()
            Timber.e("Deleted private key file: $deleted")
        }
    }
}
