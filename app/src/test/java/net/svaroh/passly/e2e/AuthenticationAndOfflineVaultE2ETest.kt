/**
 * Passly - Open source password manager for teams
 * Copyright (c) 2026 Svaroh
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License (AGPL) as published by the Free Software Foundation version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see GNU Affero General Public License v3 (http://www.gnu.org/licenses/agpl-3.0.html).
 *
 * @copyright Copyright (c) Svaroh
 * @license https://opensource.org/licenses/AGPL-3.0 AGPL License
 * @link https://passly.svaroh.net Passly
 * @since v1.0
 */
package net.svaroh.passly.e2e

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.setMain
import org.mockito.kotlin.atLeastOnce
import net.svaroh.passly.common.usecase.UserIdInput
import net.svaroh.passly.core.idlingresource.SignInIdlingResource
import net.svaroh.passly.core.mvp.authentication.MfaProvidersHandler
import net.svaroh.passly.core.navigation.ActivityIntents.AuthConfig
import net.svaroh.passly.core.navigation.AppContext
import net.svaroh.passly.core.passphrasememorycache.PassphraseMemoryCache
import net.svaroh.passly.core.security.rootdetection.RootDetector
import net.svaroh.passly.core.security.runtimeauth.RuntimeAuthenticatedFlag
import net.svaroh.passly.database.usecase.HasLocalReplicaUseCase
import net.svaroh.passly.domain.accounts.AuthenticatedAccountFlow
import net.svaroh.passly.domain.accounts.usecase.GetAccountDataUseCase
import net.svaroh.passly.domain.accounts.usecase.GetSelectedAccountUseCase
import net.svaroh.passly.domain.accounts.usecase.SaveSelectedAccountUseCase
import net.svaroh.passly.domain.accounts.usecase.SaveServerFingerprintUseCase
import net.svaroh.passly.domain.auth.model.ServerSignOutStatus
import net.svaroh.passly.domain.auth.usecase.GetPassphraseUseCase
import net.svaroh.passly.domain.auth.usecase.SaveMfaTokenUseCase
import net.svaroh.passly.domain.auth.usecase.SaveSessionUseCase
import net.svaroh.passly.domain.inappreview.usecase.InAppReviewInteractor
import net.svaroh.passly.domain.preferences.PreferencesDefaults
import net.svaroh.passly.domain.preferences.usecase.GetGlobalPreferencesUseCase
import net.svaroh.passly.domain.privatekey.model.PrivateKey
import net.svaroh.passly.domain.privatekey.usecase.GetPrivateKeyUseCase
import net.svaroh.passly.dto.response.ChallengeResponseDto
import net.svaroh.passly.encryptedstorage.biometric.BiometricCipher
import net.svaroh.passly.feature.authentication.auth.AuthIntent
import net.svaroh.passly.feature.authentication.auth.AuthSideEffect
import net.svaroh.passly.feature.authentication.auth.AuthViewModel
import net.svaroh.passly.feature.authentication.auth.challenge.MfaStatus
import net.svaroh.passly.feature.authentication.auth.challenge.MfaStatusProvider
import net.svaroh.passly.feature.authentication.auth.usecase.BackgroundSignInExecutor
import net.svaroh.passly.feature.authentication.auth.usecase.BiometryInteractor
import net.svaroh.passly.feature.authentication.auth.usecase.GetAndVerifyServerKeysAndTimeInteractor
import net.svaroh.passly.feature.authentication.auth.usecase.PostSignInActionsInteractor
import net.svaroh.passly.feature.authentication.auth.usecase.RefreshSessionUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.ServerKeysWarmup
import net.svaroh.passly.feature.authentication.auth.usecase.SignInVerifyInteractor
import net.svaroh.passly.feature.authentication.auth.usecase.SignOutUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.VerifyPassphraseUseCase
import net.svaroh.passly.ui.GlobalPreferencesUiModel
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class AuthenticationAndOfflineVaultE2ETest {
    private val testDispatcher = StandardTestDispatcher()

    private val mockGetAccountDataUseCase = mock<GetAccountDataUseCase>()
    private val mockGetPrivateKeyUseCase = mock<GetPrivateKeyUseCase>()
    private val mockVerifyPassphraseUseCase = mock<VerifyPassphraseUseCase>()
    private val mockBiometricCipher = mock<BiometricCipher>()
    private val mockGetPassphraseUseCase = mock<GetPassphraseUseCase>()
    private val mockPassphraseMemoryCache = mock<PassphraseMemoryCache>()
    private val mockRootDetector = mock<RootDetector>()
    private val mockBiometryInteractor = mock<BiometryInteractor>()
    private val mockGetGlobalPreferencesUseCase = mock<GetGlobalPreferencesUseCase>()
    private val mockRuntimeAuthenticatedFlag = mock<RuntimeAuthenticatedFlag>()
    private val mockSaveSessionUseCase = mock<SaveSessionUseCase>()
    private val mockSaveMfaTokenUseCase = mock<SaveMfaTokenUseCase>()
    private val mockSaveSelectedAccountUseCase = mock<SaveSelectedAccountUseCase>()
    private val mockGetSelectedAccountUseCase = mock<GetSelectedAccountUseCase>()
    private val mockSignOutUseCase = mock<SignOutUseCase>()
    private val mockSaveServerFingerprintUseCase = mock<SaveServerFingerprintUseCase>()
    private val mockMfaStatusProvider = mock<MfaStatusProvider>()
    private val mockGetAndVerifyServerKeysInteractor = mock<GetAndVerifyServerKeysAndTimeInteractor>()
    private val mockSignInVerifyInteractor = mock<SignInVerifyInteractor>()
    private val mockInAppReviewInteractor = mock<InAppReviewInteractor>()
    private val mockSignInIdlingResource = mock<SignInIdlingResource>()
    private val mockPostSignInActionsInteractor = mock<PostSignInActionsInteractor>()
    private val mockRefreshSessionUseCase = mock<RefreshSessionUseCase>()
    private val mockMfaProvidersHandler = mock<MfaProvidersHandler>()
    private val mockServerKeysWarmup = mock<ServerKeysWarmup>()
    private val mockHasLocalReplicaUseCase = mock<HasLocalReplicaUseCase>()
    private val mockBackgroundSignInExecutor = mock<BackgroundSignInExecutor>()

    private val authenticatedAccountFlow =
        AuthenticatedAccountFlow(
            mockGetSelectedAccountUseCase.apply {
                whenever(execute(Unit)).thenReturn(GetSelectedAccountUseCase.Output(null))
            },
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(mockGetAccountDataUseCase.execute(any())).thenReturn(
            GetAccountDataUseCase.Output(
                firstName = "Ton",
                lastName = "Tester",
                email = "ton@passly.svaroh.net",
                avatarUrl = null,
                url = "https://passly.svaroh.net",
                serverId = "server-user-id",
                label = "Ton Tester",
                role = "user",
            ),
        )
        whenever(mockGetPrivateKeyUseCase.execute(any())).thenReturn(
            GetPrivateKeyUseCase.Output(
                PrivateKey("-----BEGIN PGP PRIVATE KEY BLOCK-----\ntest\n-----END PGP PRIVATE KEY BLOCK-----"),
            ),
        )
        whenever(mockGetGlobalPreferencesUseCase.execute(Unit)).thenReturn(
            GlobalPreferencesUiModel(
                areDebugLogsEnabled = false,
                debugLogFileCreationDateTime = null,
                debugLogLastAppVersion = null,
                isHideRootDialogEnabled = false,
                isAuthRequiredOnEveryEntry = true,
                apiFetchPageSize = PreferencesDefaults.API_FETCH_PAGE_SIZE,
                isApiFetchPageSizeManuallySet = false,
                accessibilityPoliciesConsentGiven = false,
                deprecatedOsWarningHiddenForSdk = null,
            ),
        )
        whenever(mockRootDetector.isDeviceRooted()).thenReturn(false)
        whenever(mockMfaStatusProvider.provideMfaStatus()).thenReturn(MfaStatus.NOT_REQUIRED)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        reset(
            mockGetAccountDataUseCase,
            mockGetPrivateKeyUseCase,
            mockVerifyPassphraseUseCase,
            mockPassphraseMemoryCache,
            mockRuntimeAuthenticatedFlag,
            mockSaveSessionUseCase,
            mockSaveSelectedAccountUseCase,
            mockGetAndVerifyServerKeysInteractor,
            mockSignInVerifyInteractor,
            mockHasLocalReplicaUseCase,
            mockSignOutUseCase,
            mockRefreshSessionUseCase,
        )
    }

    private fun createViewModel(
        authConfig: AuthConfig = AuthConfig.Startup,
    ): AuthViewModel =
        AuthViewModel(
            authConfig = authConfig,
            userId = TEST_USER_ID,
            appContext = AppContext.APP,
            getAccountDataUseCase = mockGetAccountDataUseCase,
            getPrivateKeyUseCase = mockGetPrivateKeyUseCase,
            verifyPassphraseUseCase = mockVerifyPassphraseUseCase,
            biometricCipher = mockBiometricCipher,
            getPassphraseUseCase = mockGetPassphraseUseCase,
            passphraseMemoryCache = mockPassphraseMemoryCache,
            rootDetector = mockRootDetector,
            biometryInteractor = mockBiometryInteractor,
            getGlobalPreferencesUseCase = mockGetGlobalPreferencesUseCase,
            runtimeAuthenticatedFlag = mockRuntimeAuthenticatedFlag,
            saveSessionUseCase = mockSaveSessionUseCase,
            saveMfaTokenUseCase = mockSaveMfaTokenUseCase,
            saveSelectedAccountUseCase = mockSaveSelectedAccountUseCase,
            authenticatedAccountFlow = authenticatedAccountFlow,
            signOutUseCase = mockSignOutUseCase,
            saveServerFingerprintUseCase = mockSaveServerFingerprintUseCase,
            mfaStatusProvider = mockMfaStatusProvider,
            getAndVerifyServerKeysInteractor = mockGetAndVerifyServerKeysInteractor,
            signInVerifyInteractor = mockSignInVerifyInteractor,
            inAppReviewInteractor = mockInAppReviewInteractor,
            signInIdlingResource = mockSignInIdlingResource,
            postSignInActionsInteractor = mockPostSignInActionsInteractor,
            refreshSessionUseCase = mockRefreshSessionUseCase,
            mfaProvidersHandler = mockMfaProvidersHandler,
            serverKeysWarmup = mockServerKeysWarmup,
            hasLocalReplicaUseCase = mockHasLocalReplicaUseCase,
            backgroundSignInExecutor = mockBackgroundSignInExecutor,
        )

    @Test
    fun as_an_online_user_i_can_sign_in_with_valid_passphrase_and_receive_authenticated_session() =
        runTest(testDispatcher) {
            whenever(mockVerifyPassphraseUseCase.execute(any())).thenReturn(
                VerifyPassphraseUseCase.Output(isCorrect = true),
            )
            whenever(mockHasLocalReplicaUseCase.execute(any())).thenReturn(
                HasLocalReplicaUseCase.Output(hasLocalReplica = false),
            )

            doAnswer { invocation ->
                val onSuccess = invocation.getArgument<suspend (GetAndVerifyServerKeysAndTimeInteractor.Success) -> Unit>(2)
                runBlocking {
                    onSuccess(
                        GetAndVerifyServerKeysAndTimeInteractor.Success(
                            pgpKey = "mockServerPublicKey",
                            pgpKeyFingerprint = "mockFingerprint",
                            rsaKey = "mockRsaKey",
                        ),
                    )
                }
                Unit
            }.whenever(mockGetAndVerifyServerKeysInteractor).getAndVerifyServerKeys(any(), any(), any())

            doAnswer { invocation ->
                val onSuccess = invocation.getArgument<suspend (SignInVerifyInteractor.Success) -> Unit>(5)
                runBlocking {
                    onSuccess(
                        SignInVerifyInteractor.Success(
                            challengeResponseDto = ChallengeResponseDto(
                                version = "v4",
                                domain = "https://passly.svaroh.net",
                                verifyToken = "token",
                                accessToken = "mock-access-token",
                                refreshToken = "mock-refresh-token",
                                mfaProviders = null,
                            ),
                            accessToken = "mock-access-token",
                            refreshToken = "mock-refresh-token",
                            mfaToken = null,
                            currentMfaToken = null,
                        ),
                    )
                }
                Unit
            }.whenever(mockSignInVerifyInteractor).signInVerify(any(), any(), any(), any(), any(), any())

            doAnswer { invocation ->
                val onSuccess = invocation.getArgument<suspend () -> Unit>(1)
                runBlocking { onSuccess() }
                Unit
            }.whenever(mockPostSignInActionsInteractor).launchPostSignInActions(any(), any())

            val viewModel = createViewModel(authConfig = AuthConfig.SignIn)

            viewModel.sideEffect.test {
                viewModel.onIntent(AuthIntent.PassphraseInputChanged(TEST_PASSPHRASE.toByteArray()))
                viewModel.onIntent(AuthIntent.SignIn)
                testDispatcher.scheduler.advanceUntilIdle()

                assertIs<AuthSideEffect.HideKeyboard>(awaitItem())
                val effect = awaitItem()
                assertIs<AuthSideEffect.AuthSuccess>(effect)
                verify(mockRuntimeAuthenticatedFlag, atLeastOnce()).isAuthenticated = true
                verify(mockSaveSessionUseCase).execute(any())
                verify(mockPassphraseMemoryCache, atLeastOnce()).set(any())
            }
        }

    @Test
    fun as_a_user_with_incorrect_passphrase_sign_in_fails_with_wrong_passphrase_error() =
        runTest(testDispatcher) {
            whenever(mockVerifyPassphraseUseCase.execute(any())).thenReturn(
                VerifyPassphraseUseCase.Output(isCorrect = false),
            )

            val viewModel = createViewModel()

            viewModel.sideEffect.test {
                viewModel.onIntent(AuthIntent.PassphraseInputChanged("WrongPassword!".toByteArray()))
                viewModel.onIntent(AuthIntent.SignIn)
                testDispatcher.scheduler.advanceUntilIdle()

                assertIs<AuthSideEffect.HideKeyboard>(awaitItem())
                val effect = awaitItem()
                assertIs<AuthSideEffect.ShowErrorSnackbar>(effect)
                assertThat(effect.kind).isEqualTo(AuthSideEffect.SnackbarErrorType.WRONG_PASSPHRASE)
                verify(mockRuntimeAuthenticatedFlag, never()).isAuthenticated = true
            }
        }

    @Test
    fun as_an_offline_user_with_local_replica_i_can_unlock_vault_without_server_connection() =
        runTest(testDispatcher) {
            whenever(mockVerifyPassphraseUseCase.execute(any())).thenReturn(
                VerifyPassphraseUseCase.Output(isCorrect = true),
            )

            // Симулюємо відсутність підключення до сервера (Server Not Reachable / Offline)
            doAnswer { invocation ->
                val onError = invocation.getArgument<(GetAndVerifyServerKeysAndTimeInteractor.Error) -> Unit>(1)
                onError(GetAndVerifyServerKeysAndTimeInteractor.Error.ServerNotReachable("https://passly.svaroh.net"))
                Unit
            }.whenever(mockGetAndVerifyServerKeysInteractor).getAndVerifyServerKeys(any(), any(), any())

            // Локальна зашифрована репліка доступна на пристрої
            whenever(mockHasLocalReplicaUseCase.execute(UserIdInput(TEST_USER_ID))).thenReturn(
                HasLocalReplicaUseCase.Output(hasLocalReplica = true),
            )

            val viewModel = createViewModel()

            viewModel.sideEffect.test {
                viewModel.onIntent(AuthIntent.PassphraseInputChanged(TEST_PASSPHRASE.toByteArray()))
                viewModel.onIntent(AuthIntent.SignIn)
                testDispatcher.scheduler.advanceUntilIdle()

                assertIs<AuthSideEffect.HideKeyboard>(awaitItem())
                // Сховище розблоковується в офлайн режимі без потреби у сервері!
                val effect = awaitItem()
                assertIs<AuthSideEffect.AuthSuccess>(effect)
                verify(mockRuntimeAuthenticatedFlag, atLeastOnce()).isAuthenticated = true
                verify(mockPassphraseMemoryCache, atLeastOnce()).set(any())
                assertThat(viewModel.viewState.value.showServerNotReachable).isFalse()
            }
        }

    @Test
    fun as_an_offline_user_without_local_replica_vault_unlock_shows_server_unreachable_state() =
        runTest(testDispatcher) {
            whenever(mockVerifyPassphraseUseCase.execute(any())).thenReturn(
                VerifyPassphraseUseCase.Output(isCorrect = true),
            )

            // Симулюємо відсутність зв'язку
            doAnswer { invocation ->
                val onError = invocation.getArgument<(GetAndVerifyServerKeysAndTimeInteractor.Error) -> Unit>(1)
                onError(GetAndVerifyServerKeysAndTimeInteractor.Error.ServerNotReachable("https://passly.svaroh.net"))
                Unit
            }.whenever(mockGetAndVerifyServerKeysInteractor).getAndVerifyServerKeys(any(), any(), any())

            // Локальної репліки ще немає (наприклад, перший запуск додатку без інтернету)
            whenever(mockHasLocalReplicaUseCase.execute(UserIdInput(TEST_USER_ID))).thenReturn(
                HasLocalReplicaUseCase.Output(hasLocalReplica = false),
            )

            val viewModel = createViewModel()

            viewModel.sideEffect.test {
                viewModel.onIntent(AuthIntent.PassphraseInputChanged(TEST_PASSPHRASE.toByteArray()))
                viewModel.onIntent(AuthIntent.SignIn)
                testDispatcher.scheduler.advanceUntilIdle()

                assertIs<AuthSideEffect.HideKeyboard>(awaitItem())
                // Додаток коректно інформує про недоступність сервера замість падіння
                expectNoEvents()
                assertThat(viewModel.viewState.value.showServerNotReachable).isTrue()
                assertThat(viewModel.viewState.value.serverNotReachableDomain).isEqualTo("https://passly.svaroh.net")
            }
        }

    @Test
    fun active_session_can_be_refreshed_automatically() =
        runTest(testDispatcher) {
            whenever(mockRefreshSessionUseCase.execute(any())).thenReturn(
                RefreshSessionUseCase.Output.Success,
            )

            whenever(mockVerifyPassphraseUseCase.execute(any())).thenReturn(
                VerifyPassphraseUseCase.Output(isCorrect = true),
            )

            val viewModel = createViewModel(authConfig = AuthConfig.RefreshSession)

            viewModel.sideEffect.test {
                viewModel.onIntent(AuthIntent.PassphraseInputChanged(TEST_PASSPHRASE.toByteArray()))
                viewModel.onIntent(AuthIntent.SignIn)
                testDispatcher.scheduler.advanceUntilIdle()

                assertIs<AuthSideEffect.HideKeyboard>(awaitItem())
                val effect = awaitItem()
                assertIs<AuthSideEffect.AuthSuccess>(effect)
                verify(mockRefreshSessionUseCase).execute(any())
            }
        }

    @Test
    fun user_can_sign_out_which_clears_session_and_memory_cache() =
        runTest(testDispatcher) {
            whenever(mockSignOutUseCase.execute(any())).thenReturn(
                SignOutUseCase.Output(ServerSignOutStatus.SIGNED_OUT),
            )

            val viewModel = createViewModel()

            viewModel.sideEffect.test {
                viewModel.onIntent(AuthIntent.SignOut)
                testDispatcher.scheduler.advanceUntilIdle()

                val effect = awaitItem()
                assertIs<AuthSideEffect.NavigateBack>(effect)
                verify(mockSignOutUseCase).execute(any())
            }
        }

    @Test
    fun multi_account_user_can_switch_between_accounts_and_update_selected_account() =
        runTest(testDispatcher) {
            val targetUserId = "target-user-uuid-456"

            val viewModel = createViewModel()
            viewModel.sideEffect.test {
                viewModel.onIntent(AuthIntent.ConnectToExistingAccount)
                testDispatcher.scheduler.advanceUntilIdle()

                val effect = awaitItem()
                assertIs<AuthSideEffect.NavigateToAccountList>(effect)
            }

            // Перевіряємо, що збереження вибраного акаунту коректно делегується
            mockSaveSelectedAccountUseCase.execute(UserIdInput(targetUserId))
            verify(mockSaveSelectedAccountUseCase).execute(UserIdInput(targetUserId))
        }

    private companion object {
        private const val TEST_USER_ID = "test-user-uuid-123"
        private const val TEST_PASSPHRASE = "SuperSecretPassphrase123!"
    }
}
