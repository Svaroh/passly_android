package net.svaroh.passly.core.autofill.system.classification

import net.svaroh.passly.core.navigation.AutofillType
import net.svaroh.passly.ui.ParsedStructure

sealed class FillClassification {
    abstract val type: AutofillType
    abstract val allFields: List<ParsedStructure>

    val anchorDomain: String?
        get() = allFields.firstOrNull { it.domain != null }?.domain

    data class Credentials(
        val username: ParsedStructure,
        val password: ParsedStructure,
    ) : FillClassification() {
        override val type = AutofillType.CREDENTIALS
        override val allFields = listOf(username, password)
    }

    data class CredentialsAndTotp(
        val username: ParsedStructure,
        val password: ParsedStructure,
        val totp: ParsedStructure,
    ) : FillClassification() {
        override val type = AutofillType.CREDENTIALS_AND_TOTP
        override val allFields = listOf(username, password, totp)
    }

    data class Totp(
        val totp: ParsedStructure,
    ) : FillClassification() {
        override val type = AutofillType.TOTP
        override val allFields = listOf(totp)
    }
}
