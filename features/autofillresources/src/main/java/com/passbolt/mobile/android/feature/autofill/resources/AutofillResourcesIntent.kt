package net.svaroh.passly.feature.autofill.resources

import net.svaroh.passly.ui.ResourceModel

sealed interface AutofillResourcesIntent {
    data object UserAuthenticated : AutofillResourcesIntent

    data class SelectAutofillItem(
        val resourceModel: ResourceModel,
    ) : AutofillResourcesIntent

    data class NewResourceCreated(
        val resourceId: String,
    ) : AutofillResourcesIntent
}
