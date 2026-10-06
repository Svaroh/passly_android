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

package net.svaroh.passly.data.favourites

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.architecture.result.DomainResult.Incomplete.Error.Reason.UNKNOWN
import net.svaroh.passly.domain.favourites.FavouritesDataSource
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

class FavouritesRepositoryImplTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single<FavouritesDataSource> { mock<FavouritesDataSource>() }
                        factoryOf(::FavouritesRepositoryImpl)
                    },
                ),
            )
        }

    private lateinit var remote: FavouritesDataSource
    private lateinit var repository: FavouritesRepositoryImpl

    @Before
    fun setUp() {
        remote = get()
        repository = get()
    }

    @Test
    fun `addToFavourites delegates to remote and returns its result`() =
        runTest {
            remote.stub { on { addToFavourites(RESOURCE_ID) }.thenReturn(DomainResult.Finished(FAVOURITE_ID)) }

            val result = repository.addToFavourites(RESOURCE_ID)

            assertThat(result).isEqualTo(DomainResult.Finished(FAVOURITE_ID))
            verify(remote).addToFavourites(RESOURCE_ID)
        }

    @Test
    fun `addToFavourites propagates remote failure`() =
        runTest {
            val failure = DomainResult.Incomplete.Error(UNKNOWN, "boom")
            remote.stub { on { addToFavourites(RESOURCE_ID) }.thenReturn(failure) }

            val result = repository.addToFavourites(RESOURCE_ID)

            assertThat(result).isEqualTo(failure)
        }

    @Test
    fun `removeFromFavourites delegates to remote and returns its result`() =
        runTest {
            remote.stub { on { removeFromFavourites(FAVOURITE_ID) }.thenReturn(DomainResult.Finished(Unit)) }

            val result = repository.removeFromFavourites(FAVOURITE_ID)

            assertThat(result).isEqualTo(DomainResult.Finished(Unit))
            verify(remote).removeFromFavourites(FAVOURITE_ID)
        }

    @Test
    fun `removeFromFavourites propagates remote failure`() =
        runTest {
            remote.stub { on { removeFromFavourites(FAVOURITE_ID) }.thenReturn(DomainResult.Incomplete.Unauthorized) }

            val result = repository.removeFromFavourites(FAVOURITE_ID)

            assertThat(result).isEqualTo(DomainResult.Incomplete.Unauthorized)
        }

    private companion object {
        private const val RESOURCE_ID = "resource-id"
        private const val FAVOURITE_ID = "favourite-id"
    }
}
