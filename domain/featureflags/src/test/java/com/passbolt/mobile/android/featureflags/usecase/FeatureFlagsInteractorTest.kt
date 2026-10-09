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

package net.svaroh.passly.featureflags.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.architecture.result.DomainResult.Incomplete.Error.Reason.UNKNOWN
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import net.svaroh.passly.featureflags.FeatureFlagsRepository
import net.svaroh.passly.featureflags.mapper.toFeatureFlagsModel
import net.svaroh.passly.featureflags.model.FeatureFlags
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
import org.mockito.kotlin.whenever

class FeatureFlagsInteractorTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<FeatureFlagsRepository>() }
                        single { mock<GetSelectedAccountUseCase>() }
                        factoryOf(::FeatureFlagsInteractor)
                    },
                ),
            )
        }

    private lateinit var repository: FeatureFlagsRepository
    private lateinit var getSelectedAccountUseCase: GetSelectedAccountUseCase
    private lateinit var interactor: FeatureFlagsInteractor

    @Before
    fun setUp() {
        repository = get()
        getSelectedAccountUseCase = get()
        whenever(getSelectedAccountUseCase.execute(Unit)).thenReturn(GetSelectedAccountUseCase.Output(USER_ID))
        interactor = get()
    }

    @Test
    fun `refresh success returns success with refreshed feature flags model`() =
        runTest {
            val featureFlags = FeatureFlags.defaults().copy(isRbacAvailable = true)
            repository.stub {
                on { refreshFeatureFlags(USER_ID) }.thenReturn(DomainResult.Finished(featureFlags))
            }

            val result = interactor.fetchAndSaveFeatureFlags()

            assertThat(result).isEqualTo(FeatureFlagsInteractor.Output.Success(featureFlags.toFeatureFlagsModel()))
        }

    @Test
    fun `refresh failure returns failure`() =
        runTest {
            repository.stub {
                on { refreshFeatureFlags(USER_ID) }.thenReturn(DomainResult.Incomplete.Error(UNKNOWN, "boom"))
            }

            val result = interactor.fetchAndSaveFeatureFlags()

            assertThat(result).isEqualTo(FeatureFlagsInteractor.Output.Failure)
        }

    private companion object {
        const val USER_ID = "user-id"
    }
}
