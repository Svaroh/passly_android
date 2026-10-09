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

package net.svaroh.passly.domain.resources.actions

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.transform
import net.svaroh.passly.domain.secrets.model.SecretJsonModel
import net.svaroh.passly.domain.secrets.parser.SecretParser
import net.svaroh.passly.domain.secrets.usecase.decrypt.SecretInteractor
import net.svaroh.passly.feature.authentication.session.runAuthenticatedOperation
import net.svaroh.passly.jsonmodel.delegates.SecretCustomFieldsModel
import net.svaroh.passly.jsonmodel.delegates.TotpSecret
import net.svaroh.passly.ui.DecryptedSecretOrError
import net.svaroh.passly.ui.ResourceUiModel
import net.svaroh.passly.ui.contentType
import timber.log.Timber

class SecretPropertiesActionsInteractor(
    private val resource: ResourceUiModel,
    private val secretParser: SecretParser,
    private val secretInteractor: SecretInteractor,
) {
    suspend fun provideDecryptedSecret(): Flow<SecretPropertyActionResult<SecretJsonModel>> =
        fetchAndDecrypt()
            .mapSuccess {
                when (val secret = secretParser.parseSecret(resource.slug, it.secret)) {
                    is DecryptedSecretOrError.DecryptedSecret ->
                        SecretPropertyActionResult.Success(
                            JSON_SECRET_LABEL,
                            isSecret = true,
                            secret.secret,
                        )

                    is DecryptedSecretOrError.Error ->
                        SecretPropertyActionResult.DecryptionFailure()
                }
            }

    suspend fun provideNote(): Flow<SecretPropertyActionResult<String>> =
        fetchAndDecrypt()
            .mapSuccess {
                when (val description = secretParser.parseSecret(resource.slug, it.secret)) {
                    is DecryptedSecretOrError.DecryptedSecret ->
                        SecretPropertyActionResult.Success(
                            NOTE_LABEL,
                            isSecret = true,
                            description.secret.description.orEmpty(),
                        )

                    is DecryptedSecretOrError.Error ->
                        SecretPropertyActionResult.DecryptionFailure()
                }
            }

    suspend fun providePassword(): Flow<SecretPropertyActionResult<String?>> =
        fetchAndDecrypt()
            .mapSuccess {
                when (val password = secretParser.parseSecret(resource.slug, it.secret)) {
                    is DecryptedSecretOrError.DecryptedSecret ->
                        SecretPropertyActionResult.Success(
                            SECRET_LABEL,
                            isSecret = true,
                            password.secret.getPassword(resource.contentType()),
                        )
                    is DecryptedSecretOrError.Error ->
                        SecretPropertyActionResult.DecryptionFailure()
                }
            }

    suspend fun provideOtp(): Flow<SecretPropertyActionResult<TotpSecret>> =
        fetchAndDecrypt()
            .mapSuccess {
                when (val totp = secretParser.parseSecret(resource.slug, it.secret)) {
                    is DecryptedSecretOrError.DecryptedSecret ->
                        SecretPropertyActionResult.Success(
                            OTP_LABEL,
                            isSecret = true,
                            requireNotNull(totp.secret.totp),
                        )
                    is DecryptedSecretOrError.Error ->
                        SecretPropertyActionResult.DecryptionFailure()
                }
            }

    suspend fun provideCustomFields(): Flow<SecretPropertyActionResult<SecretCustomFieldsModel?>> =
        fetchAndDecrypt()
            .mapSuccess {
                when (val secretModel = secretParser.parseSecret(resource.slug, it.secret)) {
                    is DecryptedSecretOrError.DecryptedSecret -> {
                        SecretPropertyActionResult.Success(
                            CUSTOM_FIELD_LABEL,
                            isSecret = true,
                            secretModel.secret.customFields,
                        )
                    }
                    is DecryptedSecretOrError.Error ->
                        SecretPropertyActionResult.DecryptionFailure()
                }
            }

    suspend fun providePinCode(): Flow<SecretPropertyActionResult<String>> =
        fetchAndDecrypt()
            .mapSuccess {
                when (val secret = secretParser.parseSecret(resource.slug, it.secret)) {
                    is DecryptedSecretOrError.DecryptedSecret ->
                        SecretPropertyActionResult.Success(
                            PIN_CODE_LABEL,
                            isSecret = true,
                            secret.secret.pinCode.orEmpty(),
                        )
                    is DecryptedSecretOrError.Error ->
                        SecretPropertyActionResult.DecryptionFailure()
                }
            }

    /**
     * Reading a secret is local work: [SecretInteractor] serves the stored ciphertext and only asks the network for a
     * secret this device has never seen. Checking the server session before that read is what made showing a password
     * hang exactly as signing in did - a session probe is itself a request, so an unreachable server spent the whole
     * connection budget before anything was shown, and a device with no session at all was sent to the sign in
     * screen for data it already held. The passphrase is still required, and asking for it stays local.
     */
    private suspend fun fetchAndDecrypt(): Flow<SecretFetchAndDecryptResult> =
        flowOf(
            when (
                val output =
                    runAuthenticatedOperation(requiresSession = false) {
                        secretInteractor.fetchAndDecrypt(resource.resourceId)
                    }
            ) {
                is SecretInteractor.Output.DecryptFailure ->
                    SecretFetchAndDecryptResult.DecryptFailure
                is SecretInteractor.Output.FetchFailure ->
                    SecretFetchAndDecryptResult.FetchFailure
                is SecretInteractor.Output.Success ->
                    SecretFetchAndDecryptResult.Success(output.decryptedSecret)
                is SecretInteractor.Output.Unauthorized ->
                    SecretFetchAndDecryptResult.Unauthorized
            },
        )

    private inline fun <T> Flow<SecretFetchAndDecryptResult>.mapSuccess(
        crossinline transform: suspend (value: SecretFetchAndDecryptResult.Success) -> SecretPropertyActionResult<T>,
    ): Flow<SecretPropertyActionResult<T>> =
        transform {
            emit(
                when (it) {
                    is SecretFetchAndDecryptResult.DecryptFailure ->
                        SecretPropertyActionResult.DecryptionFailure()
                    is SecretFetchAndDecryptResult.FetchFailure ->
                        SecretPropertyActionResult.FetchFailure()
                    is SecretFetchAndDecryptResult.Unauthorized ->
                        SecretPropertyActionResult.Unauthorized()
                    is SecretFetchAndDecryptResult.Success -> {
                        transform(it)
                    }
                },
            )
        }

    private sealed class SecretFetchAndDecryptResult {
        data object FetchFailure : SecretFetchAndDecryptResult()

        data object DecryptFailure : SecretFetchAndDecryptResult()

        data object Unauthorized : SecretFetchAndDecryptResult()

        class Success(
            val secret: String,
        ) : SecretFetchAndDecryptResult()
    }

    companion object {
        @VisibleForTesting
        const val SECRET_LABEL = "Secret"

        @VisibleForTesting
        const val NOTE_LABEL = "Note"

        @VisibleForTesting
        const val JSON_SECRET_LABEL = "JSON Secret"

        @VisibleForTesting
        const val OTP_LABEL = "TOTP"

        @VisibleForTesting
        const val CUSTOM_FIELD_LABEL = "Custom Field"

        @VisibleForTesting
        const val PIN_CODE_LABEL = "PIN Code"
    }
}

suspend fun <T> performSecretPropertyAction(
    action: suspend () -> Flow<SecretPropertyActionResult<T>>,
    doOnFetchFailure: () -> Unit,
    doOnDecryptionFailure: () -> Unit,
    doOnSuccess: (SecretPropertyActionResult.Success<T>) -> Unit,
) {
    action().single().let {
        when (it) {
            is SecretPropertyActionResult.DecryptionFailure -> {
                doOnDecryptionFailure()
            }
            is SecretPropertyActionResult.FetchFailure -> {
                doOnFetchFailure()
            }
            is SecretPropertyActionResult.Success -> {
                doOnSuccess(it)
            }
            is SecretPropertyActionResult.Unauthorized -> {
                // can be ignored - runAuthenticatedOperation handles it
                Timber.d("Unauthorized during decrypting secret")
            }
        }
    }
}
