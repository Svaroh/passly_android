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

package net.svaroh.passly.domain.groups.usecase

import androidx.paging.PagingData
import androidx.paging.map
import net.svaroh.passly.common.usecase.AsyncUseCase
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import net.svaroh.passly.domain.groups.GroupsRepository
import net.svaroh.passly.domain.groups.mapper.toUiModel
import net.svaroh.passly.ui.GroupWithCount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetLocalGroupsWithShareItemsCountPaginatedUseCase(
    private val groupsRepository: GroupsRepository,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
) : AsyncUseCase<GetLocalGroupsWithShareItemsCountPaginatedUseCase.Input, GetLocalGroupsWithShareItemsCountPaginatedUseCase.Output> {
    override suspend fun execute(input: Input): Output {
        val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        return Output(
            groupsRepository
                .getGroupsWithItemsCountPaged(input.searchQuery, input.pageSize, userId)
                .map { pagingData -> pagingData.map { it.toUiModel() } },
        )
    }

    data class Input(
        val searchQuery: String? = null,
        val pageSize: Int = DEFAULT_PAGE_SIZE,
    )

    data class Output(
        val pagedGroupsFlow: Flow<PagingData<GroupWithCount>>,
    )

    private companion object {
        private const val DEFAULT_PAGE_SIZE = 20
    }
}
