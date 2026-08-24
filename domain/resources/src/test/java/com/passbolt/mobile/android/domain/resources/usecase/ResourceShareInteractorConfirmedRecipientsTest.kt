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

package com.passbolt.mobile.android.domain.resources.usecase

import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.core.architecture.result.DomainResult.Incomplete.Error.Reason.UNKNOWN
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountUseCase
import com.passbolt.mobile.android.domain.privatekey.PrivateKeyRepository
import com.passbolt.mobile.android.domain.resources.usecase.db.GetLocalResourcePermissionsUseCase
import com.passbolt.mobile.android.domain.secrets.usecase.decrypt.SecretInteractor
import com.passbolt.mobile.android.domain.share.model.ShareChanges
import com.passbolt.mobile.android.domain.share.model.ShareRecipient
import com.passbolt.mobile.android.domain.users.usecase.GetLocalUserUseCase
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.mappers.SharePermissionsModelMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verifyNoInteractions
import kotlin.test.assertIs

@ExperimentalCoroutinesApi
class ResourceShareInteractorConfirmedRecipientsTest {
    private val simulateShareUseCase = mock<SimulateShareResourceUseCase>()
    private val shareResourceUseCase = mock<ShareResourceUseCase>()
    private val secretInteractor = mock<SecretInteractor>()
    private val sharePermissionsModelMapper = mock<SharePermissionsModelMapper>()

    private val interactor =
        ResourceShareInteractor(
            getLocalResourcePermissionsUseCase = mock<GetLocalResourcePermissionsUseCase>(),
            getLocalUserUseCase = mock<GetLocalUserUseCase>(),
            simulateShareUseCase = simulateShareUseCase,
            shareResourceUseCase = shareResourceUseCase,
            getSelectedAccountUseCase = mock<GetSelectedAccountUseCase>(),
            privateKeyRepository = mock<PrivateKeyRepository>(),
            secretInteractor = secretInteractor,
            openPgp = mock<OpenPgp>(),
            passphraseMemoryCache = mock<PassphraseMemoryCache>(),
            sharePermissionsModelMapper = sharePermissionsModelMapper,
        )

    @Before
    fun setUp() {
        sharePermissionsModelMapper.stub {
            on { mapForSimulation(any(), any(), any()) } doReturn emptyList()
        }
        secretInteractor.stub {
            on { fetchAndDecrypt(RESOURCE_ID) } doReturn
                SecretInteractor.Output.FetchFailure(DomainResult.Incomplete.Error(UNKNOWN, "stop"))
        }
    }

    @Test
    fun `dry-run recipient outside the confirmed snapshot is a drift`() =
        runTest {
            stubSimulatedAdditions(USER_A, USER_B)

            val result =
                interactor.simulateAndShareResource(
                    resourceId = RESOURCE_ID,
                    recipients = emptyList(),
                    recipientsPublicKeys = mapOf(USER_A to "confirmed-key-$USER_A"),
                    existingPermissions = emptyList(),
                )

            assertIs<ResourceShareInteractor.Output.DriftDetected>(result)
            verifyNoInteractions(secretInteractor)
            verifyNoInteractions(shareResourceUseCase)
        }

    @Test
    fun `dry-run recipients within the confirmed snapshot proceed to share`() =
        runTest {
            stubSimulatedAdditions(USER_A)

            val result =
                interactor.simulateAndShareResource(
                    resourceId = RESOURCE_ID,
                    recipients = emptyList(),
                    recipientsPublicKeys = mapOf(USER_A to "confirmed-key-$USER_A"),
                    existingPermissions = emptyList(),
                )

            assertIs<ResourceShareInteractor.Output.SecretFetchFailure>(result)
        }

    @Test
    fun `share without confirmed keys skips the dry-run drift check`() =
        runTest {
            stubSimulatedAdditions(USER_A, USER_B)

            val result =
                interactor.simulateAndShareResource(
                    resourceId = RESOURCE_ID,
                    recipients = emptyList(),
                    existingPermissions = emptyList(),
                )

            assertIs<ResourceShareInteractor.Output.SecretFetchFailure>(result)
        }

    private fun stubSimulatedAdditions(vararg userIds: String) {
        simulateShareUseCase.stub {
            on { execute(any()) } doReturn
                SimulateShareResourceUseCase.Output.Success(
                    ShareChanges(
                        added = userIds.map { ShareRecipient(it) },
                        removed = emptyList(),
                    ),
                )
        }
    }

    private companion object {
        private const val RESOURCE_ID = "resource-id"
        private const val USER_A = "user-a"
        private const val USER_B = "user-b"
    }
}
