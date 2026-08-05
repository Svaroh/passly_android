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

package com.passbolt.mobile.android.data.preferences.datasource.local

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.ui.HomeDisplayViewUiModel
import org.junit.Test

class HomeDisplayViewSerializerTest {
    private val serializer = HomeDisplayViewSerializer()

    @Test
    fun `serialize maps every home view to its stored identifier`() {
        val identifiers = HomeDisplayViewUiModel.entries.associateWith(serializer::serialize)

        assertThat(identifiers).containsExactlyEntriesIn(
            mapOf(
                HomeDisplayViewUiModel.ALL_ITEMS to "ALL_ITEMS",
                HomeDisplayViewUiModel.FAVOURITES to "FAVOURITES",
                HomeDisplayViewUiModel.RECENTLY_MODIFIED to "RECENTLY_MODIFIED",
                HomeDisplayViewUiModel.SHARED_WITH_ME to "SHARED_WITH_ME",
                HomeDisplayViewUiModel.OWNED_BY_ME to "OWNED_BY_ME",
                HomeDisplayViewUiModel.EXPIRY to "EXPIRY",
                HomeDisplayViewUiModel.FOLDERS to "FOLDERS",
                HomeDisplayViewUiModel.TAGS to "TAGS",
                HomeDisplayViewUiModel.GROUPS to "GROUPS",
            ),
        )
    }

    @Test
    fun `deserialize restores every home view from its stored identifier`() {
        HomeDisplayViewUiModel.entries.forEach {
            assertThat(serializer.deserialize(serializer.serialize(it))).isEqualTo(it)
        }
    }

    @Test
    fun `identifiers are unique across home views`() {
        val identifiers = HomeDisplayViewUiModel.entries.map(serializer::serialize)

        assertThat(identifiers).containsNoDuplicates()
    }

    @Test
    fun `deserialize returns null for missing and unrecognized identifiers`() {
        assertThat(serializer.deserialize(null)).isNull()
        assertThat(serializer.deserialize("")).isNull()
        assertThat(serializer.deserialize("REMOVED_HOME_VIEW")).isNull()
    }

    @Test
    fun `deserialize does not accept ordinals of the previously stored format`() {
        HomeDisplayViewUiModel.entries.indices.forEach {
            assertThat(serializer.deserialize(it.toString())).isNull()
        }
    }
}
