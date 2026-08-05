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

package com.passbolt.mobile.android.data.inappreview.datasource.local

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.domain.inappreview.model.InAppReviewShowMode
import org.junit.Test

class InAppReviewShowSerializerTest {
    private val serializer = InAppReviewShowSerializer()

    @Test
    fun `serialize maps show modes to their stored identifiers`() {
        assertThat(serializer.serialize(InAppReviewShowMode.FirstShow())).isEqualTo("FIRST_SHOW")
        assertThat(serializer.serialize(InAppReviewShowMode.ConsecutiveShow())).isEqualTo("CONSECUTIVE_SHOW")
    }

    @Test
    fun `deserialize maps stored identifiers back to show modes`() {
        assertThat(serializer.deserialize("FIRST_SHOW")).isInstanceOf(InAppReviewShowMode.FirstShow::class.java)
        assertThat(serializer.deserialize("CONSECUTIVE_SHOW")).isInstanceOf(InAppReviewShowMode.ConsecutiveShow::class.java)
    }

    @Test
    fun `deserialize restores every show mode from its stored identifier`() {
        listOf(InAppReviewShowMode.FirstShow(), InAppReviewShowMode.ConsecutiveShow()).forEach {
            assertThat(serializer.deserialize(serializer.serialize(it))).isInstanceOf(it::class.java)
        }
    }

    @Test
    fun `deserialize returns null for missing and unrecognized identifiers`() {
        assertThat(serializer.deserialize(null)).isNull()
        assertThat(serializer.deserialize("")).isNull()
        assertThat(serializer.deserialize("REMOVED_SHOW_MODE")).isNull()
    }

    @Test
    fun `deserialize does not accept ordinals of the previously stored format`() {
        assertThat(serializer.deserialize("0")).isNull()
        assertThat(serializer.deserialize("1")).isNull()
    }
}
