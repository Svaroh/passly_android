package net.svaroh.passly.folderdetails

import PassboltTheme
import net.svaroh.passly.core.navigation.compose.base.EntryProviderInstaller
import net.svaroh.passly.core.navigation.compose.base.FeatureModuleNavigation
import net.svaroh.passly.core.navigation.compose.keys.FolderDetailsNavigationKey.FolderDetails

class FolderDetailsFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<FolderDetails> { key ->
                PassboltTheme {
                    FolderDetailsScreen(folderId = key.folderId)
                }
            }
        }
}
