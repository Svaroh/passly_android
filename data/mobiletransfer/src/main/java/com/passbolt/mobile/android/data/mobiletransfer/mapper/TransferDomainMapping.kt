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

package net.svaroh.passly.data.mobiletransfer.mapper

import net.svaroh.passly.domain.mobiletransfer.model.CreateTransferModel
import net.svaroh.passly.domain.mobiletransfer.model.TransferModel
import net.svaroh.passly.domain.mobiletransfer.model.UpdateTransferModel
import net.svaroh.passly.dto.request.CreateTransferRequestDto
import net.svaroh.passly.dto.request.StatusRequest
import net.svaroh.passly.dto.request.UpdateTransferRequestDto
import net.svaroh.passly.dto.response.CreateTransferResponseDto
import net.svaroh.passly.dto.response.StatusResponse
import net.svaroh.passly.dto.response.TransferResponseDto
import net.svaroh.passly.ui.Status

internal fun TransferResponseDto.toTransferModel(): TransferModel =
    TransferModel(
        id = id.toString(),
        status = status.toDomain(),
        currentPage = currentPage,
        totalPages = totalPages,
        hash = hash,
    )

internal fun TransferResponseDto.toUpdateTransferModel(): UpdateTransferModel {
    val profile = user?.profile
    return UpdateTransferModel(
        id = id.toString(),
        firstName = profile?.firstName,
        lastName = profile?.lastName,
        avatarUrl = profile?.avatar?.url?.medium,
        email = user?.email,
    )
}

internal fun CreateTransferResponseDto.toCreateTransferModel(): CreateTransferModel =
    CreateTransferModel(
        id = id.toString(),
        status = status.toDomain(),
        currentPage = currentPage,
        totalPages = totalPages,
        hash = hash,
        authenticationToken = authToken.token,
    )

internal fun updateTransferRequestDto(
    currentPage: Int,
    status: Status,
): UpdateTransferRequestDto = UpdateTransferRequestDto(currentPage, status.toRequest())

internal fun createTransferRequestDto(
    totalPagesCount: Int,
    hash: String,
): CreateTransferRequestDto = CreateTransferRequestDto(totalPagesCount, hash)

private fun StatusResponse.toDomain(): Status =
    when (this) {
        StatusResponse.ERROR -> Status.ERROR
        StatusResponse.IN_PROGRESS -> Status.IN_PROGRESS
        StatusResponse.COMPLETE -> Status.COMPLETE
        StatusResponse.CANCEL -> Status.CANCEL
        StatusResponse.START -> Status.START
    }

private fun Status.toRequest(): StatusRequest =
    when (this) {
        Status.ERROR -> StatusRequest.ERROR
        Status.IN_PROGRESS -> StatusRequest.IN_PROGRESS
        Status.COMPLETE -> StatusRequest.COMPLETE
        Status.CANCEL -> StatusRequest.CANCEL
        Status.START -> StatusRequest.START
    }
