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
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.svaroh.passly.common.coroutinetimer.TimerFactory
import net.svaroh.passly.common.datarefresh.DataRefreshTrackingFlow
import net.svaroh.passly.commontest.TestCoroutineLaunchContext
import net.svaroh.passly.core.idlingresource.ResourceDetailActionIdlingResource
import net.svaroh.passly.core.otpcore.TotpParametersProvider
import net.svaroh.passly.core.passwordgenerator.PasswordGenerator
import net.svaroh.passly.core.passwordgenerator.PinCodeGenerator
import net.svaroh.passly.core.passwordgenerator.SecretGenerator
import net.svaroh.passly.core.passwordgenerator.dice.Dice
import net.svaroh.passly.core.passwordgenerator.entropy.EntropyCalculator
import net.svaroh.passly.domain.folders.usecase.GetLocalFolderLocationUseCase
import net.svaroh.passly.domain.metadata.usecase.CanShareResourceUseCase
import net.svaroh.passly.domain.rbac.usecase.GetRbacRulesUseCase
import net.svaroh.passly.domain.resources.actions.ResourceCommonActionsInteractor
import net.svaroh.passly.domain.resources.actions.ResourcePropertiesActionsInteractor
import net.svaroh.passly.domain.resources.actions.SecretPropertiesActionsInteractor
import net.svaroh.passly.domain.resources.actions.SecretPropertyActionResult
import net.svaroh.passly.domain.resources.usecase.db.GetLocalResourcePermissionsUseCase
import net.svaroh.passly.domain.resources.usecase.db.GetLocalResourceTagsUseCase
import net.svaroh.passly.domain.resources.usecase.db.GetLocalResourceUseCase
import net.svaroh.passly.entity.featureflags.FeatureFlagsModel
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsIntent
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsSideEffect
import net.svaroh.passly.feature.resourcedetails.details.ResourceDetailsViewModel
import net.svaroh.passly.featureflags.usecase.GetFeatureFlagsUseCase
import net.svaroh.passly.mappers.ResourceFormMapper
import net.svaroh.passly.ui.MetadataJsonModel
import net.svaroh.passly.ui.PasswordGeneratorSettingsUiModel
import net.svaroh.passly.ui.PinCodeUiModel
import net.svaroh.passly.ui.RbacModel
import net.svaroh.passly.ui.RbacRuleModel
import net.svaroh.passly.ui.ResourcePermission
import net.svaroh.passly.ui.ResourceUiModel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import java.security.SecureRandom
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ResourceVaultManagementE2ETest : KoinTest {

    private val testDispatcher = StandardTestDispatcher()

    private val mockSecretPropertiesActionsInteractor = mock<SecretPropertiesActionsInteractor>()
    private val mockResourcePropertiesActionsInteractor = mock<ResourcePropertiesActionsInteractor>()
    private val mockResourceCommonActionsInteractor = mock<ResourceCommonActionsInteractor>()

    private val testVaultModule = module {
        factory { (resourceModel: ResourceUiModel) -> mockSecretPropertiesActionsInteractor }
        factory { (resourceModel: ResourceUiModel) -> mockResourcePropertiesActionsInteractor }
        factory { (resourceModel: ResourceUiModel) -> mockResourceCommonActionsInteractor }
    }

    @get:Rule
    val koinTestRule =
        KoinTestRule.create {
            printLogger(Level.ERROR)
            modules(testVaultModule)
        }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun password_generator_creates_strong_passwords_matching_length_and_character_sets() =
        runTest {
            val coroutineLaunchContext = TestCoroutineLaunchContext()
            val generator = PasswordGenerator(coroutineLaunchContext)

            // 1. Тест паролю довжиною 16 символів з великими літерами, малими літерами та цифрами
            val settings16 = PasswordGeneratorSettingsUiModel(
                length = 16,
                maskUpper = true,
                maskLower = true,
                maskDigit = true,
                maskParenthesis = false,
                maskEmoji = false,
                maskChar1 = false,
                maskChar2 = false,
                maskChar3 = false,
                maskChar4 = false,
                maskChar5 = false,
                excludeLookAlikeChars = false,
            )
            val codepoints16 = generator.generate(settings16)
            assertThat(codepoints16).hasSize(16)

            // 2. Тест паролю довжиною 32 символи з усіма спеціальними символами
            val settings32 = PasswordGeneratorSettingsUiModel(
                length = 32,
                maskUpper = true,
                maskLower = true,
                maskDigit = true,
                maskParenthesis = true,
                maskEmoji = false,
                maskChar1 = true,
                maskChar2 = true,
                maskChar3 = true,
                maskChar4 = true,
                maskChar5 = true,
                excludeLookAlikeChars = true,
            )
            val codepoints32 = generator.generate(settings32)
            assertThat(codepoints32).hasSize(32)

            // Перевіряємо, що виключені look-alike символи (O, 0, l, 1, |, I)
            val lookAlikeValues = listOf('O'.code, '0'.code, 'l'.code, '1'.code, '|'.code, 'I'.code)
            val containsLookAlike = codepoints32.any { it.value in lookAlikeValues }
            assertThat(containsLookAlike).isFalse()
        }

    @Test
    fun pincode_generator_generates_digits_only_with_specified_lengths() {
        val pinGenerator = PinCodeGenerator(SecureRandom())

        // Генерація 4-значного PIN
        val pin4 = pinGenerator.generate(4)
        assertThat(pin4).hasLength(4)
        assertThat(pin4.all { it.isDigit() }).isTrue()

        // Генерація 6-значного PIN
        val pin6 = pinGenerator.generate(6)
        assertThat(pin6).hasLength(6)
        assertThat(pin6.all { it.isDigit() }).isTrue()

        // Генерація максимального 12-значного PIN
        val pin12 = pinGenerator.generate(PinCodeUiModel.MAX_LENGTH)
        assertThat(pin12).hasLength(PinCodeUiModel.MAX_LENGTH)
        assertThat(pin12.all { it.isDigit() }).isTrue()
    }

    @Test
    fun secret_generator_computes_high_entropy_for_complex_passwords() =
        runTest {
            val coroutineLaunchContext = TestCoroutineLaunchContext()
            val passwordGenerator = PasswordGenerator(coroutineLaunchContext)
            val mockDice = mock<Dice>()
            val entropyCalculator = EntropyCalculator(mockDice, coroutineLaunchContext)

            val secretGenerator = SecretGenerator(
                passwordGenerator = passwordGenerator,
                passphraseGenerator = mock(),
                entropyCalculator = entropyCalculator,
            )

            val complexSettings = PasswordGeneratorSettingsUiModel(
                length = 24,
                maskUpper = true,
                maskLower = true,
                maskDigit = true,
                maskParenthesis = true,
                maskEmoji = false,
                maskChar1 = true,
                maskChar2 = true,
                maskChar3 = true,
                maskChar4 = true,
                maskChar5 = true,
                excludeLookAlikeChars = false,
            )

            val result = secretGenerator.generatePassword(complexSettings)
            assertIs<SecretGenerator.SecretGenerationResult.Success>(result)
            assertThat(result.password).hasSize(24)
            // Висока ентропія для 24-символьного складного паролю (> 80 біт)
            assertThat(result.entropy).isGreaterThan(80.0)
        }

    @Test
    fun resource_details_manages_visibility_and_clipboard_actions_for_decrypted_secrets() =
        runTest {
            val decryptedSecret = "PasslyEnterpriseStrongSecret!2026"
            mockSecretPropertiesActionsInteractor.stub {
                on { providePassword() } doReturn flowOf(
                    SecretPropertyActionResult.Success(
                        SecretPropertiesActionsInteractor.SECRET_LABEL,
                        isSecret = true,
                        decryptedSecret,
                    ),
                )
            }

            val mockGetFeatureFlagsUseCase = mock<GetFeatureFlagsUseCase>()
            val mockGetLocalResourceUseCase = mock<GetLocalResourceUseCase>()
            val mockGetLocalResourcePermissionsUseCase = mock<GetLocalResourcePermissionsUseCase>()
            val mockGetLocalResourceTagsUseCase = mock<GetLocalResourceTagsUseCase>()
            val mockGetLocalFolderLocation = mock<GetLocalFolderLocationUseCase>()
            val mockTotpParametersProvider = mock<TotpParametersProvider>()
            val mockGetRbacRulesUseCase = mock<GetRbacRulesUseCase>()
            val mockResourceDetailActionIdlingResource = mock<ResourceDetailActionIdlingResource>()
            val mockCanShareResourceUseCase = mock<CanShareResourceUseCase>()
            val mockResourceFormMapper = mock<ResourceFormMapper>()
            val mockTimerFactory = mock<TimerFactory>()

            val featureFlags = FeatureFlagsModel(
                privacyPolicyUrl = null,
                termsAndConditionsUrl = null,
                isPreviewPasswordAvailable = true,
                areFoldersAvailable = true,
                areTagsAvailable = true,
                isTotpAvailable = true,
                isRbacAvailable = true,
                isPasswordExpiryAvailable = true,
                arePasswordPoliciesAvailable = true,
                canUpdatePasswordPolicies = true,
                isV5MetadataAvailable = false,
            )
            whenever(mockGetFeatureFlagsUseCase.execute(Unit)).thenReturn(
                GetFeatureFlagsUseCase.Output(featureFlags),
            )

            whenever(mockGetRbacRulesUseCase.execute(Unit)).thenReturn(
                GetRbacRulesUseCase.Output(
                    RbacModel(
                        passwordPreviewRule = RbacRuleModel.ALLOW,
                        passwordCopyRule = RbacRuleModel.ALLOW,
                        tagsUseRule = RbacRuleModel.ALLOW,
                        shareViewRule = RbacRuleModel.ALLOW,
                        foldersUseRule = RbacRuleModel.ALLOW,
                    ),
                ),
            )

            val testResource = ResourceUiModel(
                resourceId = UUID.randomUUID().toString(),
                resourceTypeId = UUID.randomUUID().toString(),
                slug = "password-and-description",
                folderId = null,
                permission = ResourcePermission.OWNER,
                favouriteId = null,
                modified = ZonedDateTime.now(),
                expiry = null,
                metadataKeyId = null,
                metadataKeyType = null,
                metadataJsonModel = MetadataJsonModel(
                    """
                    {
                        "name": "Production Database Admin",
                        "username": "admin@passly.internal",
                        "uri": "https://db.internal"
                    }
                    """.trimIndent(),
                ),
            )

            whenever(mockGetLocalResourceUseCase.execute(any())).thenReturn(
                GetLocalResourceUseCase.Output(testResource),
            )

            whenever(mockTimerFactory.createInfiniteTimer(any())).thenReturn(flowOf())
            whenever(mockGetLocalResourcePermissionsUseCase.execute(any())).thenReturn(
                GetLocalResourcePermissionsUseCase.Output(emptyList()),
            )
            whenever(mockGetLocalResourceTagsUseCase.execute(any())).thenReturn(
                GetLocalResourceTagsUseCase.Output(emptyList()),
            )
            whenever(mockGetLocalFolderLocation.execute(any())).thenReturn(
                GetLocalFolderLocationUseCase.Output(emptyList()),
            )

            val viewModel = ResourceDetailsViewModel(
                getFeatureFlagsUseCase = mockGetFeatureFlagsUseCase,
                getLocalResourceUseCase = mockGetLocalResourceUseCase,
                getLocalResourcePermissionsUseCase = mockGetLocalResourcePermissionsUseCase,
                getLocalResourceTagsUseCase = mockGetLocalResourceTagsUseCase,
                getLocalFolderLocation = mockGetLocalFolderLocation,
                totpParametersProvider = mockTotpParametersProvider,
                getRbacRulesUseCase = mockGetRbacRulesUseCase,
                resourceDetailActionIdlingResource = mockResourceDetailActionIdlingResource,
                canShareResourceUseCase = mockCanShareResourceUseCase,
                resourceFormMapper = mockResourceFormMapper,
                coroutineLaunchContext = TestCoroutineLaunchContext(),
                dataRefreshTrackingFlow = DataRefreshTrackingFlow(),
                timerFactory = mockTimerFactory,
            )

            viewModel.onIntent(ResourceDetailsIntent.Initialize(testResource))

            // 1. Початково пароль прихований
            viewModel.viewState.drop(1).test {
                // Перемикаємо видимість паролю
                viewModel.onIntent(ResourceDetailsIntent.TogglePasswordVisibility)
                val visibleState = awaitItem()
                assertThat(visibleState.passwordData.isPasswordVisible).isTrue()
                assertThat(visibleState.passwordData.password).isEqualTo(decryptedSecret)

                // Повторне натискання приховує пароль
                viewModel.onIntent(ResourceDetailsIntent.TogglePasswordVisibility)
                val hiddenState = awaitItem()
                assertThat(hiddenState.passwordData.isPasswordVisible).isFalse()
            }

            // 2. Копіювання паролю надсилає SideEffect в буфер обміну
            viewModel.sideEffect.test {
                viewModel.onIntent(ResourceDetailsIntent.CopyPassword)
                val sideEffect = awaitItem()
                assertIs<ResourceDetailsSideEffect.AddToClipboard>(sideEffect)
                assertThat(sideEffect.value).isEqualTo(decryptedSecret)
            }
        }
}
