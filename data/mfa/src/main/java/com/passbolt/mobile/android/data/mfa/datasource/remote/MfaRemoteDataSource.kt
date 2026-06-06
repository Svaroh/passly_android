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

package net.svaroh.passly.data.mfa.datasource.remote

import net.svaroh.passly.common.CookieExtractor
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.architecture.result.map
import net.svaroh.passly.core.networking.ErrorHeaderMapper
import net.svaroh.passly.core.networking.ResponseHandler
import net.svaroh.passly.core.networking.callWithHandler
import net.svaroh.passly.core.networking.toDomainResult
import net.svaroh.passly.data.mfa.datasource.remote.api.MfaApi
import net.svaroh.passly.domain.mfa.MfaDataSource
import net.svaroh.passly.domain.mfa.model.DuoPrompt
import net.svaroh.passly.domain.mfa.model.DuoVerification
import net.svaroh.passly.domain.mfa.model.TotpVerification
import net.svaroh.passly.domain.mfa.model.YubikeyVerification
import net.svaroh.passly.dto.request.HotpRequest
import net.svaroh.passly.dto.request.TotpRequest
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_MOVED_TEMP
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED

internal class MfaRemoteDataSource(
    private val mfaApi: MfaApi,
    private val responseHandler: ResponseHandler,
    private val cookieExtractor: CookieExtractor,
    private val errorHeaderMapper: ErrorHeaderMapper,
) : MfaDataSource {
    override suspend fun verifyTotp(
        otp: String,
        remember: Boolean,
        authToken: String?,
    ): DomainResult<TotpVerification> =
        callWithHandler(responseHandler) {
            mfaApi.verifyTotp(TotpRequest(otp, remember), authToken.toBearer())
        }.toDomainResult().map { it.toTotpVerification() }

    override suspend fun verifyYubikeyOtp(
        otp: String,
        remember: Boolean,
        authToken: String?,
    ): DomainResult<YubikeyVerification> =
        callWithHandler(responseHandler) {
            mfaApi.verifyYubikeyOtp(HotpRequest(otp, remember), authToken.toBearer())
        }.toDomainResult().map { it.toYubikeyVerification() }

    override suspend fun getDuoPrompt(authToken: String?): DomainResult<DuoPrompt> =
        callWithHandler(responseHandler) {
            mfaApi.getDuoPromptUrl(authToken.toBearer())
        }.toDomainResult().map { it.toDuoPrompt() }

    override suspend fun verifyDuoCallback(
        authToken: String?,
        duoStateUuid: String,
        state: String?,
        code: String?,
    ): DomainResult<DuoVerification> =
        callWithHandler(responseHandler) {
            mfaApi.verifyDuoCallback(
                authHeader = authToken.toBearer(),
                passboltDuoState = DUO_STATE_COOKIE_TEMPLATE.format(duoStateUuid),
                state = state,
                code = code,
            )
        }.toDomainResult().map { it.toDuoVerification() }

    private fun Response<Void>.toTotpVerification(): TotpVerification =
        when {
            isSuccessful -> TotpVerification.Succeeded(mfaHeader())
            code() == HTTP_UNAUTHORIZED -> TotpVerification.Unauthorized
            errorHeaderMapper.getValidationFieldsError(errorBody())?.contains(VALID_OTP) == true -> TotpVerification.WrongOtp
            else -> TotpVerification.OtherFailure(code())
        }

    private fun Response<Void>.toYubikeyVerification(): YubikeyVerification =
        when {
            isSuccessful -> YubikeyVerification.Succeeded(mfaHeader())
            code() == HTTP_UNAUTHORIZED -> YubikeyVerification.Unauthorized
            code() == HTTP_BAD_REQUEST && isYubikeyFromAnotherUser() -> YubikeyVerification.NotFromCurrentUser
            else -> YubikeyVerification.OtherFailure(code())
        }

    private fun Response<Void>.toDuoPrompt(): DuoPrompt =
        when (code()) {
            HTTP_MOVED_TEMP -> {
                val location = headers()[LOCATION_HEADER]
                val duoUuid = cookieExtractor.getCookieValue(this, DUO_UUID_COOKIE)
                if (location != null && duoUuid != null) {
                    DuoPrompt.Found(location, duoUuid)
                } else {
                    DuoPrompt.NotFound
                }
            }
            HTTP_UNAUTHORIZED -> DuoPrompt.Unauthorized
            else -> DuoPrompt.OtherFailure(code())
        }

    private fun Response<Void>.toDuoVerification(): DuoVerification =
        if (isSuccessful) {
            DuoVerification.Succeeded(mfaHeader())
        } else {
            DuoVerification.Failed(message())
        }

    private fun Response<Void>.mfaHeader(): String? = cookieExtractor.get(this, CookieExtractor.MFA_COOKIE)

    // checks if field "isSameYubikeyId" exists in the response - that means that it's not a Yubikey
    // that is associated with the account
    private fun Response<Void>.isYubikeyFromAnotherUser(): Boolean =
        try {
            errorBody()?.string()?.let { body ->
                JSONObject(body)
                    .getJSONObject(RESPONSE_BODY)
                    .getJSONObject(RESPONSE_BODY_HOTP)
                    .has(HOTP_BODY_IS_SAME_YUBIKEY)
            } ?: false
        } catch (exception: JSONException) {
            false
        }

    private fun String?.toBearer(): String? = this?.let { "Bearer $it" }

    private companion object {
        private const val VALID_OTP = "isValidOtp"
        private const val LOCATION_HEADER = "location"
        private const val DUO_UUID_COOKIE = "passbolt_duo_state"
        private const val DUO_STATE_COOKIE_TEMPLATE = "passbolt_duo_state=%s"
        private const val RESPONSE_BODY = "body"
        private const val RESPONSE_BODY_HOTP = "hotp"
        private const val HOTP_BODY_IS_SAME_YUBIKEY = "isSameYubikeyId"
    }
}
