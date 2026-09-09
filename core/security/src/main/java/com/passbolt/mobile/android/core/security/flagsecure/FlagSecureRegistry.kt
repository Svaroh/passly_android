/**
 * Passbolt - Open source password manager for teams
 * Copyright (c) 2026 Passbolt SA
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

package com.passbolt.mobile.android.core.security.flagsecure

import android.app.Activity
import com.passbolt.mobile.android.core.security.flagsecure.WindowFlagAction.APPLY_FLAG
import com.passbolt.mobile.android.core.security.flagsecure.WindowFlagAction.CLEAR_FLAG
import com.passbolt.mobile.android.core.security.flagsecure.WindowFlagAction.NO_ACTION_NEEDED
import java.util.WeakHashMap

class FlagSecureRegistry {
    private val holdersCount = WeakHashMap<Activity, Int>()

    fun addActivity(activity: Activity): WindowFlagAction {
        val current = holdersCount[activity] ?: 0
        holdersCount[activity] = current + 1
        return if (current == 0) APPLY_FLAG else NO_ACTION_NEEDED
    }

    fun removeActivity(activity: Activity): WindowFlagAction {
        val current = holdersCount[activity] ?: return NO_ACTION_NEEDED
        return if (current <= 1) {
            holdersCount.remove(activity)
            CLEAR_FLAG
        } else {
            holdersCount[activity] = current - 1
            NO_ACTION_NEEDED
        }
    }
}
