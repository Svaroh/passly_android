package net.svaroh.passly.feature.authentication.auth.challenge

import io.fusionauth.jwt.InvalidJWTSignatureException
import io.fusionauth.jwt.JWTExpiredException
import io.fusionauth.jwt.Verifier
import io.fusionauth.jwt.domain.JWT
import io.fusionauth.jwt.rsa.RSAVerifier
import net.svaroh.passly.dto.response.ChallengeResponseDto
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
class ChallengeVerifier(
    private val domainComparator: DomainComparator,
) {
    fun verify(
        challengeResponseDto: ChallengeResponseDto,
        rsaPublicKey: String,
        sentVerifyToken: String,
        sentDomain: String,
    ): Output {
        if (challengeResponseDto.verifyToken != sentVerifyToken) {
            Timber.e("Challenge response verify token does not match the one sent in the challenge")
            return Output.VerifyTokenMismatch
        }
        if (!domainComparator.matches(challengeResponseDto.domain, sentDomain)) {
            Timber.e("Challenge response domain does not match the one sent in the challenge")
            return Output.DomainMismatch
        }
        return try {
            val verifier: Verifier = RSAVerifier.newVerifier(rsaPublicKey)
            JWT.getDecoder().decode(challengeResponseDto.accessToken, verifier)

            Output.Verified(
                challengeResponseDto.accessToken,
                challengeResponseDto.refreshToken,
            )
        } catch (exception: InvalidJWTSignatureException) {
            Timber.e(exception)
            Output.InvalidSignature
        } catch (exception: JWTExpiredException) {
            Timber.e(exception)
            Output.TokenExpired
        } catch (exception: Exception) {
            Timber.e(exception)
            Output.Failure
        }
    }

    sealed class Output {
        data object TokenExpired : Output()

        data class Verified(
            val accessToken: String,
            val refreshToken: String,
        ) : Output()

        data object InvalidSignature : Output()

        data object VerifyTokenMismatch : Output()

        data object DomainMismatch : Output()

        data object Failure : Output()
    }
}
