package com.passbolt.mobile.android.feature.authentication.auth.challenge

import com.google.gson.Gson
import com.passbolt.mobile.android.common.UuidProvider
import com.passbolt.mobile.android.common.extension.erase
import com.passbolt.mobile.android.common.time.TimeProvider
import com.passbolt.mobile.android.domain.privatekey.PrivateKeyRepository
import com.passbolt.mobile.android.dto.request.ChallengeDto
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult

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
class ChallengeProvider(
    private val gson: Gson,
    private val openPgp: OpenPgp,
    private val privateKeyRepository: PrivateKeyRepository,
    private val timeProvider: TimeProvider,
    private val uuidProvider: UuidProvider,
) {
    suspend fun get(
        domain: String,
        serverPublicKey: String,
        passphrase: ByteArray,
        userId: String,
    ): Output {
        val passphraseCopy = passphrase.copyOf()
        try {
            val privateKey =
                requireNotNull(privateKeyRepository.getPrivateKey(userId)) { "Unable to restore private key." }.armoredKey
            val tokenExpiry = getVerifyTokenExpiry()
            val verifyToken = uuidProvider.get()

            val challengeJson =
                ChallengeDto(CHALLENGE_VERSION, domain, verifyToken, tokenExpiry)
                    .run { gson.toJson(this) }

            return when (
                val encryptedChallenge =
                    openPgp.encryptSignMessageArmored(
                        publicKey = serverPublicKey,
                        privateKey = privateKey,
                        passphrase = passphraseCopy,
                        message = challengeJson,
                    )
            ) {
                is OpenPgpResult.Result -> Output.Success(encryptedChallenge.result, verifyToken)
                is OpenPgpResult.Error -> Output.WrongPassphrase
            }
        } finally {
            passphraseCopy.erase()
        }
    }

    private fun getVerifyTokenExpiry() = timeProvider.getCurrentEpochSeconds() + TOKEN_VALIDATION_TIME

    sealed class Output {
        data class Success(
            val challenge: String,
            val verifyToken: String,
        ) : Output()

        data object WrongPassphrase : Output()
    }

    companion object {
        private const val TOKEN_VALIDATION_TIME = 120
        private const val CHALLENGE_VERSION = "1.0.0"
    }
}
