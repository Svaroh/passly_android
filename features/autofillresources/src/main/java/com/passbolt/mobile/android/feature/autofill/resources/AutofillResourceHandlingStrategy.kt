package net.svaroh.passly.feature.autofill.resources

import net.svaroh.passly.core.navigation.AppContext
import net.svaroh.passly.feature.home.screen.ResourceHandlingStrategy
import net.svaroh.passly.feature.home.screen.ShowSuggestedModel
import net.svaroh.passly.ui.ResourceModel

class AutofillResourceHandlingStrategy(
    private val autofillUri: String?,
    private val onItemClick: (ResourceModel) -> Unit,
    private val onResourceCreated: (String) -> Unit,
) : ResourceHandlingStrategy {
    override val appContext: AppContext = AppContext.AUTOFILL

    override fun resourceItemClick(resourceModel: ResourceModel) {
        onItemClick(resourceModel)
    }

    override fun shouldShowResourceMoreMenu() = false

    override fun shouldShowFolderMoreMenu() = false

    override fun shouldShowCloseButton() = true

    override fun showSuggestedModel() = autofillUri?.let { ShowSuggestedModel.Show(it) } ?: ShowSuggestedModel.DoNotShow

    override fun resourcePostCreateAction(resourceId: String) {
        onResourceCreated(resourceId)
    }
}
