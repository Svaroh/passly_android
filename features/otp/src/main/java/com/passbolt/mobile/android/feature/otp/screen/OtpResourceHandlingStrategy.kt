package net.svaroh.passly.feature.otp.screen

import net.svaroh.passly.core.navigation.AppContext
import net.svaroh.passly.feature.home.screen.ResourceHandlingStrategy
import net.svaroh.passly.feature.home.screen.ShowSuggestedModel
import net.svaroh.passly.ui.ResourceUiModel

class OtpResourceHandlingStrategy(
    private val onItemClick: (ResourceUiModel) -> Unit,
) : ResourceHandlingStrategy {
    override val appContext: AppContext = AppContext.APP

    override fun resourceItemClick(resourceModel: ResourceUiModel) {
        onItemClick(resourceModel)
    }

    override fun shouldShowResourceMoreMenu() = true

    override fun shouldShowCloseButton() = false

    override fun showSuggestedModel() = ShowSuggestedModel.DoNotShow

    override fun resourcePostCreateAction(resourceId: String) {
        // no-op
    }

    override fun shouldShowFolderMoreMenu() = true
}
