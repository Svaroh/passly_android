package net.svaroh.passly.feature.resourceform.navigation

import PassboltTheme
import net.svaroh.passly.core.navigation.compose.base.EntryProviderInstaller
import net.svaroh.passly.core.navigation.compose.base.FeatureModuleNavigation
import net.svaroh.passly.core.navigation.compose.keys.ResourceFormNavigationKey.AdditionalUrisForm
import net.svaroh.passly.core.navigation.compose.keys.ResourceFormNavigationKey.AppearanceForm
import net.svaroh.passly.core.navigation.compose.keys.ResourceFormNavigationKey.CustomFieldsForm
import net.svaroh.passly.core.navigation.compose.keys.ResourceFormNavigationKey.DescriptionForm
import net.svaroh.passly.core.navigation.compose.keys.ResourceFormNavigationKey.MainResourceForm
import net.svaroh.passly.core.navigation.compose.keys.ResourceFormNavigationKey.NoteForm
import net.svaroh.passly.core.navigation.compose.keys.ResourceFormNavigationKey.PasswordForm
import net.svaroh.passly.core.navigation.compose.keys.ResourceFormNavigationKey.TotpAdvancedSettingsForm
import net.svaroh.passly.core.navigation.compose.keys.ResourceFormNavigationKey.TotpForm
import net.svaroh.passly.core.navigation.compose.results.ResultEffect
import net.svaroh.passly.core.navigation.compose.results.ScanOtpResultEvent
import net.svaroh.passly.feature.resourceform.additionalsecrets.customfields.CustomFieldsFormScreen
import net.svaroh.passly.feature.resourceform.additionalsecrets.note.NoteFormScreen
import net.svaroh.passly.feature.resourceform.additionalsecrets.password.PasswordFormScreen
import net.svaroh.passly.feature.resourceform.additionalsecrets.totp.TotpFormScreen
import net.svaroh.passly.feature.resourceform.additionalsecrets.totp.advanced.TotpAdvancedSettingsFormScreen
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.AdditionalUrisResult
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.AppearanceResult
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.CustomFieldsResult
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.DescriptionResult
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.NoteResult
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.PasswordResult
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.ScanOtpResult
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.TotpAdvancedSettingsResult
import net.svaroh.passly.feature.resourceform.main.ResourceFormIntent.TotpResult
import net.svaroh.passly.feature.resourceform.main.ResourceFormScreen
import net.svaroh.passly.feature.resourceform.main.ResourceFormViewModel
import net.svaroh.passly.feature.resourceform.metadata.additionaluris.AdditionalUrisFormScreen
import net.svaroh.passly.feature.resourceform.metadata.appearance.AppearanceFormScreen
import net.svaroh.passly.feature.resourceform.metadata.description.DescriptionFormScreen
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
                ResultEffect<NoteFormResult> { result ->
                    viewModel.onIntent(NoteResult(result.note))
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
        }
}
