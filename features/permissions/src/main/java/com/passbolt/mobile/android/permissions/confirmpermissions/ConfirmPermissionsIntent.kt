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

package com.passbolt.mobile.android.permissions.confirmpermissions

import com.passbolt.mobile.android.ui.PermissionModelUi

sealed interface ConfirmPermissionsIntent {
    data object GoBack : ConfirmPermissionsIntent

    data object Confirm : ConfirmPermissionsIntent

    data object TrustNewMetadataKey : ConfirmPermissionsIntent

    data object TrustedMetadataKeyDeleted : ConfirmPermissionsIntent

    data object DismissMetadataKeyModifiedDialog : ConfirmPermissionsIntent

    data object DismissMetadataKeyDeletedDialog : ConfirmPermissionsIntent

    data object AddPermission : ConfirmPermissionsIntent

    data class SeePermission(
        val permission: PermissionModelUi,
    ) : ConfirmPermissionsIntent

    data class ShareRecipientsAdded(
        val recipients: List<PermissionModelUi>?,
    ) : ConfirmPermissionsIntent

    data class UserPermissionModified(
        val permission: PermissionModelUi.UserPermissionModel,
    ) : ConfirmPermissionsIntent

    data class UserPermissionDeleted(
        val permission: PermissionModelUi.UserPermissionModel,
    ) : ConfirmPermissionsIntent

    data class GroupPermissionModified(
        val permission: PermissionModelUi.GroupPermissionModel,
    ) : ConfirmPermissionsIntent

    data class GroupPermissionDeleted(
        val permission: PermissionModelUi.GroupPermissionModel,
    ) : ConfirmPermissionsIntent

    data class SkipConfirmationToggled(
        val isChecked: Boolean,
    ) : ConfirmPermissionsIntent
}
