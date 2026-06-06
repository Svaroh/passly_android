package net.svaroh.passly.locationdetails

import PassboltTheme
import net.svaroh.passly.core.navigation.compose.base.EntryProviderInstaller
import net.svaroh.passly.core.navigation.compose.base.FeatureModuleNavigation
import net.svaroh.passly.core.navigation.compose.keys.LocationDetailsNavigationKey.LocationDetails
import net.svaroh.passly.locationdetails.ui.LocationItem
import net.svaroh.passly.core.navigation.compose.keys.LocationDetailsNavigationKey.LocationItem as NavigationLocationItem

class LocationDetailsFeatureNavigation : FeatureModuleNavigation {
    override fun provideEntryProviderInstaller(): EntryProviderInstaller =
        {
            entry<LocationDetails> { key ->
                PassboltTheme {
                    LocationDetailsScreen(
                        locationItem = key.locationItem.toFeatureLocationItem(),
                        itemId = key.itemId,
                    )
                }
            }
        }
}

private fun NavigationLocationItem.toFeatureLocationItem(): LocationItem =
    when (this) {
        NavigationLocationItem.RESOURCE -> LocationItem.RESOURCE
        NavigationLocationItem.FOLDER -> LocationItem.FOLDER
    }
