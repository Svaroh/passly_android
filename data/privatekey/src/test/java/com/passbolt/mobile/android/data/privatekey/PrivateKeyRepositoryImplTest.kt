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

package net.svaroh.passly.data.privatekey

import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.domain.privatekey.datasource.PrivateKeyLocalDataSource
import net.svaroh.passly.domain.privatekey.model.PrivateKey
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

class PrivateKeyRepositoryImplTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single<PrivateKeyLocalDataSource> { mock<PrivateKeyLocalDataSource>() }
                        factoryOf(::PrivateKeyRepositoryImpl)
                    },
                ),
            )
        }

    private lateinit var localDataSource: PrivateKeyLocalDataSource
    private lateinit var repository: PrivateKeyRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = get()
        repository = get()
    }

    @Test
    fun `getPrivateKey delegates to local data source and returns its result`() {
        val privateKey = PrivateKey(ARMORED_KEY)
        localDataSource.stub { on { getPrivateKey(USER_ID) }.thenReturn(privateKey) }

        val result = repository.getPrivateKey(USER_ID)

        assertThat(result).isEqualTo(privateKey)
        verify(localDataSource).getPrivateKey(USER_ID)
    }

    @Test
    fun `getPrivateKey returns null when local data source has no key`() {
        localDataSource.stub { on { getPrivateKey(USER_ID) }.thenReturn(null) }

        val result = repository.getPrivateKey(USER_ID)

        assertThat(result).isNull()
    }

    @Test
    fun `savePrivateKey delegates to local data source and returns success`() {
        val privateKey = PrivateKey(ARMORED_KEY)
        localDataSource.stub { on { savePrivateKey(USER_ID, privateKey) }.thenReturn(true) }

        val result = repository.savePrivateKey(USER_ID, privateKey)

        assertThat(result).isTrue()
        verify(localDataSource).savePrivateKey(USER_ID, privateKey)
    }

    @Test
    fun `savePrivateKey returns false when local data source fails`() {
        val privateKey = PrivateKey(ARMORED_KEY)
        localDataSource.stub { on { savePrivateKey(USER_ID, privateKey) }.thenReturn(false) }

        val result = repository.savePrivateKey(USER_ID, privateKey)

        assertThat(result).isFalse()
    }

    @Test
    fun `removePrivateKey delegates to local data source`() {
        repository.removePrivateKey(USER_ID)

        verify(localDataSource).removePrivateKey(USER_ID)
    }

    private companion object {
        private const val USER_ID = "user-id"
        private const val ARMORED_KEY = "armored-private-key"
    }
}
