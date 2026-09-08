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

package com.passbolt.mobile.android.serializers.gson

import com.passbolt.mobile.android.common.extension.erase
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.core.passphrasememorycache.usePassphraseCopy
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountUseCase
import com.passbolt.mobile.android.domain.metadata.sessionkeys.ForeignModel.RESOURCE
import com.passbolt.mobile.android.domain.metadata.sessionkeys.SessionKeysMemoryCache
import com.passbolt.mobile.android.domain.privatekey.PrivateKeyRepository
import com.passbolt.mobile.android.dto.response.MetadataKeyTypeDto.PERSONAL
import com.passbolt.mobile.android.dto.response.MetadataKeyTypeDto.SHARED
import com.passbolt.mobile.android.dto.response.ResourceResponseV5Dto
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.ui.ParsedMetadataKeyModel
import com.proton.gopenpgp.crypto.Crypto
import com.proton.gopenpgp.crypto.Key
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

class MetadataDecryptor(
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val privateKeyRepository: PrivateKeyRepository,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val metadataKeys: List<ParsedMetadataKeyModel>,
    private val openPgp: OpenPgp,
    private val sessionKeysCache: SessionKeysMemoryCache,
    private val coroutineLaunchContext: CoroutineLaunchContext,
) {
    private val cachedSharedKeys = ConcurrentHashMap<String, Key>()

    @Volatile
    private var cachedPersonalKey: Key? = null
    private val personalKeyMutex = Mutex()

    suspend fun decryptMetadata(resource: ResourceResponseV5Dto): Output {
        return try {
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

    private suspend fun getUnlockedKey(resource: ResourceResponseV5Dto): Key =
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

                    val passphraseCopy = metadataPrivateKey.passphrase.toByteArray()
                    try {
                        Crypto
                            .newPrivateKeyFromArmored(
                                metadataPrivateKey.keyData,
                                passphraseCopy,
                            ).also {
                                cachedSharedKeys[resource.metadataKeyId.toString()] = it
                            }
                    } finally {
                        passphraseCopy.erase()
                    }
                }
            }
            PERSONAL -> personalKey()
        }

    private suspend fun personalKey(): Key {
        cachedPersonalKey?.let { return it }
        return personalKeyMutex.withLock {
            cachedPersonalKey ?: run {
                val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
                val privateKey = privateKeyRepository.getPrivateKey(userId)?.armoredKey
                require(privateKey != null) { "Selected user private key not found" }
                passphraseMemoryCache.usePassphraseCopy(
                    onPassphraseNotPresent = { error("Passphrase not present in cache") },
                ) { passphrase ->
                    Crypto.newPrivateKeyFromArmored(privateKey, passphrase).also { cachedPersonalKey = it }
                }
            }
        }
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
