package net.svaroh.passly.ui

import android.view.autofill.AutofillId

data class ParsedStructures(
    val structures: Set<ParsedStructure>,
) {
    val hasDifferentDomains: Boolean
        get() = structures.mapNotNull { it.domain }.toSet().size > 1
}

data class ParsedStructure(
    var id: AutofillId,
    val autofillHints: List<String>? = null,
    val inputType: Int? = null,
    val domain: String? = null,
    val packageId: String? = null,
)
