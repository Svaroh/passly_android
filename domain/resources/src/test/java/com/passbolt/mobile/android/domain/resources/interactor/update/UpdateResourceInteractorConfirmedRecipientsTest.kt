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

package com.passbolt.mobile.android.domain.resources.interactor.update

import com.google.common.truth.Truth.assertThat
import com.passbolt.mobile.android.core.architecture.result.DomainResult
import com.passbolt.mobile.android.core.architecture.result.DomainResult.Incomplete.Error.Reason.UNKNOWN
import com.passbolt.mobile.android.core.passphrasememorycache.PassphraseMemoryCache
import com.passbolt.mobile.android.core.passphrasememorycache.PotentialPassphrase
import com.passbolt.mobile.android.domain.accounts.usecase.GetSelectedAccountUseCase
import com.passbolt.mobile.android.domain.passwordexpiry.model.PasswordExpirySettings
import com.passbolt.mobile.android.domain.passwordexpiry.usecase.GetPasswordExpirySettingsUseCase
import com.passbolt.mobile.android.domain.privatekey.PrivateKeyRepository
import com.passbolt.mobile.android.domain.privatekey.model.PrivateKey
import com.passbolt.mobile.android.domain.resources.ResourcesRepository
import com.passbolt.mobile.android.domain.resourcetypes.usecase.GetResourceTypeIdToSlugMappingUseCase
import com.passbolt.mobile.android.domain.secrets.model.SecretJsonModel
import com.passbolt.mobile.android.domain.secrets.usecase.decrypt.SecretInput
import com.passbolt.mobile.android.domain.users.usecase.FetchUsersUseCase
import com.passbolt.mobile.android.dto.request.CreateResourceDto
import com.passbolt.mobile.android.gopenpgp.OpenPgp
import com.passbolt.mobile.android.gopenpgp.exception.OpenPgpResult
import com.passbolt.mobile.android.jsonmodel.jsonModelModule
import com.passbolt.mobile.android.mappers.MetadataMapper
import com.passbolt.mobile.android.serializers.gson.MetadataEncryptor
import com.passbolt.mobile.android.serializers.gson.validation.JsonSchemaValidationRunner
import com.passbolt.mobile.android.supportedresourceTypes.ContentType.PasswordAndDescription
import com.passbolt.mobile.android.ui.GpgKeyUiModel
import com.passbolt.mobile.android.ui.MetadataJsonModel
import com.passbolt.mobile.android.ui.UpdateResourceModel
import com.passbolt.mobile.android.ui.UserProfileUiModel
import com.passbolt.mobile.android.ui.UserUiModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import java.util.UUID
import kotlin.test.assertIs

