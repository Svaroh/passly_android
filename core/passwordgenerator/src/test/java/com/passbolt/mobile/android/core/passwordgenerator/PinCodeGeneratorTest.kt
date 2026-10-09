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

package net.svaroh.passly.core.passwordgenerator

import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.ui.PinCodeUiModel.Companion.MAX_LENGTH
import net.svaroh.passly.ui.PinCodeUiModel.Companion.MIN_LENGTH
import org.junit.Assert.assertThrows
import org.junit.Test
import java.security.SecureRandom

class PinCodeGeneratorTest {
    private val generator = PinCodeGenerator(SecureRandom())

    @Test
    fun `generate returns a string of the requested length`() {
        val pin = generator.generate(8)
        assertThat(pin).hasLength(8)
    }

    @Test
    fun `generate returns only digits`() {
        repeat(20) {
            val pin = generator.generate(MAX_LENGTH)
            assertThat(pin.all { it.isDigit() }).isTrue()
        }
    }

    @Test
    fun `generate throws when length is out of range`() {
        assertThrows(IllegalArgumentException::class.java) { generator.generate(MIN_LENGTH - 1) }
        assertThrows(IllegalArgumentException::class.java) { generator.generate(MAX_LENGTH + 1) }
    }

    @Test
    fun `generate supports minimum boundary length`() {
        assertThat(generator.generate(MIN_LENGTH)).hasLength(MIN_LENGTH)
    }

    @Test
    fun `generate supports maximum boundary length`() {
        assertThat(generator.generate(MAX_LENGTH)).hasLength(MAX_LENGTH)
    }
}
