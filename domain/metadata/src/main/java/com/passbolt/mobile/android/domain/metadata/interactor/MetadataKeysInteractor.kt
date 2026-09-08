package com.passbolt.mobile.android.domain.metadata.interactor

import com.google.gson.Gson
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticatedUseCaseOutput
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState
import com.passbolt.mobile.android.core.mvp.authentication.AuthenticationState.Unauthenticated.Reason.Passphrase
import com.passbolt.mobile.android.core.mvp.authentication.CompleteAuthenticatedOutput
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.mvp.coroutinecontext.mapAsync
import com.passbolt.mobile.android.core.mvp.coroutinecontext.mapAsyncNotNull
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.core.passphrasememorycache.usePassphraseCopy
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountUseCase
import com.passbolt.mobile.android.domain.metadata.privatekeys.MetadataPrivateKeysValidator
import com.passbolt.mobile.android.domain.metadata.usecase.FetchMetadataKeysUseCase
import com.passbolt.mobile.android.domain.metadata.usecase.db.RebuildMetadataKeysTablesUseCase
import com.passbolt.mobile.android.domain.privatekey.PrivateKeyRepository
import com.passbolt.mobile.android.dto.PassphraseNotInCacheException
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.ui.DecryptedMetadataPrivateKeyJsonModel
import com.passbolt.mobile.android.ui.MetadataKeyModel
import com.passbolt.mobile.android.ui.ParsedMetadataKeyModel
import com.passbolt.mobile.android.ui.ParsedMetadataPrivateKeyModel
import timber.log.Timber
import java.time.ZonedDateTime

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

class MetadataKeysInteractor(
    private val fetchMetadataKeysUseCase: FetchMetadataKeysUseCase,
    private val rebuildMetadataKeysTablesUseCase: RebuildMetadataKeysTablesUseCase,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val privateKeyRepository: PrivateKeyRepository,
    private val openPgp: OpenPgp,
    private val gson: Gson,
    private val metadataPrivateKeysValidator: MetadataPrivateKeysValidator,
    private val coroutineLaunchContext: CoroutineLaunchContext,
) {
    suspend fun fetchAndSaveMetadataKeys(): Output =
        when (val response = fetchMetadataKeysUseCase.execute(Unit)) {
            is FetchMetadataKeysUseCase.Output.Success -> {
                try {
                    saveMetadataKeys(response.metadataKeysModel)
                } catch (_: PassphraseNotInCacheException) {
                    Output.Failure(AuthenticationState.Unauthenticated(Passphrase))
                }
            }
            is FetchMetadataKeysUseCase.Output.Failure ->
                Output.Failure(response.authenticationState)
        }

    @Suppress("LongMethod")
    @Throws(PassphraseNotInCacheException::class)
    private suspend fun saveMetadataKeys(metadataKeysModel: List<MetadataKeyModel>): Output {
        val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        val privateKey = privateKeyRepository.getPrivateKey(userId)?.armoredKey
        if (privateKey == null) {
            Timber.e("User private key not found")
            return Output.Failure(AuthenticationState.Unauthenticated(Passphrase))
        }
        return passphraseMemoryCache.usePassphraseCopy(
            onPassphraseNotPresent = { throw PassphraseNotInCacheException() },
        ) { passphrase ->
            val decryptedKeysModel =
                metadataKeysModel.mapAsync(coroutineLaunchContext) {
                    ParsedMetadataKeyModel(
                        id = it.id,
                        armoredKey = it.armoredKey,
                        fingerprint = it.fingerprint,
                        modified = it.modified,
                        expired = it.expired,
                        deleted = it.deleted,
                        metadataPrivateKeys =
                            it.metadataPrivateKeys.mapAsyncNotNull(coroutineLaunchContext) { metadataPrivateKey ->
                                val decryptedKeyData =
                                    openPgp.decryptMessageArmored(
                                        privateKey,
                                        passphrase,
                                        metadataPrivateKey.pgpMessage,
                                    )
                                when (decryptedKeyData) {
                                    is OpenPgpResult.Error -> null
                                    is OpenPgpResult.Result -> {
                                        val keyModel =
                                            gson.fromJson(
                                                decryptedKeyData.result,
                                                DecryptedMetadataPrivateKeyJsonModel::class.java,
                                            )
                                        if (metadataPrivateKeysValidator.isValid(keyModel)) {
                                            ParsedMetadataPrivateKeyModel(
                                                id = metadataPrivateKey.id,
                                                userId = metadataPrivateKey.userId,
                                                keyData = keyModel.armoredKey,
                                                passphrase = keyModel.passphrase,
                                                pgpMessage = metadataPrivateKey.pgpMessage,
                                                created = ZonedDateTime.parse(metadataPrivateKey.created),
                                                createdBy = metadataPrivateKey.createdBy,
                                                modified = ZonedDateTime.parse(metadataPrivateKey.modified),
                                                modifiedBy = metadataPrivateKey.modifiedBy,
                                                fingerprint = keyModel.fingerprint,
                                                domain = keyModel.domain,
                                            )
                                        } else {
                                            Timber.e(
                                                "Invalid metadata private key for metadata " +
                                                    "key: ${metadataPrivateKey.metadataKeyId}",
                                            )
                                            null
                                        }
                                    }
                                }
                            },
                    )
                }
            rebuildMetadataKeysTablesUseCase.execute(
                RebuildMetadataKeysTablesUseCase.Input(decryptedKeysModel),
            )
            Output.Success
        }
    }

    sealed class Output : AuthenticatedUseCaseOutput {
        data object Success :
            Output(),
            CompleteAuthenticatedOutput

        data class Failure(
            override val authenticationState: AuthenticationState,
        ) : Output()
    }
}
