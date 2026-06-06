package net.svaroh.passly.core.autofill.conflict

import net.svaroh.passly.common.autofill.DetectAutofillConflict
import net.svaroh.passly.core.autofill.AutofillInformationProvider

class DetectSystemAutofillConflict(
    private val autofillInformationProvider: AutofillInformationProvider,
) : DetectAutofillConflict {
    override fun invoke(): Boolean {
        val isAccessibilityAutofillChecked = autofillInformationProvider.isAccessibilityAutofillSetup()
        val isNativeAutofillChecked =
            autofillInformationProvider.isAutofillServiceSupported() &&
                autofillInformationProvider.isPassboltAutofillServiceSet()

        return isAccessibilityAutofillChecked && isNativeAutofillChecked
    }
}
