package net.svaroh.passly.feature.resourceform.main

import android.content.Context
import net.svaroh.passly.common.extension.toSingleLine
import net.svaroh.passly.feature.resourceform.additionalsecrets.note.NoteValidationError
import net.svaroh.passly.feature.resourceform.additionalsecrets.note.NoteValidationError.MaxLengthExceeded
import net.svaroh.passly.feature.resourceform.additionalsecrets.totp.TotpSecretValidationError
import net.svaroh.passly.feature.resourceform.additionalsecrets.totp.TotpSecretValidationError.MustBeBase32
import net.svaroh.passly.feature.resourceform.additionalsecrets.totp.TotpSecretValidationError.MustNotBeEmpty
import net.svaroh.passly.ui.LeadingContentType.CUSTOM_FIELDS
import net.svaroh.passly.ui.LeadingContentType.PASSWORD
import net.svaroh.passly.ui.LeadingContentType.STANDALONE_NOTE
import net.svaroh.passly.ui.LeadingContentType.TOTP
import net.svaroh.passly.ui.ResourceFormMode
import net.svaroh.passly.ui.ResourceFormMode.Create
import net.svaroh.passly.ui.ResourceFormMode.Edit
import net.svaroh.passly.core.localization.R as LocalizationR

internal fun getScreenTitle(
    context: Context,
    state: ResourceFormState,
): String =
    when (val mode = state.mode) {
        is Create ->
            when (state.leadingContentType) {
                TOTP -> context.getString(LocalizationR.string.resource_form_create_totp)
                PASSWORD -> context.getString(LocalizationR.string.resource_form_create_password)
                CUSTOM_FIELDS -> context.getString(LocalizationR.string.resource_form_create_custom_fields)
                STANDALONE_NOTE -> context.getString(LocalizationR.string.resource_form_create_note)
                null -> ""
            }
        is Edit -> context.getString(LocalizationR.string.resource_form_edit_resource, mode.resourceName.toSingleLine())
        null -> ""
    }

internal fun getPrimaryButtonText(
    context: Context,
    mode: ResourceFormMode?,
): String =
    when (mode) {
        is Create -> context.getString(LocalizationR.string.resource_form_create)
        is Edit -> context.getString(LocalizationR.string.resource_form_save)
        null -> ""
    }

internal fun getTotpSecretErrorMessage(
    context: Context,
    error: TotpSecretValidationError,
): String =
    when (error) {
        MustNotBeEmpty -> context.getString(LocalizationR.string.validation_is_required)
        MustBeBase32 -> context.getString(LocalizationR.string.validation_invalid_totp_secret)
    }

internal fun getNoteErrorMessage(
    context: Context,
    error: NoteValidationError,
): String =
    when (error) {
        is MaxLengthExceeded -> context.getString(LocalizationR.string.validation_max_length, error.maxLength)
    }
