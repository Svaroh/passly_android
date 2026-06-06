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

package net.svaroh.passly.domain.favourites

import net.svaroh.passly.core.mvp.authentication.AuthenticatedUseCaseOutput
import net.svaroh.passly.core.mvp.authentication.AuthenticationState
import net.svaroh.passly.core.mvp.authentication.CompleteAuthenticatedOutput
import net.svaroh.passly.domain.favourites.usecase.AddToFavouritesUseCase
import net.svaroh.passly.domain.favourites.usecase.RemoveFromFavouritesUseCase
import timber.log.Timber

class FavouritesInteractor(
    private val addToFavouritesUseCase: AddToFavouritesUseCase,
    private val removeFromFavouritesUseCase: RemoveFromFavouritesUseCase,
) {
    suspend fun addToFavourites(resourceId: String): Output {
        val addToFavouritesResult =
            addToFavouritesUseCase.execute(
                AddToFavouritesUseCase.Input(resourceId),
            )

        return when (addToFavouritesResult) {
            is AddToFavouritesUseCase.Output.Failure -> {
                Timber.e("Error when adding to favourites. Failure: %s", addToFavouritesResult.incomplete)
                Output.Failure(addToFavouritesResult.authenticationState)
            }
            is AddToFavouritesUseCase.Output.Success ->
                Output.Success(favouriteId = addToFavouritesResult.favouriteId)
        }
    }

    suspend fun removeFromFavourites(favouriteId: String): Output {
        val removeFromFavouritesResult =
            removeFromFavouritesUseCase.execute(
                RemoveFromFavouritesUseCase.Input(favouriteId),
            )

        return when (removeFromFavouritesResult) {
            is RemoveFromFavouritesUseCase.Output.Failure -> {
                Timber.e("Error when removing from favourites. Failure: %s", removeFromFavouritesResult.incomplete)
                Output.Failure(removeFromFavouritesResult.authenticationState)
            }
            is RemoveFromFavouritesUseCase.Output.Success ->
                Output.Success(favouriteId = null)
        }
    }

    sealed class Output : AuthenticatedUseCaseOutput {
        data class Success(
            val favouriteId: String?,
        ) : Output(),
            CompleteAuthenticatedOutput

        class Failure(
            override val authenticationState: AuthenticationState,
        ) : Output()
    }
}
