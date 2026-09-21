package com.passbolt.mobile.android.feature.resourceform.navigation

import com.passbolt.mobile.android.core.compose.PassboltTheme
import com.passbolt.mobile.android.core.navigation.compose.base.EntryProviderInstaller
import com.passbolt.mobile.android.core.navigation.compose.base.FeatureModuleNavigation
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceFormNavigationKey.AdditionalUrisForm
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceFormNavigationKey.AdvancedSecretGenerationForm
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceFormNavigationKey.AppearanceForm
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceFormNavigationKey.CustomFieldsForm
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceFormNavigationKey.DescriptionForm
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceFormNavigationKey.MainResourceForm
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceFormNavigationKey.NoteForm
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceFormNavigationKey.PasswordForm
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceFormNavigationKey.PinCodeAdvancedGenerationForm
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceFormNavigationKey.PinCodeForm
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceFormNavigationKey.TotpAdvancedSettingsForm
import com.passbolt.mobile.android.core.navigation.compose.keys.ResourceFormNavigationKey.TotpForm
import com.passbolt.mobile.android.core.navigation.compose.results.PermissionsConfirmedResult
import com.passbolt.mobile.android.core.navigation.compose.results.ResultEffect
import com.passbolt.mobile.android.core.navigation.compose.results.ScanOtpResultEvent
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.customfields.CustomFieldsFormScreen
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.note.NoteFormScreen
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.password.PasswordFormScreen
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.pincode.PinCodeFormScreen
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.pincode.advanced.PinCodeAdvancedGenerationFormScreen
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.secret.advanced.AdvancedSecretGenerationScreen
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.TotpFormScreen
import com.passbolt.mobile.android.feature.resourceform.additionalsecrets.totp.advanced.TotpAdvancedSettingsFormScreen
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.AdditionalUrisResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.AdvancedSecretGenerationResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.AppearanceResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.ConfirmedPermissionsResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.CustomFieldsResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.DescriptionResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.NoteResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.PasswordResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.PinCodeAdvancedGenerationResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.PinCodeResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.ScanOtpResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.TotpAdvancedSettingsResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormIntent.TotpResult
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormScreen
import com.passbolt.mobile.android.feature.resourceform.main.ResourceFormViewModel
import com.passbolt.mobile.android.feature.resourceform.metadata.additionaluris.AdditionalUrisFormScreen
import com.passbolt.mobile.android.feature.resourceform.metadata.appearance.AppearanceFormScreen
import com.passbolt.mobile.android.feature.resourceform.metadata.description.DescriptionFormScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

class ResourceFormFeatureNavigation : FeatureModuleNavigation {
    @Suppress("LongMethod")
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<MainResourceForm> { key ->
                val viewModel: ResourceFormViewModel =
                    koinViewModel(
                        parameters = {
                            parametersOf(key.mode)
                        },
                    )

                ResultEffect<PasswordFormResult> { result ->
                    viewModel.onIntent(PasswordResult(result.model))
                }
                ResultEffect<TotpFormResult> { result ->
                    viewModel.onIntent(TotpResult(result.totpUiModel))
                }
                ResultEffect<TotpAdvancedSettingsFormResult> { result ->
                    viewModel.onIntent(TotpAdvancedSettingsResult(result.totpModel))
                }
                ResultEffect<AdvancedSecretGenerationFormResult> { result ->
                    viewModel.onIntent(AdvancedSecretGenerationResult(result))
                }
                ResultEffect<NoteFormResult> { result ->
                    viewModel.onIntent(NoteResult(result.note))
                }
                ResultEffect<PinCodeFormResult> { result ->
                    viewModel.onIntent(PinCodeResult(result.pinCodeUiModel))
                }
                ResultEffect<PinCodeAdvancedGenerationFormResult> { result ->
                    viewModel.onIntent(PinCodeAdvancedGenerationResult(result.pinCodeUiModel))
                }
                ResultEffect<DescriptionFormResult> { result ->
                    viewModel.onIntent(DescriptionResult(result.metadataDescription))
                }
                ResultEffect<AdditionalUrisFormResult> { result ->
                    viewModel.onIntent(AdditionalUrisResult(result.model))
                }
                ResultEffect<AppearanceFormResult> { result ->
                    viewModel.onIntent(AppearanceResult(result.model))
                }
                ResultEffect<CustomFieldsFormResult> {
                    viewModel.onIntent(CustomFieldsResult)
                }
                ResultEffect<ScanOtpResultEvent> { result ->
                    viewModel.onIntent(
                        ScanOtpResult(result.isManualCreationChosen, result.scannedTotp),
                    )
                }
                ResultEffect<PermissionsConfirmedResult> { result ->
                    viewModel.onIntent(ConfirmedPermissionsResult(result.permissions))
                }

                PassboltTheme {
                    ResourceFormScreen(viewModel = viewModel)
                }
            }

            entry<PasswordForm> { key ->
                PassboltTheme {
                    PasswordFormScreen(
                        mode = key.mode,
                        passwordModel = key.passwordModel,
                    )
                }
            }

            entry<TotpForm> { key ->
                PassboltTheme {
                    TotpFormScreen(
                        mode = key.mode,
                        totpUiModel = key.totpUiModel,
                    )
                }
            }

            entry<TotpAdvancedSettingsForm> { key ->
                PassboltTheme {
                    TotpAdvancedSettingsFormScreen(
                        mode = key.mode,
                        totpUiModel = key.totpUiModel,
                    )
                }
            }

            entry<AdvancedSecretGenerationForm> { key ->
                PassboltTheme {
                    AdvancedSecretGenerationScreen(
                        initialTab = key.selectedTab,
                        initialPasswordSettings = key.passwordSettings,
                        initialPassphraseSettings = key.passphraseSettings,
                    )
                }
            }

            entry<NoteForm> { key ->
                PassboltTheme {
                    NoteFormScreen(
                        mode = key.mode,
                        note = key.note,
                    )
                }
            }

            entry<DescriptionForm> { key ->
                PassboltTheme {
                    DescriptionFormScreen(
                        mode = key.mode,
                        metadataDescription = key.metadataDescription,
                    )
                }
            }

            entry<AdditionalUrisForm> { key ->
                PassboltTheme {
                    AdditionalUrisFormScreen(
                        mode = key.mode,
                        additionalUris = key.additionalUris,
                    )
                }
            }

            entry<AppearanceForm> { key ->
                PassboltTheme {
                    AppearanceFormScreen(
                        mode = key.mode,
                        appearanceModel = key.appearanceModel,
                    )
                }
            }

            entry<CustomFieldsForm> { key ->
                PassboltTheme {
                    CustomFieldsFormScreen(customFieldsUiModel = key.customFieldsUiModel)
                }
            }

            entry<PinCodeForm> { key ->
                PassboltTheme {
                    PinCodeFormScreen(
                        mode = key.mode,
                        pinCodeUiModel = key.pinCodeUiModel,
                    )
                }
            }

            entry<PinCodeAdvancedGenerationForm> { key ->
                PassboltTheme {
                    PinCodeAdvancedGenerationFormScreen(
                        mode = key.mode,
                        pinCodeUiModel = key.pinCodeUiModel,
                    )
                }
            }
        }
}
