package net.svaroh.passly.feature.settings.accounts.keyinspector

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
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.svaroh.passly.commontest.TestCoroutineLaunchContext
import net.svaroh.passly.core.architecture.result.DomainResult
import net.svaroh.passly.core.architecture.result.DomainResult.Incomplete.Error.Reason.UNKNOWN
import net.svaroh.passly.core.formatter.DateFormatter
import net.svaroh.passly.core.formatter.FingerprintFormatter
import net.svaroh.passly.core.mvp.authentication.SessionRefreshTrackingFlow
import net.svaroh.passly.core.mvp.coroutinecontext.CoroutineLaunchContext
import net.svaroh.passly.core.passphrasememorycache.PassphraseMemoryCache
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountDataUseCase
import net.svaroh.passly.domain.users.usecase.FetchCurrentUserUseCase
import net.svaroh.passly.domain.users.usecase.GetLocalCurrentUserUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.GetSessionExpiryUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.GetSessionExpiryUseCase.Output.JwtWillExpire
import net.svaroh.passly.feature.settings.screen.accounts.keyinspector.KeyInspectorIntent.CopyFingerprint
import net.svaroh.passly.feature.settings.screen.accounts.keyinspector.KeyInspectorIntent.CopyUid
import net.svaroh.passly.feature.settings.screen.accounts.keyinspector.KeyInspectorScreenSideEffect.AddFingerprintToClipboard
import net.svaroh.passly.feature.settings.screen.accounts.keyinspector.KeyInspectorScreenSideEffect.AddUidToClipboard
import net.svaroh.passly.feature.settings.screen.accounts.keyinspector.KeyInspectorScreenSideEffect.ErrorSnackbarType.FAILED_TO_FETCH_KEY
import net.svaroh.passly.feature.settings.screen.accounts.keyinspector.KeyInspectorScreenSideEffect.ShowErrorSnackbar
import net.svaroh.passly.feature.settings.screen.accounts.keyinspector.KeyInspectorViewModel
import net.svaroh.passly.ui.GpgKeyUiModel
import net.svaroh.passly.ui.UserProfileUiModel
import net.svaroh.passly.ui.UserUiModel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
class KeyInspectorViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                listOf(
                    module {
                        single { mock<FetchCurrentUserUseCase>() }
                        single { mock<GetLocalCurrentUserUseCase>() }
                        single { mock<GetSelectedAccountDataUseCase>() }
                        single { mock<DateFormatter>() }
                        single { mock<FingerprintFormatter>() }
                        single { mock<GetSessionExpiryUseCase>() }
                        single { mock<PassphraseMemoryCache>() }
                        singleOf(::TestCoroutineLaunchContext) bind CoroutineLaunchContext::class
                        factoryOf(::KeyInspectorViewModel)
                        singleOf(::SessionRefreshTrackingFlow)
                    },
                ),
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: KeyInspectorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val passphraseMemoryCache: PassphraseMemoryCache = get()
        whenever(passphraseMemoryCache.getSessionDurationSeconds()) doReturn 5 * 60

        val getSessionExpiryUseCase: GetSessionExpiryUseCase = get()
        whenever(getSessionExpiryUseCase.execute(Unit)) doReturn JwtWillExpire(ZonedDateTime.now().plusMinutes(5))

        val getSelectedAccountDataUseCase = get<GetSelectedAccountDataUseCase>()
        whenever(getSelectedAccountDataUseCase.execute(Unit)) doReturn selectedAccountData

        val fetchCurrentUserUseCase = get<FetchCurrentUserUseCase>()
        fetchCurrentUserUseCase.stub {
            on { execute(Unit) } doReturn user
        }

        val getLocalCurrentUserUseCase = get<GetLocalCurrentUserUseCase>()
        getLocalCurrentUserUseCase.stub {
            onBlocking { execute(Unit) } doReturn GetLocalCurrentUserUseCase.Output(user.userUiModel)
        }

        val fingerprintFormatter: FingerprintFormatter = get()
        whenever(fingerprintFormatter.format(any(), any())) doAnswer { it.getArgument(0) }

        val dateFormatter: DateFormatter = get()
        whenever(dateFormatter.format(any())) doAnswer { it.getArgument<ZonedDateTime>(0).toString() }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `key data and account data should be shown initially`() =
        runTest {
            viewModel = get()

            viewModel.viewState.test {
                val state = awaitItem()
                assertThat(state.avatarUrl).isEqualTo(selectedAccountData.avatarUrl)
                assertThat(state.label).isEqualTo(selectedAccountData.label)
                assertThat(state.fingerprint).isEqualTo(user.userUiModel.gpgKey.fingerprint)
                assertThat(state.keyLength).isEqualTo(user.userUiModel.gpgKey.bits)
                assertThat(state.uid).isEqualTo(user.userUiModel.gpgKey.uid)
                assertThat(state.created).isEqualTo(
                    user.userUiModel.gpgKey.keyCreationDate
                        .toString(),
                )
                assertThat(state.expires).isEqualTo(
                    user.userUiModel.gpgKey.keyExpirationDate
                        .toString(),
                )
                assertThat(state.algorithm).isEqualTo(user.userUiModel.gpgKey.type)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `error should be shown when key data fails to fetch`() =
        runTest {
            val errorMessage = "errorMessage"
            // no local copy of the user, so this account has to ask the server
            val getLocalCurrentUserUseCase: GetLocalCurrentUserUseCase = get()
            getLocalCurrentUserUseCase.stub {
                onBlocking { execute(Unit) } doAnswer { throw IllegalStateException("no local user") }
            }
            val fetchCurrentUserUseCase: FetchCurrentUserUseCase = get()
            fetchCurrentUserUseCase.stub {
                on { execute(Unit) }.thenReturn(
                    FetchCurrentUserUseCase.Output.Failure(
                        DomainResult.Incomplete.Error(UNKNOWN, errorMessage),
                    ),
                )
            }

            viewModel = get()

            viewModel.sideEffect.test {
                val effect = awaitItem()
                assertThat(effect).isInstanceOf(ShowErrorSnackbar::class.java)
                assertThat((effect as ShowErrorSnackbar).type).isEqualTo(FAILED_TO_FETCH_KEY)
            }
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `key data should be read locally without asking the server`() =
        runTest {
            viewModel = get()

            viewModel.viewState.test {
                assertThat(awaitItem().fingerprint).isEqualTo(user.userUiModel.gpgKey.fingerprint)
            }

            val fetchCurrentUserUseCase: FetchCurrentUserUseCase = get()
            verifyNoInteractions(fetchCurrentUserUseCase)
        }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `copy actions should copy correct data`() =
        runTest {
            viewModel = get()

            viewModel.sideEffect.test {
                viewModel.onIntent(CopyUid)
                val copyUidEffect = awaitItem()
                assertThat(copyUidEffect).isInstanceOf(AddUidToClipboard::class.java)
                assertThat((copyUidEffect as AddUidToClipboard).uid)
                    .isEqualTo(user.userUiModel.gpgKey.uid)

                viewModel.onIntent(CopyFingerprint)
                val copyFingerprintEffect = awaitItem()
                assertThat(copyFingerprintEffect).isInstanceOf(AddFingerprintToClipboard::class.java)
                assertThat((copyFingerprintEffect as AddFingerprintToClipboard).fingerprint)
                    .isEqualTo(user.userUiModel.gpgKey.fingerprint)
            }
        }

    private companion object {
        private val selectedAccountData =
            GetSelectedAccountDataUseCase.Output(
                firstName = "John",
                lastName = "Doe",
                avatarUrl = "https://passbolt.com/avatar.jpg",
                label = "John Doe",
                email = "john.doe@passbolt.com",
                url = "https://passbolt.com",
                serverId = "123e4567-e89b-12d3-a456-426614174000",
                role = "admin",
            )

        private val user =
            FetchCurrentUserUseCase.Output.Success(
                UserUiModel(
                    id = "newUserId",
                    userName = "newUserName",
                    disabled = false,
                    gpgKey =
                        GpgKeyUiModel(
                            armoredKey = "keyData",
                            fingerprint = "fingerprint",
                            bits = 1,
                            uid = "uid",
                            keyId = "keyid",
                            type = "rsa",
                            keyExpirationDate = ZonedDateTime.now(),
                            keyCreationDate = ZonedDateTime.now(),
                            id = UUID.randomUUID().toString(),
                        ),
                    profile =
                        UserProfileUiModel(
                            username = "username",
                            firstName = "first",
                            lastName = "last",
                            avatarUrl = "avatar_url",
                        ),
                ),
            )
    }
}
