package net.svaroh.passly.encryptedstorage

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.Mockito.mockConstruction
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import java.security.GeneralSecurityException

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
class EncryptedFileFactoryTest {
    private val context =
        mock<Context> {
            on { filesDir } doReturn File("files")
        }
    private val masterKey = mock<MasterKey>()
    private val factory = EncryptedFileFactory(context, masterKey)

    @Test
    fun `get should build a file once and reuse the instance`() {
        val encryptedFile = mock<EncryptedFile>()

        mockConstruction(EncryptedFile.Builder::class.java) { builder, _ ->
            whenever(builder.build()).thenReturn(encryptedFile)
        }.use { builders ->
            val first = factory.get(FILE_NAME)
            val second = factory.get(FILE_NAME)

            assertThat(first).isSameInstanceAs(encryptedFile)
            assertThat(second).isSameInstanceAs(encryptedFile)
            assertThat(builders.constructed()).hasSize(1)
        }
    }

    @Test
    fun `get should keep a separate instance per file name`() {
        mockConstruction(EncryptedFile.Builder::class.java) { builder, _ ->
            whenever(builder.build()).thenReturn(mock())
        }.use { builders ->
            val first = factory.get(FILE_NAME)
            val other = factory.get(OTHER_FILE_NAME)

            assertThat(first).isNotSameInstanceAs(other)
            assertThat(builders.constructed()).hasSize(2)
        }
    }

    @Test
    fun `get should not cache a failed build`() {
        val encryptedFile = mock<EncryptedFile>()

        mockConstruction(EncryptedFile.Builder::class.java) { builder, construction ->
            if (construction.count == 1) {
                whenever(builder.build()).thenThrow(GeneralSecurityException())
            } else {
                whenever(builder.build()).thenReturn(encryptedFile)
            }
        }.use { builders ->
            val failure = runCatching { factory.get(FILE_NAME) }
            val retry = factory.get(FILE_NAME)

            assertThat(failure.exceptionOrNull()).isInstanceOf(GeneralSecurityException::class.java)
            assertThat(retry).isSameInstanceAs(encryptedFile)
            assertThat(builders.constructed()).hasSize(2)
        }
    }

    private companion object {
        private const val FILE_NAME = "file"
        private const val OTHER_FILE_NAME = "other_file"
    }
}
