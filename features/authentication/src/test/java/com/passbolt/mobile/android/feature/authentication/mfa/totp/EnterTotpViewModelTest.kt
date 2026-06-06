package net.svaroh.passly.feature.authentication.mfa.totp

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.feature.authentication.auth.usecase.RefreshSessionUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.SignOutUseCase
import net.svaroh.passly.feature.authentication.auth.usecase.VerifyTotpUseCase
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpIntent.Close
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpIntent.ConfirmSetupLeave
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpIntent.DismissSetupLeave
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpIntent.ValidateOtp
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpSideEffect.ClearOtp
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpSideEffect.CloseAndNavigateToStartup
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpSideEffect.NavigateToLogin
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpSideEffect.ShowErrorSnackbar
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpSideEffect.SnackbarErrorType.GENERIC
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpSideEffect.SnackbarErrorType.SESSION_EXPIRED
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpSideEffect.SnackbarErrorType.WRONG_CODE
import net.svaroh.passly.feature.authentication.mfa.totp.EnterTotpState.OtpTextColor.ERROR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
import org.mockito.kotlin.whenever
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class EnterTotpViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mock<SignOutUseCase>() }
                    single { mock<VerifyTotpUseCase>() }
                    single { mock<RefreshSessionUseCase>() }
                    factory { params ->
                        EnterTotpViewModel(
                            authToken = params[0],
                            hasOtherProvider = params[1],
                            isSetupFlow = params[2],
                            signOutUseCase = get(),
                            verifyTotpUseCase = get(),
                            refreshSessionUseCase = get(),
                        )
                    }
                },
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: EnterTotpViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `unauthorized triggers session refresh retry and on failure navigates to login`() =
        runTest {
            val verifyTotpUseCase: VerifyTotpUseCase = get()
            verifyTotpUseCase.stub {
                on { execute(any()) } doReturn VerifyTotpUseCase.Output.Unauthorized
            }

            val refreshSessionUseCase: RefreshSessionUseCase = get()
            whenever(refreshSessionUseCase.execute(any())) doReturn RefreshSessionUseCase.Output.Failure

            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(ValidateOtp("123456"))
                val error = awaitItem()
                assertIs<ShowErrorSnackbar>(error)
                assertThat(error.kind).isEqualTo(SESSION_EXPIRED)
                assertIs<NavigateToLogin>(awaitItem())
            }
        }

    @Test
    fun `wrong code sets error color and clears otp`() =
        runTest {
            val verifyTotpUseCase: VerifyTotpUseCase = get()
            verifyTotpUseCase.stub {
                on { execute(any()) } doReturn VerifyTotpUseCase.Output.WrongCode
            }

            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(ValidateOtp("000000"))
                val error = awaitItem()
                assertIs<ShowErrorSnackbar>(error)
                assertThat(error.kind).isEqualTo(WRONG_CODE)

                assertThat(viewModel.viewState.value.otpTextColor).isEqualTo(ERROR)

                assertIs<ClearOtp>(awaitItem())
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

    @Test
    fun `success with null mfaHeader emits generic error`() =
        runTest {
            val verifyTotpUseCase: VerifyTotpUseCase = get()
            verifyTotpUseCase.stub {
                on { execute(any()) } doReturn VerifyTotpUseCase.Output.Success(mfaHeader = null)
            }

            viewModel = get(parameters = { parametersOf(AUTH_TOKEN, false, false) })

            viewModel.sideEffect.test {
                viewModel.onIntent(ValidateOtp("123456"))
                val error = awaitItem()
                assertIs<ShowErrorSnackbar>(error)
                assertThat(error.kind).isEqualTo(GENERIC)
            }
        }

    private companion object {
        const val AUTH_TOKEN = "test-auth-token"
    }
}