@ExperimentalCoroutinesApi
class UpdateResourceInteractorConfirmedRecipientsTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(jsonModelModule)
        }

    private val passphraseMemoryCache = mock<PassphraseMemoryCache>()
    private val resourcesRepository = mock<ResourcesRepository>()
    private val fetchUsersUseCase = mock<FetchUsersUseCase>()
    private val jsonSchemaValidationRunner = mock<JsonSchemaValidationRunner>()
    private val privateKeyRepository = mock<PrivateKeyRepository>()
    private val openPgp = mock<OpenPgp>()

    private val interactor =
        UpdateResourceInteractor(
            passphraseMemoryCache = passphraseMemoryCache,
            resourcesRepository = resourcesRepository,
            fetchUsersUseCase = fetchUsersUseCase,
            getResourceTypeIdToSlugMappingUseCase =
                mock<GetResourceTypeIdToSlugMappingUseCase>().stub {
                    on { execute(Unit) } doReturn
                        GetResourceTypeIdToSlugMappingUseCase.Output(
                            mapOf(UUID.randomUUID() to PasswordAndDescription.slug),
                        )
                },
            jsonSchemaValidationRunner = jsonSchemaValidationRunner,
            getSelectedAccountUseCase =
                mock<GetSelectedAccountUseCase>().stub {
                    on { execute(Unit) } doReturn GetSelectedAccountUseCase.Output(ACCOUNT_ID)
                },
            privateKeyRepository = privateKeyRepository,
            openPgp = openPgp,
            passwordExpirySettingsUseCase =
                mock<GetPasswordExpirySettingsUseCase>().stub {
                    on { execute(Unit) } doReturn PasswordExpirySettings.defaults()
                },
            metadataMapper = mock<MetadataMapper>(),
            metadataEncryptor = mock<MetadataEncryptor>(),
        )

    @Before
    fun setUp() {
        passphraseMemoryCache.stub {
            on { get() } doReturn PotentialPassphrase.Passphrase("passphrase".toByteArray())
        }
        jsonSchemaValidationRunner.stub {
            on { isSecretValid(any(), any()) } doReturn true
            on { isResourceValid(any(), any()) } doReturn true
        }
        fetchUsersUseCase.stub {
            on { execute(any()) } doReturn FetchUsersUseCase.Output.Success(listOf(user(USER_A), user(USER_B)))
        }
        privateKeyRepository.stub {
            on { getPrivateKey(ACCOUNT_ID) } doReturn PrivateKey("private-key")
        }
        openPgp.stub {
            on { encryptSignMessageArmored(any(), any(), any(), any()) }
                .doReturn(OpenPgpResult.Result("encrypted"))
        }
        resourcesRepository.stub {
            on { updateResource(any(), any(), any()) } doReturn DomainResult.Incomplete.Error(UNKNOWN, "stop")
        }
    }

    @Test
    fun `update fails closed when an unconfirmed recipient has access`() =
        runTest {
            val result =
                interactor.execute(
                    resourceInput = updateResourceModel(),
                    secretInput = secretInput(),
                    confirmedRecipientsPublicKeys = mapOf(USER_A to "confirmed-key-$USER_A"),
                )

            assertIs<UpdateResourceInteractor.Output.OpenPgpError>(result)
            verifyNoInteractions(openPgp)
            verifyNoInteractions(resourcesRepository)
        }

    @Test
    fun `confirmed keys are used for secret encryption`() =
        runTest {
            interactor.execute(
                resourceInput = updateResourceModel(),
                secretInput = secretInput(),
                confirmedRecipientsPublicKeys =
                    mapOf(
                        USER_A to "confirmed-key-$USER_A",
                        USER_B to "confirmed-key-$USER_B",
                    ),
            )

            val publicKeyCaptor = argumentCaptor<String>()
            verify(openPgp, times(2))
                .encryptSignMessageArmored(publicKeyCaptor.capture(), any(), any(), any())
            assertThat(publicKeyCaptor.allValues).containsExactly("confirmed-key-$USER_A", "confirmed-key-$USER_B")
        }

    @Test
    fun `local keys are used when there is no confirmation`() =
        runTest {
            interactor.execute(
                resourceInput = updateResourceModel(),
                secretInput = secretInput(),
            )

            val publicKeyCaptor = argumentCaptor<String>()
            verify(openPgp, times(2))
                .encryptSignMessageArmored(publicKeyCaptor.capture(), any(), any(), any())
            assertThat(publicKeyCaptor.allValues).containsExactly("gpg-key-$USER_A", "gpg-key-$USER_B")
        }

    @Test
    fun `unchanged secret skips encryption and sends no secrets payload`() =
        runTest {
            interactor.execute(
                resourceInput = updateResourceModel(),
                secretInput = secretInput(secretChanged = false),
            )

            val dtoCaptor = argumentCaptor<CreateResourceDto>()
            verify(resourcesRepository).updateResource(eq(RESOURCE_ID), dtoCaptor.capture(), any())
            assertThat(dtoCaptor.firstValue.secrets).isNull()
            verifyNoInteractions(openPgp)
            verifyNoInteractions(fetchUsersUseCase)
        }

    @Test
    fun `changed secret is encrypted for all users with access and sent`() =
        runTest {
            interactor.execute(
                resourceInput = updateResourceModel(),
                secretInput = secretInput(),
            )

            val dtoCaptor = argumentCaptor<CreateResourceDto>()
            verify(resourcesRepository).updateResource(eq(RESOURCE_ID), dtoCaptor.capture(), any())
            assertThat(dtoCaptor.firstValue.secrets).hasSize(2)
        }

    @Test
    fun `cached passphrase copy is wiped after execute`() =
        runTest {
            val cachedPassphraseCopy = "passphrase".toByteArray()
            passphraseMemoryCache.stub {
                on { get() } doReturn PotentialPassphrase.Passphrase(cachedPassphraseCopy)
            }

            interactor.execute(
                resourceInput = updateResourceModel(),
                secretInput = secretInput(),
            )

            assertThat(cachedPassphraseCopy.all { it == 0.toByte() }).isTrue()
        }

    private fun updateResourceModel() =
        UpdateResourceModel(
            contentType = PasswordAndDescription,
            resourceId = RESOURCE_ID,
            folderId = null,
            expiry = null,
            metadataKeyId = null,
            metadataKeyType = null,
            metadataJsonModel = MetadataJsonModel("""{"name": "Test"}"""),
        )

    private fun secretInput(secretChanged: Boolean = true) =
        SecretInput(
            secretJsonModel = SecretJsonModel("""{"password":"password"}"""),
            passwordChanged = false,
            secretChanged = secretChanged,
        )

    private fun user(userId: String) =
        UserUiModel(
            id = userId,
            userName = "$userId@passbolt.com",
            disabled = false,
            gpgKey =
                GpgKeyUiModel(
                    id = "gpg-$userId",
                    armoredKey = "gpg-key-$userId",
                    fingerprint = "fingerprint-$userId",
                    bits = 2048,
                    uid = null,
                    keyId = "key-$userId",
                    type = null,
                    keyExpirationDate = null,
                    keyCreationDate = null,
                ),
            profile =
                UserProfileUiModel(
                    username = "$userId@passbolt.com",
                    firstName = "first-$userId",
                    lastName = "last-$userId",
                    avatarUrl = null,
                ),
        )

    private companion object {
        private const val ACCOUNT_ID = "account-id"
        private const val RESOURCE_ID = "resource-id"
        private const val USER_A = "user-a"
        private const val USER_B = "user-b"
    }
}
