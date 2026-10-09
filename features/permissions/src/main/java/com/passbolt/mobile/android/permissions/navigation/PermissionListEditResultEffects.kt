package net.svaroh.passly.permissions.navigation

import androidx.compose.runtime.Composable
import net.svaroh.passly.core.navigation.compose.results.NavigationResultEventBus
import net.svaroh.passly.core.navigation.compose.results.ResultEffect
import net.svaroh.passly.ui.PermissionModelUi
import net.svaroh.passly.ui.PermissionModelUi.GroupPermissionModel
import net.svaroh.passly.ui.PermissionModelUi.UserPermissionModel

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

@Composable
internal fun PermissionListEditResultEffects(
    onModifyUserPermission: (UserPermissionModel) -> Unit,
    onDeleteUserPermission: (UserPermissionModel) -> Unit,
    onModifyGroupPermission: (GroupPermissionModel) -> Unit,
    onDeleteGroupPermission: (GroupPermissionModel) -> Unit,
    onAddShareRecipients: (List<PermissionModelUi>?) -> Unit,
) {
    val resultBus = NavigationResultEventBus.current

    ResultEffect<GroupPermissionModifiedResult>(resultBus) { result ->
        onModifyGroupPermission(result.permission)
    }
    ResultEffect<GroupPermissionDeletedResult>(resultBus) { result ->
        onDeleteGroupPermission(result.permission)
    }
    ResultEffect<UserPermissionModifiedResult>(resultBus) { result ->
        onModifyUserPermission(result.permission)
    }
    ResultEffect<UserPermissionDeletedResult>(resultBus) { result ->
        onDeleteUserPermission(result.permission)
    }
    ResultEffect<ShareRecipientsAddedResult>(resultBus) { result ->
        onAddShareRecipients(result.permissions)
    }
}
