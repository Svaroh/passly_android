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

package net.svaroh.passly.serializers.gson

import net.svaroh.passly.core.accounts.usecase.privatekey.GetSelectedUserPrivateKeyUseCase
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.core.passphrasememorycache.PassphraseMemoryCache
import net.svaroh.passly.core.passphrasememorycache.PotentialPassphrase
import net.svaroh.passly.dto.response.MetadataKeyTypeDto.PERSONAL
import net.svaroh.passly.dto.response.MetadataKeyTypeDto.SHARED
import net.svaroh.passly.dto.response.ResourceResponseV5Dto
import net.svaroh.passly.gopenpgp.OpenPgp
import net.svaroh.passly.gopenpgp.exception.OpenPgpResult
import net.svaroh.passly.metadata.sessionkeys.ForeignModel.RESOURCE
import net.svaroh.passly.metadata.sessionkeys.SessionKeysMemoryCache
import net.svaroh.passly.ui.ParsedMetadataKeyModel
import com.proton.gopenpgp.crypto.Crypto
import com.proton.gopenpgp.crypto.Key
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

class MetadataDecryptor(
    private val getSelectedUserPrivateKeyUseCase: GetSelectedUserPrivateKeyUseCase,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val metadataKeys: List<ParsedMetadataKeyModel>,
    private val openPgp: OpenPgp,
    private val sessionKeysCache: SessionKeysMemoryCache,
    private val coroutineLaunchContext: CoroutineLaunchContext,
) {
    private val cachedSharedKeys = ConcurrentHashMap<String, Key>()
    private lateinit var cachedPersonalKey: Key

    suspend fun decryptMetadata(resource: ResourceResponseV5Dto): Output {
        return try {
            if (!::cachedPersonalKey.isInitialized) {
                val privateKey = getSelectedUserPrivateKeyUseCase.execute(Unit).privateKey
                require(privateKey != null) { "Selected user private key not found" }
                val passphrase = passphraseMemoryCache.get()
                require(passphrase is PotentialPassphrase.Passphrase) { "Passphrase not present in cache" }
                cachedPersonalKey = Crypto.newPrivateKeyFromArmored(privateKey, passphrase.passphrase)
            }

            withContext(coroutineLaunchContext.io) {
                // decrypt using cached session key
                val cachedSessionKey = sessionKeysCache.getSessionKeyHexString(RESOURCE.value, resource.id)
                if (cachedSessionKey != null) {
                    val decryptUsingCachedResult =
                        openPgp.decryptMessageArmoredWithSessionKey(
                            cachedSessionKey,
                            resource.metadata,
                        )
                    // if result is success, return it; otherwise, fallback to full decrypt
                    // session key may not be valid but we know it only when trying to use it and it fails
                    if (decryptUsingCachedResult is OpenPgpResult.Result) {
                        return@withContext Output.Success(decryptUsingCachedResult.result)
                    }
                }

                // fallback to full decrypt
                val fullDecryptResult =
                    openPgp.decryptMessageArmoredWithSessionKeyRetrieve(
                        getUnlockedKey(resource),
                        resource.metadata,
                    )
                require(fullDecryptResult is OpenPgpResult.Result) {
                    "Failed to decrypt with session key retrieve for resource id=(${resource.id}), skipping"
                }

                sessionKeysCache.put(RESOURCE.value, resource.id, fullDecryptResult.result.sessionKeyHex)

                Output.Success(fullDecryptResult.result.decryptedMessage)
            }
        } catch (exception: Exception) {
            Timber.e(exception, "Exception during metadata decryption")
            Output.Failure(exception)
        }
    }

    private fun getUnlockedKey(resource: ResourceResponseV5Dto): Key =
        when (resource.metadataKeyType) {
            SHARED -> {
                val cachedKey = cachedSharedKeys[resource.metadataKeyId.toString()]
                if (cachedKey != null) {
                    cachedKey
                } else {
                    val metadataPrivateKey =
                        metadataKeys
                            .firstOrNull { it.id == resource.metadataKeyId }
                            ?.metadataPrivateKeys
                            ?.firstOrNull()

                    require(metadataPrivateKey != null) {
                        "Metadata private key for resource id=(${resource.id}) not found, skipping"
                    }

                    Crypto
                        .newPrivateKeyFromArmored(
                            metadataPrivateKey.keyData,
                            metadataPrivateKey.passphrase.toByteArray(),
                        ).also {
                            cachedSharedKeys[resource.metadataKeyId.toString()] = it
                        }
                }
            }
            PERSONAL -> cachedPersonalKey
        }

    sealed class Output {
        data class Success(
            val decryptedMetadata: String,
        ) : Output()

        data class Failure(
            val error: Throwable?,
        ) : Output()
    }
}
