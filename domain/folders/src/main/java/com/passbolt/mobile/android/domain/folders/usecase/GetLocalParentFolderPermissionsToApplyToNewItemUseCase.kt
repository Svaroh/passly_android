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

import net.svaroh.passly.common.usecase.AsyncUseCase
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountDataUseCase
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import net.svaroh.passly.domain.folders.FoldersRepository
import net.svaroh.passly.domain.folders.model.ParentPermissionItemId
import net.svaroh.passly.ui.PermissionModelUi

typealias ItemIdResourceId = ParentPermissionItemId.ResourceId
typealias ItemIdFolderId = ParentPermissionItemId.FolderId

class GetLocalParentFolderPermissionsToApplyToNewItemUseCase(
    private val foldersRepository: FoldersRepository,
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase,
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase,
) : AsyncUseCase<
        GetLocalParentFolderPermissionsToApplyToNewItemUseCase.Input,
        GetLocalParentFolderPermissionsToApplyToNewItemUseCase.Output,
    > {
    /**
     * Gets folder permissions which are to be copied and applied to a newly created resource or folder in that folder.
     *
     * Permission IDs need to be replaced to a constant SharePermissionsModelMapper.TEMPORARY_NEW_PERMISSION_ID
     */
    override suspend fun execute(input: Input): Output {
        val currentUserServerId = requireNotNull(getSelectedAccountDataUseCase.execute(Unit).serverId)
        val userId = requireNotNull(getSelectedAccountUseCase.execute(Unit).selectedAccount)
        return Output(
            foldersRepository.getParentFolderPermissionsToApplyToNewItem(
                input.parentFolderId,
                input.itemId,
                currentUserServerId,
                userId,
            ),
        )
    }

    data class Input(
        val parentFolderId: String,
        val itemId: ParentPermissionItemId,
    )

    data class Output(
        val permissions: List<PermissionModelUi>,
    )
}
