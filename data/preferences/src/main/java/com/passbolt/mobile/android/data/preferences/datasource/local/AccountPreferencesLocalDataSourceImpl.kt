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

package net.svaroh.passly.data.preferences.datasource.local

import androidx.core.content.edit
import net.svaroh.passly.data.preferences.AccountPreferencesFileName
import net.svaroh.passly.data.preferences.KEY_CHROME_NATIVE_AUTOFILL_DIALOG_SHOWN
import net.svaroh.passly.data.preferences.KEY_LAST_USED_HOME_VIEW_ID
import net.svaroh.passly.data.preferences.KEY_USER_SET_HOME_VIEW_ID
import net.svaroh.passly.domain.preferences.AccountFlagsUpdate
import net.svaroh.passly.domain.preferences.AccountPreferencesLocalDataSource
import net.svaroh.passly.domain.preferences.HomeDisplayViewPreferencesUpdate
import net.svaroh.passly.domain.preferences.PreferencesDefaults
import net.svaroh.passly.encryptedstorage.EncryptedSharedPreferencesFactory
import net.svaroh.passly.ui.AccountFlagsUiModel
import net.svaroh.passly.ui.HomeDisplayViewPreferencesUiModel
import timber.log.Timber

internal class AccountPreferencesLocalDataSourceImpl(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory,
    private val homeDisplayViewSerializer: HomeDisplayViewSerializer,
    private val defaultFilterSerializer: DefaultFilterSerializer,
) : AccountPreferencesLocalDataSource {
    override fun getHomeDisplayViewPreferences(userId: String): HomeDisplayViewPreferencesUiModel {
        with(sharedPreferences(userId)) {
            val lastUsedHomeViewId = getString(KEY_LAST_USED_HOME_VIEW_ID, null)
            val lastUsedHomeView = homeDisplayViewSerializer.deserialize(lastUsedHomeViewId)
            val userSetHomeViewId = getString(KEY_USER_SET_HOME_VIEW_ID, null)
            val userSetHomeView = defaultFilterSerializer.deserialize(userSetHomeViewId)

            if (lastUsedHomeViewId != null && lastUsedHomeView == null) {
                Timber.w("Stored home view id \"$lastUsedHomeViewId\" is not recognized, falling back to the default")
            }
            if (userSetHomeViewId != null && userSetHomeView == null) {
                Timber.w("Stored default filter id \"$userSetHomeViewId\" is not recognized, falling back to the default")
            }

            return HomeDisplayViewPreferencesUiModel(
                lastUsedHomeView = lastUsedHomeView ?: PreferencesDefaults.LAST_USED_HOME_VIEW,
                userSetHomeView = userSetHomeView ?: PreferencesDefaults.USER_SET_HOME_VIEW,
            )
        }
    }

    override fun updateHomeDisplayViewPreferences(
        update: HomeDisplayViewPreferencesUpdate,
        userId: String,
    ) {
        sharedPreferences(userId).edit {
            update.lastUsedHomeView?.let {
                putString(KEY_LAST_USED_HOME_VIEW_ID, homeDisplayViewSerializer.serialize(it))
            }
            update.userSetHomeView?.let {
                putString(KEY_USER_SET_HOME_VIEW_ID, defaultFilterSerializer.serialize(it))
            }
        }
    }

    override fun getAccountFlags(userId: String): AccountFlagsUiModel {
        with(sharedPreferences(userId)) {
            return AccountFlagsUiModel(
                wasChromeNativeAutofillDialogShown = getBoolean(KEY_CHROME_NATIVE_AUTOFILL_DIALOG_SHOWN, false),
            )
        }
    }

    override fun updateAccountFlags(
        update: AccountFlagsUpdate,
        userId: String,
    ) {
        sharedPreferences(userId).edit {
            update.wasChromeNativeAutofillDialogShown?.let { putBoolean(KEY_CHROME_NATIVE_AUTOFILL_DIALOG_SHOWN, it) }
        }
    }

    private fun sharedPreferences(userId: String) = encryptedSharedPreferencesFactory.get("${AccountPreferencesFileName(userId).name}.xml")
}
