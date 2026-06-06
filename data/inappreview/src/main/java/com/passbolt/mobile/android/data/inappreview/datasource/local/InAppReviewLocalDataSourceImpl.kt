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

package net.svaroh.passly.data.inappreview.datasource.local

import android.content.SharedPreferences
import androidx.core.content.edit
import net.svaroh.passly.domain.inappreview.InAppReviewLocalDataSource
import net.svaroh.passly.domain.inappreview.model.InAppReviewParameters
import net.svaroh.passly.domain.inappreview.model.InAppReviewShowMode
import net.svaroh.passly.encryptedstorage.EncryptedSharedPreferencesFactory
import java.time.LocalDate

internal class InAppReviewLocalDataSourceImpl(
    private val encryptedSharedPreferencesFactory: EncryptedSharedPreferencesFactory,
    private val inAppReviewShowSerializer: InAppReviewShowSerializer,
) : InAppReviewLocalDataSource {
    override fun getInAppReviewParameters(userId: String): InAppReviewParameters =
        sharedPreferences(userId).let {
            InAppReviewParameters(
                inAppReviewShowIntervalStartDate =
                    it
                        .getLong(KEY_IN_APP_REVIEW_INTERVAL_START_DATE, -1)
                        .let { intervalStartEpochDaysDate ->
                            if (intervalStartEpochDaysDate == -1L) {
                                null
                            } else {
                                LocalDate.ofEpochDay(intervalStartEpochDaysDate)
                            }
                        },
                signInCount = it.getInt(KEY_SIGN_IN_COUNT, 0),
            )
        }

    override fun saveInAppReviewParameters(
        userId: String,
        parameters: InAppReviewParameters,
    ) {
        sharedPreferences(userId).edit {
            parameters.inAppReviewShowIntervalStartDate.let {
                if (it == null) {
                    remove(KEY_IN_APP_REVIEW_INTERVAL_START_DATE)
                } else {
                    putLong(KEY_IN_APP_REVIEW_INTERVAL_START_DATE, it.toEpochDay())
                }
            }
            putInt(KEY_SIGN_IN_COUNT, parameters.signInCount)
        }
    }

    override fun getInAppReviewShowMode(userId: String): InAppReviewShowMode =
        sharedPreferences(userId).let {
            inAppReviewShowSerializer.deserialize(it.getString(KEY_IN_APP_REVIEW_SHOW_MODE_ID, null))
                ?: InAppReviewShowMode.FirstShow()
        }

    override fun saveInAppReviewShowMode(
        userId: String,
        showMode: InAppReviewShowMode,
    ) {
        sharedPreferences(userId).edit {
            putString(
                KEY_IN_APP_REVIEW_SHOW_MODE_ID,
                inAppReviewShowSerializer.serialize(showMode),
            )
        }
    }

    private fun sharedPreferences(userId: String): SharedPreferences =
        encryptedSharedPreferencesFactory.get("${InAppReviewFileName(userId).name}.xml")
}
