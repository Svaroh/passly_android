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

package com.passbolt.mobile.android.domain.resources.usecase

import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountDataUseCase
import com.passbolt.mobile.android.domain.permissionsconfirmation.usecase.GetPermissionsConfirmationOptOutUseCase
import com.passbolt.mobile.android.domain.resources.usecase.db.GetLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.feature.authentication.session.runAuthenticatedOperation
import com.passbolt.mobile.android.ui.PermissionModel
import com.passbolt.mobile.android.ui.PermissionModelUi
import com.passbolt.mobile.android.ui.ResourcePermission
import timber.log.Timber

class EditPermissionsConfirmationInteractor(
    private val getPermissionsConfirmationOptOutUseCase: GetPermissionsConfirmationOptOutUseCase,
    private val fetchResourcePermissionsUseCase: FetchResourcePermissionsUseCase,
    private val getLocalResourcePermissionsUseCase: GetLocalResourcePermissionsUseCase,
    private val getSelectedAccountDataUseCase: GetSelectedAccountDataUseCase,
) {
    suspend fun shouldConfirmPermissions(resourceId: String): Boolean {
        if (getPermissionsConfirmationOptOutUseCase.execute(Unit).isOptedOut) {
            Timber.d("Permissions confirmation opted out for this session - updating without confirmation")
            return false
        }
        val fetchOutput =
            runAuthenticatedOperation {
                fetchResourcePermissionsUseCase.execute(FetchResourcePermissionsUseCase.Input(resourceId))
            }
        return when (fetchOutput) {
            is FetchResourcePermissionsUseCase.Output.Success -> isShared(fetchOutput.permissions)
            is FetchResourcePermissionsUseCase.Output.Failure -> {
                Timber.e(
                    "Failed to fetch resource permissions to decide on confirmation: ${fetchOutput.message} - " +
                        "falling back to local permissions",
                )
                isResourceSharedLocally(resourceId)
            }
        }
    }

    private fun isShared(permissions: List<PermissionModel>): Boolean {
        val currentUserServerId = getSelectedAccountDataUseCase.execute(Unit).serverId
        val isOperatorDirectOwnershipOnly =
            permissions.singleOrNull()?.let {
                it is PermissionModel.UserPermissionModel &&
                    it.userId == currentUserServerId &&
                    it.permission == ResourcePermission.OWNER
            } == true
        return permissions.isNotEmpty() && !isOperatorDirectOwnershipOnly
    }

    private suspend fun isResourceSharedLocally(resourceId: String): Boolean {
        val resourcePermissions =
            getLocalResourcePermissionsUseCase
                .execute(GetLocalResourcePermissionsUseCase.Input(resourceId))
                .permissions
        val currentUserServerId = getSelectedAccountDataUseCase.execute(Unit).serverId
        val isOperatorDirectOwnershipOnly =
            resourcePermissions.singleOrNull()?.let {
                it is PermissionModelUi.UserPermissionModel &&
                    it.user.userId == currentUserServerId &&
                    it.permission == ResourcePermission.OWNER
            } == true
        return resourcePermissions.isNotEmpty() && !isOperatorDirectOwnershipOnly
    }
}
