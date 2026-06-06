package net.svaroh.passly.feature.home.screen

import net.svaroh.passly.core.navigation.AppContext
import net.svaroh.passly.core.navigation.compose.AppNavigator
import net.svaroh.passly.core.navigation.compose.keys.ResourceDetailsNavigationKey.ResourceDetails
import net.svaroh.passly.ui.ResourceModel

class DefaultResourceHandlingStrategy(
    private val navigator: AppNavigator,
) : ResourceHandlingStrategy {
    override val appContext: AppContext = AppContext.APP

    override fun resourceItemClick(resourceModel: ResourceModel) {
        navigator.navigateToKey(ResourceDetails(resourceModel))
    }

    override fun shouldShowResourceMoreMenu() = true

    override fun shouldShowCloseButton() = false

    override fun showSuggestedModel() = ShowSuggestedModel.DoNotShow

    override fun resourcePostCreateAction(resourceId: String) {
        // no-op
    }

    override fun shouldShowFolderMoreMenu() = true
}
