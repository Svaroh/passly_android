package net.svaroh.passly.data.mobiletransfer.datasource.remote

import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.architecture.result.map
import net.svaroh.passly.core.networking.ResponseHandler
import net.svaroh.passly.core.networking.callWithHandler
import net.svaroh.passly.core.networking.toDomainResult
import net.svaroh.passly.data.mobiletransfer.datasource.remote.api.MobileTransferApi
import net.svaroh.passly.data.mobiletransfer.mapper.createTransferRequestDto
import net.svaroh.passly.data.mobiletransfer.mapper.toCreateTransferModel
import net.svaroh.passly.data.mobiletransfer.mapper.toTransferModel
import net.svaroh.passly.data.mobiletransfer.mapper.toUpdateTransferModel
import net.svaroh.passly.data.mobiletransfer.mapper.updateTransferRequestDto
import net.svaroh.passly.domain.mobiletransfer.MobileTransferDataSource
import net.svaroh.passly.domain.mobiletransfer.model.CreateTransferModel
import net.svaroh.passly.domain.mobiletransfer.model.TransferModel
import net.svaroh.passly.domain.mobiletransfer.model.UpdateTransferModel
import net.svaroh.passly.dto.request.BrowserFirstLoginAccountRequestDto
import net.svaroh.passly.dto.request.BrowserFirstLoginResponseRequestDto
import net.svaroh.passly.dto.response.BrowserFirstLoginRequestResponseDto
import net.svaroh.passly.ui.Status

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
internal class MobileTransferRemoteDataSource(
    private val mobileTransferApi: MobileTransferApi,
    private val responseHandler: ResponseHandler,
) : MobileTransferDataSource {
    override suspend fun updateTransfer(
        uuid: String,
        authToken: String,
        currentPage: Int,
        status: Status,
    ): DomainResult<UpdateTransferModel> =
        callWithHandler(responseHandler) {
            val requestDto = updateTransferRequestDto(currentPage, status)
            val userProfile = if (status == Status.COMPLETE) PROFILE_INFO_REQUIRED else null
            mobileTransferApi.updateTransfer(uuid, authToken, requestDto, userProfile).body
        }.toDomainResult().map { it.toUpdateTransferModel() }

    override suspend fun createTransfer(
        totalPagesCount: Int,
        hash: String,
    ): DomainResult<CreateTransferModel> =
        callWithHandler(responseHandler) {
            mobileTransferApi.createTransfer(createTransferRequestDto(totalPagesCount, hash)).body
        }.toDomainResult().map { it.toCreateTransferModel() }

    override suspend fun viewTransfer(
        authToken: String,
        mfaCookie: String?,
        uuid: String,
    ): DomainResult<TransferModel> =
        callWithHandler(responseHandler) {
            if (mfaCookie != null) {
                mobileTransferApi.viewTransferWithMfa(authToken, mfaCookie, uuid).body
            } else {
                mobileTransferApi.viewTransfer(authToken, uuid).body
            }
        }.toDomainResult().map { it.toTransferModel() }

    override suspend fun setBrowserFirstLoginAccount(
        uuid: String,
        request: BrowserFirstLoginAccountRequestDto,
    ): DomainResult<BrowserFirstLoginRequestResponseDto> =
        callWithHandler(responseHandler) {
            mobileTransferApi.setBrowserFirstLoginAccount(uuid, request).body
        }.toDomainResult()

    override suspend fun setBrowserFirstLoginResponse(
        uuid: String,
        request: BrowserFirstLoginResponseRequestDto,
    ): DomainResult<BrowserFirstLoginRequestResponseDto> =
        callWithHandler(responseHandler) {
            mobileTransferApi.setBrowserFirstLoginResponse(uuid, request).body
        }.toDomainResult()

    private companion object {
        private const val PROFILE_INFO_REQUIRED = "1"
    }
}
