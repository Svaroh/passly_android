package net.svaroh.passly.feature.autofill.resources

import net.svaroh.passly.ui.ResourceUiModel

sealed interface AutofillResourcesIntent {
    data object UserAuthenticated : AutofillResourcesIntent

    data class SelectAutofillItem(
        val resourceModel: ResourceUiModel,
    ) : AutofillResourcesIntent

    data class NewResourceCreated(
        val resourceId: String,
    ) : AutofillResourcesIntent
}
