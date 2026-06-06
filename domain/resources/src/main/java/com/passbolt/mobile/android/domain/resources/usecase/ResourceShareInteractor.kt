package net.svaroh.passly.domain.resources.usecase

import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.architecture.result.displayMessage
import net.svaroh.passly.core.mvp.authentication.AuthenticatedUseCaseOutput
import net.svaroh.passly.core.mvp.authentication.AuthenticationState
import net.svaroh.passly.core.mvp.authentication.UnauthenticatedReason
import net.svaroh.passly.core.passphrasememorycache.PassphraseMemoryCache
import net.svaroh.passly.core.passphrasememorycache.usePassphraseCopy
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import net.svaroh.passly.domain.privatekey.PrivateKeyRepository
import net.svaroh.passly.domain.resources.usecase.db.GetLocalResourcePermissionsUseCase
import net.svaroh.passly.domain.secrets.usecase.decrypt.SecretInteractor
import net.svaroh.passly.domain.share.model.ShareRecipient
import net.svaroh.passly.domain.users.usecase.GetLocalUserUseCase
import net.svaroh.passly.gopenpgp.OpenPgp
import net.svaroh.passly.gopenpgp.exception.OpenPgpResult
import net.svaroh.passly.mappers.SharePermissionsModelMapper
import net.svaroh.passly.ui.EncryptedSecretOrError
import net.svaroh.passly.ui.PermissionModelUi
import timber.log.Timber

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
class ResourceShareInteractor(
    private val getLocalResourcePermissionsUseCase: GetLocalResourcePermissionsUseCase,
    private val getLocalUserUseCase: GetLocalUserUseCase,
    private val simulateShareUseCase: SimulateShareResourceUseCase,
    private val shareResourceUseCase: ShareResourceUseCase,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
    private val privateKeyRepository: PrivateKeyRepository,
    private val secretInteractor: SecretInteractor,
    private val openPgp: OpenPgp,
    private val passphraseMemoryCache: PassphraseMemoryCache,
    private val sharePermissionsModelMapper: SharePermissionsModelMapper,
) {
    suspend fun simulateAndShareResource(
        resourceId: String,
        recipients: List<PermissionModelUi>,
        recipientsPublicKeys: Map<String, String> = emptyMap(),
        existingPermissions: List<PermissionModelUi>? = null,
    ): Output {
        val existingResourcePermissions =
            existingPermissions
                ?: getLocalResourcePermissionsUseCase
                    .execute(GetLocalResourcePermissionsUseCase.Input(resourceId))
                    .permissions

        val simulateSharePermissions =
            sharePermissionsModelMapper
                .mapForSimulation(
                    SharePermissionsModelMapper.ShareItem.Resource(resourceId),
                    recipients,
                    existingResourcePermissions,
                )

        Timber.d("Starting share simulation")
        return when (
            val simulateShareOutput =
                simulateShareUseCase.execute(
                    SimulateShareResourceUseCase.Input(resourceId, simulateSharePermissions),
                )
        ) {
            is SimulateShareResourceUseCase.Output.Success -> {
                val unconfirmedRecipients =
                    if (recipientsPublicKeys.isEmpty()) {
                        emptyList()
                    } else {
                        simulateShareOutput.value.added.filter { it.userId !in recipientsPublicKeys }
                    }
                if (unconfirmedRecipients.isNotEmpty()) {
                    Timber.e(
                        "Permissions drift detected - share simulation reported " +
                            "${unconfirmedRecipients.size} recipient(s) outside of the confirmed snapshot",
                    )
                    Output.DriftDetected
                } else {
                    Timber.d("Share simulation success; Starting to share resource")
                    shareResource(
                        resourceId,
                        recipients,
                        existingResourcePermissions,
                        simulateShareOutput.value.added,
                        recipientsPublicKeys,
                    )
                }
            }
            is SimulateShareResourceUseCase.Output.Failure -> {
                Timber.e("Share simulation failure: %s", simulateShareOutput.message)
                Output.SimulateShareFailure(simulateShareOutput.incomplete)
            }
        }
    }

    @Suppress("LongMethod")
    private suspend fun shareResource(
        resourceId: String,
        recipients: List<PermissionModelUi>,
        existingPermissions: List<PermissionModelUi>,
        newUsers: List<ShareRecipient>,
        recipientsPublicKeys: Map<String, String>,
    ): Output {
        return when (val secretOutput = secretInteractor.fetchAndDecrypt(resourceId)) {
            is SecretInteractor.Output.DecryptFailure -> {
                Timber.e("Secret decrypt failure: %s", secretOutput.error.message)
                Output.SecretDecryptFailure(secretOutput.error.message)
            }
            is SecretInteractor.Output.FetchFailure -> {
                Timber.e("Secret fetch failure: %s", secretOutput.incomplete.displayMessage())
                Output.SecretFetchFailure(secretOutput.incomplete)
            }
            is SecretInteractor.Output.Unauthorized -> {
                Timber.d("Unauthorized during secret fetch")
                Output.Unauthorized(secretOutput.reason)
            }
            is SecretInteractor.Output.Success -> {
                Timber.d("Secret fetched")
                passphraseMemoryCache.usePassphraseCopy(
                    onPassphraseNotPresent = {
                        Timber.d("Passphrase not in cache")
                        Output.Unauthorized(AuthenticationState.Unauthenticated.Reason.Passphrase)
                    },
                ) { passphrase ->
                    Timber.d("Using passphrase from cache")
                    val sharePermissions =
                        sharePermissionsModelMapper
                            .mapForShare(
                                SharePermissionsModelMapper.ShareItem.Resource(resourceId),
                                recipients,
                                existingPermissions,
                            )
                    val secretsData =
                        prepareEncryptedSecretsData(
                            passphrase,
                            secretOutput.decryptedSecret,
                            newUsers,
                            recipientsPublicKeys,
                        )
                    if (secretsData.any { it is EncryptedSecretOrError.Error }) {
                        return Output.SecretEncryptFailure(
                            secretsData.filterIsInstance<EncryptedSecretOrError.Error>().first().message,
                        )
                    }
                    val secrets = secretsData.filterIsInstance<EncryptedSecretOrError.EncryptedSecret>()
                    Timber.d("Executing share request")
                    when (
                        val shareOutput =
                            shareResourceUseCase.execute(
                                ShareResourceUseCase.Input(resourceId, sharePermissions, secrets),
                            )
                    ) {
                        is ShareResourceUseCase.Output.Failure -> {
                            Timber.e("Share resource failure: %s", shareOutput.message)
                            Output.ShareFailure(shareOutput.incomplete)
                        }
                        is ShareResourceUseCase.Output.Success -> {
                            Timber.d("Share request success")
                            Output.Success
                        }
                    }
                }
            }
        }
    }

    private suspend fun prepareEncryptedSecretsData(
        passphrase: ByteArray,
        decryptedSecret: String,
        addedUsers: List<ShareRecipient>,
        recipientsPublicKeys: Map<String, String>,
    ): List<EncryptedSecretOrError> {
        val encryptedSecretsForAddedUsers = mutableListOf<EncryptedSecretOrError>()
        addedUsers
            .forEach { recipient ->
                val currentUserId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
                val privateKey =
                    requireNotNull(privateKeyRepository.getPrivateKey(currentUserId)) {
                        "Unable to restore private key."
                    }.armoredKey
                val publicKey = recipientPublicKey(recipient.userId, recipientsPublicKeys)
                if (publicKey == null) {
                    Timber.e("Public key not available for one of the recipients")
                    encryptedSecretsForAddedUsers.add(
                        EncryptedSecretOrError.Error("Public key not available for one of the recipients"),
                    )
                    return@forEach
                }

                val encryptedSecret =
                    openPgp.encryptSignMessageArmored(
                        publicKey,
                        privateKey,
                        passphrase,
                        decryptedSecret,
                    )

                encryptedSecretsForAddedUsers.add(
                    when (encryptedSecret) {
                        is OpenPgpResult.Error -> EncryptedSecretOrError.Error(encryptedSecret.error.message)
                        is OpenPgpResult.Result ->
                            EncryptedSecretOrError.EncryptedSecret(
                                recipient.userId,
                                encryptedSecret.result,
                            )
                    },
                )
            }
        return encryptedSecretsForAddedUsers
    }

    private suspend fun recipientPublicKey(
        userId: String,
        recipientsPublicKeys: Map<String, String>,
    ): String? =
        if (recipientsPublicKeys.isNotEmpty()) {
            recipientsPublicKeys[userId]
        } else {
            try {
                getLocalUserUseCase
                    .execute(GetLocalUserUseCase.Input(userId))
                    .user.gpgKey.armoredKey
            } catch (exception: NullPointerException) {
                Timber.e(exception, "Recipient user not found in the local storage")
                null
            }
        }

    sealed class Output : AuthenticatedUseCaseOutput {
        override val authenticationState: AuthenticationState
            get() =
                when (this) {
                    is SecretFetchFailure if this.incomplete is DomainResult.Incomplete.Unauthorized ->
                        AuthenticationState.Unauthenticated(AuthenticationState.Unauthenticated.Reason.Session)
                    is ShareFailure if this.incomplete is DomainResult.Incomplete.Unauthorized ->
                        AuthenticationState.Unauthenticated(AuthenticationState.Unauthenticated.Reason.Session)
                    is SimulateShareFailure if this.incomplete is DomainResult.Incomplete.Unauthorized ->
                        AuthenticationState.Unauthenticated(AuthenticationState.Unauthenticated.Reason.Session)
                    is Unauthorized -> AuthenticationState.Unauthenticated(this.reason)
                    else -> AuthenticationState.Authenticated
                }

        data class SecretFetchFailure(
            val incomplete: DomainResult.Incomplete,
        ) : Output()

        data class SecretDecryptFailure(
            val message: String,
        ) : Output()

        data class SecretEncryptFailure(
            val message: String,
        ) : Output()

        data class ShareFailure(
            val incomplete: DomainResult.Incomplete,
        ) : Output() {
            val message: String?
                get() = incomplete.displayMessage()
        }

        data class SimulateShareFailure(
            val incomplete: DomainResult.Incomplete,
        ) : Output() {
            val message: String?
                get() = incomplete.displayMessage()
        }

        class Unauthorized(
            val reason: UnauthenticatedReason,
        ) : Output()

        data object Success : Output()

        data object DriftDetected : Output()
    }
}
