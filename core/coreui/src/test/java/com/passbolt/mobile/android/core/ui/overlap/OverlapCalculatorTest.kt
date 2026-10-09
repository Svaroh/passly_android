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

package net.svaroh.passly.core.ui.overlap

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OverlapCalculatorTest {
    @Test
    fun `items that fit at full size are not overlapped`() {
        val result = OverlapCalculator(availableWidth = 1000, itemWidth = 100f, itemCount = 5).calculateLeftOverlapOffset()

        assertThat(result.allItemsFit).isTrue()
        assertThat(result.visibleItems).isEqualTo(5)
        assertThat(result.overlap).isEqualTo(0)
    }

    @Test
    fun `items are overlapped just enough to fit`() {
        val result = OverlapCalculator(availableWidth = 500, itemWidth = 100f, itemCount = 6).calculateLeftOverlapOffset()

        assertThat(result.allItemsFit).isTrue()
        assertThat(result.visibleItems).isEqualTo(6)
        // 6 * 100 - 5 * 21 = 495, the first width below the 500 available
        assertThat(result.overlap).isEqualTo(-21)
    }

    @Test
    fun `items exactly filling the width are still overlapped by a pixel`() {
        val result = OverlapCalculator(availableWidth = 500, itemWidth = 100f, itemCount = 5).calculateLeftOverlapOffset()

        assertThat(result.allItemsFit).isTrue()
        assertThat(result.visibleItems).isEqualTo(5)
        assertThat(result.overlap).isEqualTo(-1)
    }

    @Test
    fun `overlap stops growing at half the item width and reports how many items are visible`() {
        val result = OverlapCalculator(availableWidth = 300, itemWidth = 100f, itemCount = 10).calculateLeftOverlapOffset()

        assertThat(result.allItemsFit).isFalse()
        assertThat(result.overlap).isEqualTo(-50)
        // with the item effectively 50 wide, 300 of available width shows 6 of them
        assertThat(result.visibleItems).isEqualTo(6)
    }

    @Test
    fun `a larger allowed overlap can make an otherwise truncated list fit`() {
        val result =
            OverlapCalculator(
                availableWidth = 300,
                itemWidth = 100f,
                itemCount = 10,
                minOverlap = 90f,
            ).calculateLeftOverlapOffset()

        assertThat(result.allItemsFit).isTrue()
        assertThat(result.visibleItems).isEqualTo(10)
        assertThat(result.overlap).isEqualTo(-78)
    }
}
