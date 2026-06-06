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

package net.svaroh.passly.data.biometrickey

import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.domain.biometrickey.BiometricKeyLocalDataSource
import net.svaroh.passly.domain.biometrickey.model.BiometricKey
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class BiometricKeyRepositoryImplTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single<BiometricKeyLocalDataSource> { mock<BiometricKeyLocalDataSource>() }
                        factory {
                            BiometricKeyRepositoryImpl(
                                localDataSource = get(),
                            )
                        }
                    },
                ),
            )
        }

    private lateinit var localDataSource: BiometricKeyLocalDataSource
    private lateinit var repository: BiometricKeyRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = get()
        repository = get()
    }

    @Test
    fun `getBiometricKey delegates to the local data source`() {
        val biometricKey = BiometricKey(byteArrayOf(1, 2, 3))
        whenever(localDataSource.getBiometricKey(USER_ID)).thenReturn(biometricKey)

        val result = repository.getBiometricKey(USER_ID)

        assertThat(result).isSameInstanceAs(biometricKey)
    }

    @Test
    fun `saveBiometricKey writes through to the local data source`() {
        val biometricKey = BiometricKey(byteArrayOf(4, 5, 6))

        repository.saveBiometricKey(USER_ID, biometricKey)

        verify(localDataSource).saveBiometricKey(USER_ID, biometricKey)
    }

    @Test
    fun `removeBiometricKey delegates to the local data source`() {
        repository.removeBiometricKey()

        verify(localDataSource).removeBiometricKey()
    }

    private companion object {
        const val USER_ID = "user-id"
    }
}
