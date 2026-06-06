package net.svaroh.passly.feature.startup

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import net.svaroh.passly.domain.accounts.usecase.GetAccountsUseCase
import net.svaroh.passly.feature.startup.StartUpIntent.AcknowledgeDeprecatedOsWarning
import net.svaroh.passly.feature.startup.StartUpIntent.HideDeprecatedOsWarning
import net.svaroh.passly.feature.startup.StartUpSideEffect.NavigateToSetup
import net.svaroh.passly.feature.startup.StartUpSideEffect.NavigateToSignIn
import net.svaroh.passly.feature.startup.deprecatedoswarning.DeprecatedOsWarningInteractor
import net.svaroh.passly.ui.AccountSetupDataModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class StartUpViewModelTest : KoinTest {
    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(
                module {
                    single { mock<GetAccountsUseCase>() }
                    single { mock<DeprecatedOsWarningInteractor>() }
                    factory { (accountSetupDataModel: AccountSetupDataModel?) ->
                        StartUpViewModel(
                            accountSetupDataModel = accountSetupDataModel,
                            getAccountsUseCase = get(),
                            deprecatedOsWarningInteractor = get(),
                        )
                    }
                },
            )
        }

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should navigate to setup when no accounts exist`() =
        runTest {
            val getAccountsUseCase: GetAccountsUseCase = get()
            whenever(getAccountsUseCase.execute(Unit)) doReturn GetAccountsUseCase.Output(emptySet())

            val viewModel: StartUpViewModel = get { parametersOf(null) }

            viewModel.sideEffect.test {
                val effect = assertIs<NavigateToSetup>(awaitItem())
                assertThat(effect.accountSetupDataModel).isNull()
            }
        }

    @Test
    fun `should navigate to sign in when accounts exist`() =
        runTest {
            val getAccountsUseCase: GetAccountsUseCase = get()
            whenever(getAccountsUseCase.execute(Unit)) doReturn GetAccountsUseCase.Output(setOf("userId"))

            val viewModel: StartUpViewModel = get { parametersOf(null) }

            viewModel.sideEffect.test {
                assertIs<NavigateToSignIn>(awaitItem())
            }
        }

    @Test
    fun `should navigate to setup with data when account setup data is provided`() =
        runTest {
            val accountSetupData =
                AccountSetupDataModel(
                    serverUserId = "userId",
                    userName = "user",
                    domain = "https://passbolt.com",
                    firstName = "John",
                    lastName = "Doe",
                    avatarUrl = "https://passbolt.com/avatar.jpg",
                    keyFingerprint = "fingerprint",
                    armoredKey = "key",
                )

            val getAccountsUseCase: GetAccountsUseCase = get()
            whenever(getAccountsUseCase.execute(Unit)) doReturn GetAccountsUseCase.Output(setOf("existingUser"))

            val viewModel: StartUpViewModel = get { parametersOf(accountSetupData) }

            viewModel.sideEffect.test {
                val effect = assertIs<NavigateToSetup>(awaitItem())
                assertThat(effect.accountSetupDataModel).isEqualTo(accountSetupData)
            }
        }

    @Test
    fun `should hold navigation while the deprecated os warning is shown`() =
        runTest {
            stubExistingAccount()
            stubDeprecatedOsWarningShown()

            val viewModel: StartUpViewModel = get { parametersOf(null) }
            advanceUntilIdle()

            viewModel.viewState.test {
                assertThat(awaitItem().showDeprecatedOsWarning).isTrue()
            }
            viewModel.sideEffect.test {
                expectNoEvents()
            }
        }

    @Test
    fun `should navigate after the deprecated os warning is acknowledged`() =
        runTest {
            stubExistingAccount()
            stubDeprecatedOsWarningShown()

            val viewModel: StartUpViewModel = get { parametersOf(null) }
            advanceUntilIdle()

            viewModel.viewState.test {
                assertThat(awaitItem().showDeprecatedOsWarning).isTrue()

                viewModel.onIntent(AcknowledgeDeprecatedOsWarning)

                assertThat(awaitItem().showDeprecatedOsWarning).isFalse()
            }
            viewModel.sideEffect.test {
                assertIs<NavigateToSignIn>(awaitItem())
            }
            verify(deprecatedOsWarningInteractor(), never()).hideDeprecatedOsWarning()
        }

    @Test
    fun `should persist the choice and navigate after the deprecated os warning is hidden`() =
        runTest {
            stubExistingAccount()
            stubDeprecatedOsWarningShown()

            val viewModel: StartUpViewModel = get { parametersOf(null) }
            advanceUntilIdle()

            viewModel.viewState.test {
                assertThat(awaitItem().showDeprecatedOsWarning).isTrue()

                viewModel.onIntent(HideDeprecatedOsWarning)

                assertThat(awaitItem().showDeprecatedOsWarning).isFalse()
            }
            viewModel.sideEffect.test {
                assertIs<NavigateToSignIn>(awaitItem())
            }
            verify(deprecatedOsWarningInteractor()).hideDeprecatedOsWarning()
        }

    @Test
    fun `should navigate once when the deprecated os warning is dismissed twice`() =
        runTest {
            stubExistingAccount()
            stubDeprecatedOsWarningShown()

            val viewModel: StartUpViewModel = get { parametersOf(null) }
            advanceUntilIdle()

            viewModel.viewState.test {
                assertThat(awaitItem().showDeprecatedOsWarning).isTrue()

                viewModel.onIntent(AcknowledgeDeprecatedOsWarning)
                viewModel.onIntent(HideDeprecatedOsWarning)

                assertThat(awaitItem().showDeprecatedOsWarning).isFalse()
            }
            viewModel.sideEffect.test {
                assertIs<NavigateToSignIn>(awaitItem())
                expectNoEvents()
            }
            verify(deprecatedOsWarningInteractor(), never()).hideDeprecatedOsWarning()
        }

    private fun deprecatedOsWarningInteractor(): DeprecatedOsWarningInteractor = get()

    private fun stubExistingAccount() {
        val getAccountsUseCase: GetAccountsUseCase = get()
        whenever(getAccountsUseCase.execute(Unit)) doReturn GetAccountsUseCase.Output(setOf("userId"))
    }

    private fun stubDeprecatedOsWarningShown() {
        whenever(deprecatedOsWarningInteractor().shouldShowDeprecatedOsWarning()) doReturn true
    }
}
