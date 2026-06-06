package net.svaroh.passly.createfolder

import PassboltTheme
import net.svaroh.passly.core.navigation.compose.base.EntryProviderInstaller
import net.svaroh.passly.core.navigation.compose.base.FeatureModuleNavigation
import net.svaroh.passly.core.navigation.compose.keys.CreateFolderNavigationKey.CreateFolder

class CreateFolderFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<CreateFolder> { key ->
                PassboltTheme {
                    CreateFolderScreen(
                        parentFolderId = key.parentFolderId,
                    )
                }
            }
        }
}
