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

package net.svaroh.passly.domain.resources.actions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import net.svaroh.passly.domain.favourites.FavouritesInteractor
import net.svaroh.passly.domain.resources.usecase.DeleteResourceUseCase
import net.svaroh.passly.domain.resources.usecase.db.UpdateLocalResourceUseCase
import net.svaroh.passly.feature.authentication.session.runAuthenticatedOperation
import net.svaroh.passly.ui.ResourceMoreMenuModel.FavouriteOption
import net.svaroh.passly.ui.ResourceMoreMenuModel.FavouriteOption.ADD_TO_FAVOURITES
import net.svaroh.passly.ui.ResourceMoreMenuModel.FavouriteOption.REMOVE_FROM_FAVOURITES
import net.svaroh.passly.ui.ResourceUiModel
import timber.log.Timber

class ResourceCommonActionsInteractor(
    private val resource: ResourceUiModel,
    private val favouritesInteractor: FavouritesInteractor,
    private val deleteResourceUseCase: DeleteResourceUseCase,
    private val updateLocalResourceUseCase: UpdateLocalResourceUseCase,
) {
    suspend fun toggleFavourite(favouriteOption: FavouriteOption): Flow<ResourceCommonActionResult> =
        when (favouriteOption) {
            ADD_TO_FAVOURITES ->
                runAuthenticatedOperation {
                    favouritesInteractor.addToFavourites(resource.resourceId)
                }
            REMOVE_FROM_FAVOURITES ->
                runAuthenticatedOperation {
                    favouritesInteractor.removeFromFavourites(resource.favouriteId!!)
                }
        }.let { favouriteToggleOutput ->
            when (favouriteToggleOutput) {
                is FavouritesInteractor.Output.Failure -> {
                    flowOf(ResourceCommonActionResult.Failure)
                }
                is FavouritesInteractor.Output.Success -> {
                    updateLocalResourceUseCase.execute(
                        UpdateLocalResourceUseCase.Input(
                            resource.copy(favouriteId = favouriteToggleOutput.favouriteId),
                        ),
                    )
                    Timber.d("Added to favourites")
                    flowOf(ResourceCommonActionResult.Success(resource.metadataJsonModel.name))
                }
            }
        }

    suspend fun deleteResource(): Flow<ResourceCommonActionResult> =
        when (
            runAuthenticatedOperation {
                deleteResourceUseCase.execute(DeleteResourceUseCase.Input(resource.resourceId))
            }
        ) {
            is DeleteResourceUseCase.Output.Success -> {
                flowOf(ResourceCommonActionResult.Success(resource.metadataJsonModel.name))
            }
            is DeleteResourceUseCase.Output.Failure -> {
                Timber.e("Failed to delete resource")
                flowOf(ResourceCommonActionResult.Failure)
            }
        }
}

suspend fun performCommonResourceAction(
    action: suspend () -> Flow<ResourceCommonActionResult>,
    doOnSuccess: (ResourceCommonActionResult.Success) -> Unit,
    doOnFailure: () -> Unit,
) {
    when (val result = action().single()) {
        is ResourceCommonActionResult.Failure -> doOnFailure()
        is ResourceCommonActionResult.Success -> doOnSuccess(result)
    }
}
