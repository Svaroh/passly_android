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

import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.ui.DefaultFilterUiModel
import org.junit.Test

class DefaultFilterSerializerTest {
    private val serializer = DefaultFilterSerializer()

    @Test
    fun `serialize maps every default filter to its stored identifier`() {
        val identifiers = DefaultFilterUiModel.entries.associateWith(serializer::serialize)

        assertThat(identifiers).containsExactlyEntriesIn(
            mapOf(
                DefaultFilterUiModel.LAST_USED to "LAST_USED",
                DefaultFilterUiModel.ALL_ITEMS to "ALL_ITEMS",
                DefaultFilterUiModel.FAVOURITES to "FAVOURITES",
                DefaultFilterUiModel.RECENTLY_MODIFIED to "RECENTLY_MODIFIED",
                DefaultFilterUiModel.SHARED_WITH_ME to "SHARED_WITH_ME",
                DefaultFilterUiModel.OWNED_BY_ME to "OWNED_BY_ME",
                DefaultFilterUiModel.EXPIRY to "EXPIRY",
                DefaultFilterUiModel.FOLDERS to "FOLDERS",
                DefaultFilterUiModel.TAGS to "TAGS",
                DefaultFilterUiModel.GROUPS to "GROUPS",
            ),
        )
    }

    @Test
    fun `deserialize restores every default filter from its stored identifier`() {
        DefaultFilterUiModel.entries.forEach {
            assertThat(serializer.deserialize(serializer.serialize(it))).isEqualTo(it)
        }
    }

    @Test
    fun `identifiers are unique across default filters`() {
        val identifiers = DefaultFilterUiModel.entries.map(serializer::serialize)

        assertThat(identifiers).containsNoDuplicates()
    }

    @Test
    fun `deserialize returns null for missing and unrecognized identifiers`() {
        assertThat(serializer.deserialize(null)).isNull()
        assertThat(serializer.deserialize("")).isNull()
        assertThat(serializer.deserialize("REMOVED_DEFAULT_FILTER")).isNull()
    }

    @Test
    fun `deserialize does not accept ordinals of the previously stored format`() {
        DefaultFilterUiModel.entries.indices.forEach {
            assertThat(serializer.deserialize(it.toString())).isNull()
        }
    }
}
