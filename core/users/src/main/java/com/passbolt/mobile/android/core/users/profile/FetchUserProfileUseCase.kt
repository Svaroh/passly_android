package net.svaroh.passly.core.users.profile

import net.svaroh.passly.common.usecase.AsyncUseCase
import net.svaroh.passly.common.usecase.UserIdInput
import net.svaroh.passly.core.networking.NetworkResult
import net.svaroh.passly.mappers.UserProfileMapper
import net.svaroh.passly.passboltapi.users.UsersRepository
import net.svaroh.passly.ui.UserProfileModel
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
class FetchUserProfileUseCase(
    private val usersRepository: UsersRepository,
    private val userProfileMapper: UserProfileMapper,
) : AsyncUseCase<UserIdInput, FetchUserProfileUseCase.Output> {
    override suspend fun execute(input: UserIdInput): Output =
        when (val result = usersRepository.getMyProfile()) {
            is NetworkResult.Failure.NetworkError -> Output.Failure(result.exception.message)
            is NetworkResult.Failure.ServerError -> Output.Failure(result.headerMessage)
            is NetworkResult.Success -> {
                val profile = userProfileMapper.mapToUi(result.value.profile, result.value.username)
                if (profile != null) {
                    Output.Success(profile, result.value.role?.name)
                } else {
                    Timber.e("User profile was not present in the API response")
                    Output.Failure("User profile not present in the response")
                }
            }
        }

    sealed class Output {
        data class Success(
            val profile: UserProfileModel,
            val roleName: String?,
        ) : Output()

        data class Failure(
            val message: String?,
        ) : Output()
    }
}
