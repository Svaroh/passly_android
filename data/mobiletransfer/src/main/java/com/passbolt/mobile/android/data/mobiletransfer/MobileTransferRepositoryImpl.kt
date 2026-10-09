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

package net.svaroh.passly.data.mobiletransfer

import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.domain.mobiletransfer.MobileTransferDataSource
import net.svaroh.passly.domain.mobiletransfer.MobileTransferRepository
import net.svaroh.passly.domain.mobiletransfer.model.CreateTransferModel
import net.svaroh.passly.domain.mobiletransfer.model.TransferModel
import net.svaroh.passly.domain.mobiletransfer.model.UpdateTransferModel
import net.svaroh.passly.dto.request.BrowserFirstLoginAccountRequestDto
import net.svaroh.passly.dto.request.BrowserFirstLoginResponseRequestDto
import net.svaroh.passly.dto.response.BrowserFirstLoginRequestResponseDto
import net.svaroh.passly.ui.Status

internal class MobileTransferRepositoryImpl(
    private val remoteDataSource: MobileTransferDataSource,
) : MobileTransferRepository {
    override suspend fun turnPage(
        uuid: String,
        authToken: String,
        currentPage: Int,
        status: Status,
    ): DomainResult<UpdateTransferModel> = remoteDataSource.updateTransfer(uuid, authToken, currentPage, status)

    override suspend fun createTransfer(
        totalPagesCount: Int,
        hash: String,
    ): DomainResult<CreateTransferModel> = remoteDataSource.createTransfer(totalPagesCount, hash)

    override suspend fun viewTransfer(
        authToken: String,
        mfaCookie: String?,
        uuid: String,
    ): DomainResult<TransferModel> = remoteDataSource.viewTransfer(authToken, mfaCookie, uuid)

    override suspend fun setBrowserFirstLoginAccount(
        uuid: String,
        request: BrowserFirstLoginAccountRequestDto,
    ): DomainResult<BrowserFirstLoginRequestResponseDto> = remoteDataSource.setBrowserFirstLoginAccount(uuid, request)

    override suspend fun setBrowserFirstLoginResponse(
        uuid: String,
        request: BrowserFirstLoginResponseRequestDto,
    ): DomainResult<BrowserFirstLoginRequestResponseDto> = remoteDataSource.setBrowserFirstLoginResponse(uuid, request)
}
