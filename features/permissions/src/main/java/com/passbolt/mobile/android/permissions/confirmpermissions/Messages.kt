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

import android.content.Context
import com.passbolt.mobile.android.permissions.confirmpermissions.SnackbarErrorType.ONE_OWNER_REQUIRED
import com.passbolt.mobile.android.permissions.confirmpermissions.SnackbarErrorType.PERMISSIONS_DRIFTED
import com.passbolt.mobile.android.permissions.confirmpermissions.ToastType.PERMISSIONS_FETCH_FAILURE
import com.passbolt.mobile.android.core.localization.R as LocalizationR

internal fun getErrorMessage(
    context: Context,
    type: SnackbarErrorType,
): String =
    context.getString(
        when (type) {
            ONE_OWNER_REQUIRED -> LocalizationR.string.resource_permissions_one_owner
            PERMISSIONS_DRIFTED -> LocalizationR.string.confirm_permissions_drifted
        },
    )

internal fun getToastMessage(
    context: Context,
    type: ToastType,
): String =
    context.getString(
        when (type) {
            PERMISSIONS_FETCH_FAILURE -> LocalizationR.string.confirm_permissions_fetch_failure
        },
    )
