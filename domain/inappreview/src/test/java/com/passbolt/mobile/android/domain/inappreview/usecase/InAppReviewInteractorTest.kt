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

package net.svaroh.passly.domain.inappreview.usecase

import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import net.svaroh.passly.domain.inappreview.InAppReviewRepository
import net.svaroh.passly.domain.inappreview.model.InAppReviewParameters
import net.svaroh.passly.domain.inappreview.model.InAppReviewShowMode
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertTrue

class InAppReviewInteractorTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<InAppReviewRepository>() }
                        single { mock<GetSelectedAccountUseCase>() }
                        single<Clock> { Clock.fixed(BASE_INSTANT, ZoneId.systemDefault()) }
                        factoryOf(::InAppReviewInteractor)
                    },
                ),
            )
        }

    private lateinit var inAppReviewRepository: InAppReviewRepository
    private lateinit var getSelectedAccountUseCase: GetSelectedAccountUseCase
    private lateinit var clock: Clock
    private lateinit var interactor: InAppReviewInteractor

    @Before
    fun setUp() {
        inAppReviewRepository = get()
        getSelectedAccountUseCase = get()
        clock = get()
        interactor = get()
        whenever(getSelectedAccountUseCase.execute(Unit))
            .thenReturn(GetSelectedAccountUseCase.Output(USER_ID))
    }

    @Test
    fun `sign in should increase sign in count and set date if not already set`() {
        whenever(inAppReviewRepository.getInAppReviewParameters(USER_ID))
            .doReturn(InAppReviewParameters(null, 0))

        interactor.processSuccessfulSignIn()

        argumentCaptor<InAppReviewParameters> {
            verify(inAppReviewRepository).saveInAppReviewParameters(eq(USER_ID), capture())
            assertThat(firstValue.signInCount).isEqualTo(1)
            assertThat(firstValue.inAppReviewShowIntervalStartDate).isEqualTo(LocalDate.now(clock))
        }
    }

    @Test
    fun `app review show should reset review parameters and change intervals to consecutive show`() {
        interactor.inAppReviewFlowShowed()

        argumentCaptor<InAppReviewShowMode> {
            verify(inAppReviewRepository).saveInAppReviewShowMode(eq(USER_ID), capture())
            assertThat(firstValue).isInstanceOf(InAppReviewShowMode.ConsecutiveShow::class.java)
        }
        argumentCaptor<InAppReviewParameters> {
            verify(inAppReviewRepository).saveInAppReviewParameters(eq(USER_ID), capture())
            assertThat(firstValue.signInCount).isEqualTo(0)
            assertThat(firstValue.inAppReviewShowIntervalStartDate).isNull()
        }
    }

    @Test
    fun `first app review show should be showed when parameters are met`() {
        val showMode = InAppReviewShowMode.FirstShow()
        val passedDate = LocalDate.now(clock).minusDays(showMode.daysCount + 1L)
        val passedSignInCount = showMode.signInCount + 1
        whenever(inAppReviewRepository.getInAppReviewShowMode(USER_ID))
            .doReturn(showMode)
        whenever(inAppReviewRepository.getInAppReviewParameters(USER_ID))
            .doReturn(InAppReviewParameters(passedDate, passedSignInCount))

        assertTrue(interactor.shouldShowInAppReviewFlow())
    }

    private companion object {
        const val USER_ID = "user-id"
        val BASE_INSTANT: Instant = Instant.now()
    }
}
