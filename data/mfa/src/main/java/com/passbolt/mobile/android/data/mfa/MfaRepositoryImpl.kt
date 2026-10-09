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

package net.svaroh.passly.data.mfa

import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.domain.mfa.MfaDataSource
import net.svaroh.passly.domain.mfa.MfaRepository
import net.svaroh.passly.domain.mfa.model.DuoPrompt
import net.svaroh.passly.domain.mfa.model.DuoVerification
import net.svaroh.passly.domain.mfa.model.TotpVerification
import net.svaroh.passly.domain.mfa.model.YubikeyVerification

internal class MfaRepositoryImpl(
    private val remoteDataSource: MfaDataSource,
) : MfaRepository {
    override suspend fun verifyTotp(
        otp: String,
        remember: Boolean,
        authToken: String?,
    ): DomainResult<TotpVerification> = remoteDataSource.verifyTotp(otp, remember, authToken)

    override suspend fun verifyYubikeyOtp(
        otp: String,
        remember: Boolean,
        authToken: String?,
    ): DomainResult<YubikeyVerification> = remoteDataSource.verifyYubikeyOtp(otp, remember, authToken)

    override suspend fun getDuoPrompt(authToken: String?): DomainResult<DuoPrompt> = remoteDataSource.getDuoPrompt(authToken)

    override suspend fun verifyDuoCallback(
        authToken: String?,
        duoStateUuid: String,
        state: String?,
        code: String?,
    ): DomainResult<DuoVerification> = remoteDataSource.verifyDuoCallback(authToken, duoStateUuid, state, code)
}
