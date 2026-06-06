package net.svaroh.passly.feature.autofill.resources.datasetstrategy

import net.svaroh.passly.core.autofill.accessibility.AccessibilityCommunicator

class ReturnAccessibilityDataset(
    private val autofillCallback: AutofillCallback,
) : ReturnAutofillDatasetStrategy {
    override fun returnDataset(
        username: String,
        password: String,
        uri: String?,
    ) {
        AccessibilityCommunicator.lastCredentials =
            AccessibilityCommunicator.Credentials(
                username,
                password,
                uri,
            )
        autofillCallback.finishAutofill()
    }
}
