package net.svaroh.passly.feature.authentication.auth.challenge

import com.google.gson.Gson
import net.svaroh.passly.common.extension.erase
import net.svaroh.passly.domain.privatekey.PrivateKeyRepository
import net.svaroh.passly.dto.response.ChallengeResponseDto
import net.svaroh.passly.gopenpgp.OpenPgp
import net.svaroh.passly.gopenpgp.exception.OpenPgpFailure
import net.svaroh.passly.gopenpgp.exception.OpenPgpResult

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
class ChallengeDecryptor(
    private val openPgp: OpenPgp,
    private val privateKeyRepository: PrivateKeyRepository,
    private val gson: Gson,
) {
    suspend fun decrypt(
        serverPublicKey: String,
        passphrase: ByteArray,
        userId: String,
        challenge: String,
    ): Output {
        val passphraseCopy = passphrase.copyOf()
        try {
            val privateKey =
                requireNotNull(privateKeyRepository.getPrivateKey(userId)) { "Unable to restore private key." }.armoredKey
            return when (
                val decryptedChallenge =
                    openPgp.decryptVerifyMessageArmored(
                        publicKey = serverPublicKey,
                        privateKey = privateKey,
                        passphrase = passphraseCopy,
                        cipherText = challenge,
                    )
            ) {
                is OpenPgpResult.Error ->
                    when (val failure = decryptedChallenge.error) {
                        is OpenPgpFailure.SignatureVerificationFailed -> Output.ServerSignatureInvalid(failure.message)
                        is OpenPgpFailure.Generic -> Output.DecryptionError(failure.message)
                    }
                is OpenPgpResult.Result ->
                    Output.DecryptedChallenge(
                        gson.fromJson(
                            decryptedChallenge.result,
                            ChallengeResponseDto::class.java,
                        ),
                    )
            }
        } finally {
            passphraseCopy.erase()
        }
    }

    sealed class Output {
        data class DecryptedChallenge(
            val challenge: ChallengeResponseDto,
        ) : Output()

        data class DecryptionError(
            val message: String?,
        ) : Output()

        data class ServerSignatureInvalid(
            val message: String?,
        ) : Output()
    }
}
