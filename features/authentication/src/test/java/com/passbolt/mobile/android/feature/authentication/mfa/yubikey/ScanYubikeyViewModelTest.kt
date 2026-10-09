package net.svaroh.passly.feature.authentication.mfa.yubikey

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.svaroh.passly.feature.authentication.auth.usecase.RefreshSessionUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.SignOutUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.VerifyYubikeyUseCase
import net.svaroh.passly.feature.authentication.mfa.yubikey.ScanYubikeyIntent.Close
import net.svaroh.passly.feature.authentication.mfa.yubikey.ScanYubikeyIntent.ConfirmSetupLeave
import net.svaroh.passly.feature.authentication.mfa.yubikey.ScanYubikeyIntent.DismissSetupLeave
import net.svaroh.passly.feature.authentication.mfa.yubikey.ScanYubikeyIntent.ValidateYubikeyOtp
import net.svaroh.passly.feature.authentication.mfa.yubikey.ScanYubikeySideEffect.CloseAndNavigateToStartup
import net.svaroh.passly.feature.authentication.mfa.yubikey.ScanYubikeySideEffect.ShowErrorSnackbar
import net.svaroh.passly.feature.authentication.mfa.yubikey.ScanYubikeySideEffect.SnackbarErrorType.EMPTY_OTP
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ScanYubikeyViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mock<SignOutUseCase>() }
                    single { mock<VerifyYubikeyUseCase>() }
                    single { mock<RefreshSessionUseCase>() }
                    factory { params ->
                        ScanYubikeyViewModel(
                            authToken = params[0],
                            hasOtherProvider = params[1],
                            isSetupFlow = params[2],
                            signOutUseCase = get(),
                            verifyYubikeyUseCase = get(),
                            refreshSessionUseCase = get(),
                        )
                    }
                },
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: ScanYubikeyViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `null otp emits empty otp error`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(ValidateYubikeyOtp(null))
                val error = awaitItem()
                assertIs<ShowErrorSnackbar>(error)
                assertThat(error.kind).isEqualTo(EMPTY_OTP)
            }
        }

    @Test
    fun `yubikey not from current user shows dialog`() =
        runTest {
            val verifyYubikeyUseCase: VerifyYubikeyUseCase = get()
            verifyYubikeyUseCase.stub {
                on { execute(any()) } doReturn VerifyYubikeyUseCase.Output.YubikeyNotFromCurrentUser
            }

            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, false) })

            viewModel.viewState.test {
                awaitItem() // initial state
                viewModel.onIntent(ValidateYubikeyOtp("yubikey-otp-value"))
                val progressState = awaitItem()
                assertThat(progressState.showProgress).isTrue()
                val dialogState = awaitItem()
                assertThat(dialogState.showNotFromCurrentUserDialog).isTrue()
                val finalState = awaitItem()
                assertThat(finalState.showProgress).isFalse()
            }
        }

    @Test
    fun `close in setup flow shows leave confirmation and does not sign out`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, true) })

            viewModel.onIntent(Close)

            assertThat(viewModel.viewState.value.showSetupLeaveConfirmationDialog).isTrue()
            verifyNoInteractions(get<SignOutUseCase>())
        }

    @Test
    fun `confirm setup leave signs out and navigates to startup`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, true) })
            viewModel.onIntent(Close)

            viewModel.sideEffect.test {
                viewModel.onIntent(ConfirmSetupLeave)
                assertIs<CloseAndNavigateToStartup>(awaitItem())
            }

            assertThat(viewModel.viewState.value.showSetupLeaveConfirmationDialog).isFalse()
            verify(get<SignOutUseCase>()).execute(Unit)
        }

    @Test
    fun `dismiss setup leave hides confirmation and does not sign out`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, true) })
            viewModel.onIntent(Close)

            viewModel.onIntent(DismissSetupLeave)

            assertThat(viewModel.viewState.value.showSetupLeaveConfirmationDialog).isFalse()
            verifyNoInteractions(get<SignOutUseCase>())
        }

    @Test
    fun `close outside setup flow signs out and navigates to startup`() =
        runTest {
            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(Close)
                assertIs<CloseAndNavigateToStartup>(awaitItem())
            }

            verify(get<SignOutUseCase>()).execute(Unit)
        }

    private companion object {
        const val AUTH_TOKEN = "test-auth-token"
    }
}
