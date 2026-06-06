package net.svaroh.passly.resourcepicker.navigation

import PassboltTheme
import net.svaroh.passly.core.navigation.compose.base.EntryProviderInstaller
import net.svaroh.passly.core.navigation.compose.base.FeatureModuleNavigation
import net.svaroh.passly.core.navigation.compose.keys.OtpNavigationKey.ResourcePicker
import net.svaroh.passly.resourcepicker.screen.ResourcePickerScreen

class ResourcePickerFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<ResourcePicker> { key ->
                PassboltTheme {
                    ResourcePickerScreen(suggestionUri = key.suggestionUri)
                }
            }
        }
}
