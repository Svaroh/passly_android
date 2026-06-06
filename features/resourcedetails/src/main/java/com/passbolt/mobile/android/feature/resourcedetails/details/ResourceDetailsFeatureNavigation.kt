package net.svaroh.passly.feature.resourcedetails.details

import PassboltTheme
import net.svaroh.passly.core.navigation.compose.base.EntryProviderInstaller
import net.svaroh.passly.core.navigation.compose.base.FeatureModuleNavigation
import net.svaroh.passly.core.navigation.compose.keys.ResourceDetailsNavigationKey.ResourceDetails

class ResourceDetailsFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<ResourceDetails> { key ->
                PassboltTheme {
                    ResourceDetailsScreen(
                        resourceModel = key.resourceModel,
                    )
                }
            }
        }
}
