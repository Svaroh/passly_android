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

package net.svaroh.passly.domain.resourcetypes.usecase

import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import net.svaroh.passly.domain.resourcetypes.RefreshResourceTypesRepository
import net.svaroh.passly.domain.resourcetypes.model.ResourceType
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import java.util.UUID

class ResourceTypesInteractorTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mock<RefreshResourceTypesRepository>() }
                    single { mock<ResourceTypeIdToSlugMappingProvider>() }
                    single { mock<GetSelectedAccountUseCase>() }
                    singleOf(::ResourceTypesInteractor)
                },
            )
        }

    private val refreshResourceTypesRepository: RefreshResourceTypesRepository by inject()
    private val resourceTypeIdToSlugMappingProvider: ResourceTypeIdToSlugMappingProvider by inject()
    private val getSelectedAccountUseCase: GetSelectedAccountUseCase by inject()
    private val interactor: ResourceTypesInteractor by inject()

    private val resourceTypes =
        listOf(
            ResourceType(
                id = UUID.fromString("669f8c64-242a-59fb-92fc-81e660052e60"),
                slug = "password-and-description",
                name = "Password and description",
                deleted = null,
            ),
        )

    @Before
    fun setUp() {
        getSelectedAccountUseCase.stub {
            on { execute(Unit) }.thenReturn(GetSelectedAccountUseCase.Output(SELECTED_ACCOUNT_ID))
        }
    }

    @Test
    fun `fetchAndSaveResourceTypes returns Success and invalidates the cached mapping on refresh success`() =
        runTest {
            refreshResourceTypesRepository.stub {
                on { refreshResourceTypes(SELECTED_ACCOUNT_ID) }.thenReturn(DomainResult.Finished(resourceTypes))
            }

            val result = interactor.fetchAndSaveResourceTypes()

            assertThat(result).isEqualTo(ResourceTypesInteractor.Output.Success)
            verify(resourceTypeIdToSlugMappingProvider).invalidateSelectedUserMapping()
        }

    @Test
    fun `fetchAndSaveResourceTypes returns Failure and keeps the cached mapping on refresh failure`() =
        runTest {
            val failure = DomainResult.Incomplete.NotCached
            refreshResourceTypesRepository.stub { on { refreshResourceTypes(SELECTED_ACCOUNT_ID) }.thenReturn(failure) }

            val result = interactor.fetchAndSaveResourceTypes()

            assertThat(result).isEqualTo(ResourceTypesInteractor.Output.Failure(failure))
            verify(resourceTypeIdToSlugMappingProvider, never()).invalidateSelectedUserMapping()
        }

    private companion object {
        const val SELECTED_ACCOUNT_ID = "selected-account-id"
    }
}
