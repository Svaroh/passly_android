package net.svaroh.passly.resourcepicker.navigation

import net.svaroh.passly.core.compose.PassboltTheme
import net.svaroh.passly.core.navigation.compose.base.EntryProviderInstaller
import net.svaroh.passly.core.navigation.compose.base.FeatureModuleNavigation
import net.svaroh.passly.core.navigation.compose.keys.OtpNavigationKey.ResourcePicker
import net.svaroh.passly.resourcepicker.screen.ResourcePickerScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

class ResourcePickerFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<ResourcePicker> { key ->
                PassboltTheme {
                    ResourcePickerScreen(viewModel = koinViewModel(parameters = { parametersOf(key.suggestionUri) }))
                }
            }
        }
}
