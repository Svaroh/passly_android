package net.svaroh.passly.feature.resourceform.additionalsecrets.pincode

import android.content.Context
import net.svaroh.passly.ui.ResourceFormMode
import net.svaroh.passly.ui.ResourceFormMode.Create
import net.svaroh.passly.ui.ResourceFormMode.Edit
import net.svaroh.passly.core.localization.R as LocalizationR

internal fun getScreenTitle(
    context: Context,
    resourceFormMode: ResourceFormMode?,
): String =
    when (resourceFormMode) {
        is Create -> context.getString(LocalizationR.string.resource_form_create_pin_code)
        is Edit -> context.getString(LocalizationR.string.resource_form_edit_resource, resourceFormMode.resourceName)
        null -> ""
    }

internal fun getPinCodeErrorMessage(
    context: Context,
    errors: List<PinCodeValidationError>,
): String =
    when (val error = errors.first()) {
        is PinCodeValidationError.TooShort ->
            context.getString(LocalizationR.string.resource_form_pin_code_validation_too_short, error.minLength)
        is PinCodeValidationError.TooLong ->
            context.getString(LocalizationR.string.resource_form_pin_code_validation_too_long, error.maxLength)
    }
