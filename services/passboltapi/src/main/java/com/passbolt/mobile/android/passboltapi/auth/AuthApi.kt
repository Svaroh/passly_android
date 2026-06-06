package net.svaroh.passly.passboltapi.auth

import net.svaroh.passly.core.networking.AuthPaths.AUTH_JWT_REFRESH
import net.svaroh.passly.core.networking.AuthPaths.AUTH_RSA
import net.svaroh.passly.core.networking.AuthPaths.AUTH_SIGN_IN
import net.svaroh.passly.core.networking.AuthPaths.AUTH_SIGN_OUT
import net.svaroh.passly.core.networking.AuthPaths.AUTH_VERIFY
import net.svaroh.passly.dto.request.RefreshSessionRequest
import net.svaroh.passly.dto.request.SignInRequestDto
import net.svaroh.passly.dto.request.SignOutRequestDto
import net.svaroh.passly.dto.response.BaseResponse
import net.svaroh.passly.dto.response.RefreshSessionResponse
import net.svaroh.passly.dto.response.ServerPgpResponseDto
import net.svaroh.passly.dto.response.ServerRsaResponseDto
import net.svaroh.passly.dto.response.SignInResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

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
internal interface AuthApi {
    @GET(AUTH_VERIFY)
    suspend fun getServerPublicPgpKey(): BaseResponse<ServerPgpResponseDto>

    @GET(AUTH_RSA)
    suspend fun getServerPublicRsaKey(): BaseResponse<ServerRsaResponseDto>

    @POST(AUTH_SIGN_IN)
    suspend fun signIn(
        @Body signInRequestDto: SignInRequestDto,
        @Header("Cookie") authHeader: String?,
    ): Response<BaseResponse<SignInResponseDto>>

    @POST(AUTH_SIGN_OUT)
    suspend fun signOut(
        @Body signOutRequestDto: SignOutRequestDto,
    ): BaseResponse<Unit>

    @POST(AUTH_JWT_REFRESH)
    suspend fun refreshSession(
        @Body refreshSessionRequest: RefreshSessionRequest,
    ): Response<BaseResponse<RefreshSessionResponse>>
}
