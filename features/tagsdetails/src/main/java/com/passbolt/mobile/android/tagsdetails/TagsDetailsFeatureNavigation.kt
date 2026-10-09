package net.svaroh.passly.tagsdetails

import net.svaroh.passly.core.compose.PassboltTheme
import net.svaroh.passly.core.navigation.compose.base.EntryProviderInstaller
import net.svaroh.passly.core.navigation.compose.base.FeatureModuleNavigation
import net.svaroh.passly.core.navigation.compose.keys.TagsDetailsNavigationKey.ResourceTags

class TagsDetailsFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<ResourceTags> { key ->
                PassboltTheme {
                    ResourceTagsScreen(
                        resourceId = key.resourceId,
                    )
                }
            }
        }
}
