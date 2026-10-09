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

package net.svaroh.passly.domain.folders.usecase

import android.database.SQLException
import net.svaroh.passly.common.transaction.DatabaseTransactionRunner
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.mvp.authentication.AuthenticatedUseCaseOutput
import net.svaroh.passly.core.mvp.authentication.CompleteAuthenticatedOutput
import net.svaroh.passly.core.mvp.authentication.IncompleteAuthenticatedOutput
import net.svaroh.passly.domain.folders.model.FolderModelWithAttributes
import net.svaroh.passly.domain.folders.model.FolderUpdateState.PENDING
import net.svaroh.passly.domain.folders.usecase.GetFoldersPaginatedUseCase.Output.Failure
import net.svaroh.passly.domain.folders.usecase.GetFoldersPaginatedUseCase.Output.Success
import net.svaroh.passly.domain.preferences.GlobalPreferencesRepository
import net.svaroh.passly.featureflags.usecase.GetFeatureFlagsUseCase
import timber.log.Timber
import kotlin.math.ceil

class FoldersInteractor(
    private val getFeatureFlagsUseCase: GetFeatureFlagsUseCase,
    private val getFoldersPaginatedUseCase: GetFoldersPaginatedUseCase,
    private val setLocalFoldersUpdateStateUseCase: SetLocalFoldersUpdateStateUseCase,
    private val upsertLocalFoldersUseCase: UpsertLocalFoldersUseCase,
    private val removeLocalFoldersWithUpdateStateUseCase: RemoveLocalFoldersWithUpdateStateUseCase,
    private val removeLocalFolderPermissionsUseCase: RemoveLocalFolderPermissionsUseCase,
    private val addLocalFolderPermissionsUseCase: AddLocalFolderPermissionsUseCase,
    private val updateLocalFoldersIsSharedUseCase: UpdateLocalFoldersIsSharedUseCase,
    private val globalPreferencesRepository: GlobalPreferencesRepository,
    private val databaseTransactionRunner: DatabaseTransactionRunner,
) {
    @Suppress("ReturnCount")
    suspend fun fetchAndSaveFolders(onPageProcessed: suspend (processedPages: Int, totalPages: Int) -> Unit = { _, _ -> }): Output {
        if (!getFeatureFlagsUseCase.execute(Unit).featureFlags.areFoldersAvailable) {
            return Output.Success
        }

        try {
            val pageSize = globalPreferencesRepository.getGlobalPreferences().apiFetchPageSize
            markAllLocalFoldersAsPending()
            clearLocalFolderPermissions()
            fetchAndProcessAllPages(pageSize, onPageProcessed)?.let { failure -> return failure }
            removeStaleLocalFolders()
            updateFoldersIsShared()
            return Output.Success
        } catch (exception: SQLException) {
            Timber.e(exception)
            return Output.Failure(
                DomainResult.Incomplete.Error(DomainResult.Incomplete.Error.Reason.UNKNOWN, exception.message),
            )
        }
    }

    private suspend fun markAllLocalFoldersAsPending() {
        setLocalFoldersUpdateStateUseCase.execute(
            SetLocalFoldersUpdateStateUseCase.Input(PENDING),
        )
    }

    private suspend fun clearLocalFolderPermissions() {
        removeLocalFolderPermissionsUseCase.execute(Unit)
    }

    private suspend fun fetchAndProcessAllPages(
        pageSize: Int,
        onPageProcessed: suspend (processedPages: Int, totalPages: Int) -> Unit,
    ): Output.Failure? {
        when (val firstPageResult = fetchFoldersPage(FIRST_PAGE, pageSize)) {
            is Failure -> return Output.Failure(firstPageResult.incomplete)
            is Success -> {
                processFolders(firstPageResult.folders)

                val totalPages = ceil(firstPageResult.totalCount.toDouble() / pageSize).toInt()
                onPageProcessed(FIRST_PAGE, totalPages)
                for (page in SECOND_PAGE..totalPages) {
                    when (val pageResult = fetchFoldersPage(page, pageSize)) {
                        is Failure -> return Output.Failure(pageResult.incomplete)
                        is Success -> {
                            processFolders(pageResult.folders)
                            onPageProcessed(page, totalPages)
                        }
                    }
                }
            }
        }
        return null
    }

    private suspend fun fetchFoldersPage(
        page: Int,
        pageSize: Int,
    ) = getFoldersPaginatedUseCase.execute(
        GetFoldersPaginatedUseCase.Input(page = page, limit = pageSize),
    )

    private suspend fun processFolders(foldersWithAttributes: List<FolderModelWithAttributes>) {
        databaseTransactionRunner.runInTransaction {
            upsertLocalFoldersUseCase.execute(
                UpsertLocalFoldersUseCase.Input(foldersWithAttributes.map { it.folderModel }),
            )
            addLocalFolderPermissionsUseCase.execute(
                AddLocalFolderPermissionsUseCase.Input(foldersWithAttributes),
            )
        }
    }

    private suspend fun updateFoldersIsShared() {
        updateLocalFoldersIsSharedUseCase.execute(Unit)
    }

    private suspend fun removeStaleLocalFolders() {
        removeLocalFoldersWithUpdateStateUseCase.execute(
            RemoveLocalFoldersWithUpdateStateUseCase.Input(PENDING),
        )
    }

    sealed class Output : AuthenticatedUseCaseOutput {
        data object Success : Output(), CompleteAuthenticatedOutput

        data class Failure(
            override val incomplete: DomainResult.Incomplete,
        ) : Output(),
            IncompleteAuthenticatedOutput
    }

    private companion object {
        private const val FIRST_PAGE = 1
        private const val SECOND_PAGE = 2
    }
}
