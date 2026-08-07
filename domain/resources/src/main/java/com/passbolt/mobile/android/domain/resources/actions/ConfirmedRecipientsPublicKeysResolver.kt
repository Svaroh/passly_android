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

package com.passbolt.mobile.android.domain.resources.actions

import com.passbolt.mobile.android.domain.groups.usecase.GetGroupWithUsersUseCase
import com.passbolt.mobile.android.domain.permissionsconfirmation.usecase.GetPermissionsSnapshotUseCase
import com.passbolt.mobile.android.domain.users.usecase.GetLocalUserUseCase
import com.passbolt.mobile.android.ui.PermissionModelUi
import timber.log.Timber

class ConfirmedRecipientsPublicKeysResolver(
    private val getPermissionsSnapshotUseCase: GetPermissionsSnapshotUseCase,
    private val getLocalUserUseCase: GetLocalUserUseCase,
    private val getGroupWithUsersUseCase: GetGroupWithUsersUseCase,
) {
    suspend fun resolve(permissionsToApply: List<PermissionModelUi>): Map<String, String> {
        val snapshot = getPermissionsSnapshotUseCase.execute(Unit).snapshot
        val snapshotKeys =
            snapshot
                ?.users
                .orEmpty()
                .mapNotNull { (userId, profile) -> profile.gpgKey?.armoredKey?.let { userId to it } }
                .toMap()

        // the local key lookup covers users added by hand during the confirmation - they are not
        // part of the snapshot so no fresh key was fetched for them
        // TODO(MOB-4734): confirm the key source - consider fetching the added users' keys fresh by id
        val addedUsersKeys =
            permissionsToApply
                .filterIsInstance<PermissionModelUi.UserPermissionModel>()
                .filter { it.user.userId !in snapshotKeys }
                .mapNotNull { permission ->
                    localArmoredKey(permission.user.userId)?.let { permission.user.userId to it }
                }.toMap()

        // the local group members lookup covers groups added by hand during the confirmation - they
        // are not part of the snapshot so no fresh membership and keys were fetched for them
        // TODO(MOB-4734): confirm the key source - consider fetching the added groups' members and keys fresh by id
        val addedGroupsMembersKeys =
            permissionsToApply
                .filterIsInstance<PermissionModelUi.GroupPermissionModel>()
                .filter { snapshot == null || it.group.groupId !in snapshot.groupsMembers }
                .flatMap { groupPermission ->
                    getGroupWithUsersUseCase
                        .execute(GetGroupWithUsersUseCase.Input(groupPermission.group.groupId))
                        .groupWithUsers.users
                        .map { it.id to it.gpgKey.armoredKey }
                }.toMap()

        return snapshotKeys + addedUsersKeys + addedGroupsMembersKeys
    }

    private suspend fun localArmoredKey(userId: String): String? =
        try {
            getLocalUserUseCase
                .execute(GetLocalUserUseCase.Input(userId))
                .user.gpgKey.armoredKey
        } catch (exception: NullPointerException) {
            Timber.e(exception, "Added recipient not found in the local storage")
            null
        }
}
