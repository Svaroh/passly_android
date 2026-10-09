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

package net.svaroh.passly.data.featureflags

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.architecture.result.DomainResult.Incomplete.Error.Reason.UNKNOWN
import net.svaroh.passly.featureflags.FeatureFlagsLocalDataSource
import net.svaroh.passly.featureflags.FeatureFlagsRemoteDataSource
import net.svaroh.passly.featureflags.model.FeatureFlags
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

class FeatureFlagsRepositoryImplTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single<FeatureFlagsLocalDataSource> { mock<FeatureFlagsLocalDataSource>() }
                        single<FeatureFlagsRemoteDataSource> { mock<FeatureFlagsRemoteDataSource>() }
                        factory {
                            FeatureFlagsRepositoryImpl(
                                localDataSource = get(),
                                remoteDataSource = get(),
                            )
                        }
                    },
                ),
            )
        }

    private lateinit var local: FeatureFlagsLocalDataSource
    private lateinit var remote: FeatureFlagsRemoteDataSource
    private lateinit var repository: FeatureFlagsRepositoryImpl
    private val featureFlags = FeatureFlags.defaults()

    @Before
    fun setUp() {
        local = get()
        remote = get()
        repository = get()
    }

    @Test
    fun `getFeatureFlags returns local value and never calls remote`() =
        runTest {
            local.stub { on { getFeatureFlags(USER_ID) }.thenReturn(DomainResult.Finished(featureFlags)) }

            val result = repository.getFeatureFlags(USER_ID)

            assertThat(result).isEqualTo(DomainResult.Finished(featureFlags))
            verify(remote, never()).getFeatureFlags()
        }

    @Test
    fun `refreshFeatureFlags with remote success returns success and writes to local`() =
        runTest {
            remote.stub { on { getFeatureFlags() }.thenReturn(DomainResult.Finished(featureFlags)) }

            val result = repository.refreshFeatureFlags(USER_ID)

            assertThat(result).isEqualTo(DomainResult.Finished(featureFlags))
            verify(local).setFeatureFlags(USER_ID, featureFlags)
        }

    @Test
    fun `refreshFeatureFlags with remote failure returns failure and does not write to local`() =
        runTest {
            val failure = DomainResult.Incomplete.Error(UNKNOWN, "boom")
            remote.stub { on { getFeatureFlags() }.thenReturn(failure) }

            val result = repository.refreshFeatureFlags(USER_ID)

            assertThat(result).isEqualTo(failure)
            verify(local, never()).setFeatureFlags(USER_ID, featureFlags)
        }

    private companion object {
        const val USER_ID = "user-id"
    }
}
