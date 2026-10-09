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

package net.svaroh.passly.data.inappreview

import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.domain.inappreview.InAppReviewLocalDataSource
import net.svaroh.passly.domain.inappreview.model.InAppReviewParameters
import net.svaroh.passly.domain.inappreview.model.InAppReviewShowMode
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
import java.time.LocalDate

class InAppReviewRepositoryImplTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single<InAppReviewLocalDataSource> { mock<InAppReviewLocalDataSource>() }
                        factory { InAppReviewRepositoryImpl(localDataSource = get()) }
                    },
                ),
            )
        }

    private lateinit var localDataSource: InAppReviewLocalDataSource
    private lateinit var repository: InAppReviewRepositoryImpl

    @Before
    fun setUp() {
        localDataSource = get()
        repository = get()
    }

    @Test
    fun `getInAppReviewParameters returns value from local data source`() {
        val parameters = InAppReviewParameters(LocalDate.ofEpochDay(100), 3)
        whenever(localDataSource.getInAppReviewParameters(USER_ID)).thenReturn(parameters)

        assertThat(repository.getInAppReviewParameters(USER_ID)).isEqualTo(parameters)
    }

    @Test
    fun `getInAppReviewShowMode returns value from local data source`() {
        val showMode = InAppReviewShowMode.ConsecutiveShow()
        whenever(localDataSource.getInAppReviewShowMode(USER_ID)).thenReturn(showMode)

        assertThat(repository.getInAppReviewShowMode(USER_ID)).isEqualTo(showMode)
    }

    @Test
    fun `saveInAppReviewParameters delegates to local data source`() {
        val parameters = InAppReviewParameters(null, 0)

        repository.saveInAppReviewParameters(USER_ID, parameters)

        verify(localDataSource).saveInAppReviewParameters(USER_ID, parameters)
    }

    @Test
    fun `saveInAppReviewShowMode delegates to local data source`() {
        val showMode = InAppReviewShowMode.ConsecutiveShow()

        repository.saveInAppReviewShowMode(USER_ID, showMode)

        verify(localDataSource).saveInAppReviewShowMode(USER_ID, showMode)
    }

    private companion object {
        const val USER_ID = "user-id"
    }
}
