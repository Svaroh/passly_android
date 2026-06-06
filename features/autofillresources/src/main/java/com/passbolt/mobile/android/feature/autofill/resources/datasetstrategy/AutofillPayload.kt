package net.svaroh.passly.feature.autofill.resources.datasetstrategy

data class AutofillPayload(
    val username: String?,
    val password: String?,
    val totpCode: String?,
    val uri: String?,
)
