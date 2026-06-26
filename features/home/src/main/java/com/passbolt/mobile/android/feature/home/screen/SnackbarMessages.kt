package net.svaroh.passly.feature.home.screen

import android.content.Context
import net.svaroh.passly.common.extension.toSingleLine
import net.svaroh.passly.feature.home.screen.SnackbarErrorType.CANNOT_UPDATE_WITH_CURRENT_CONFIGURATION
import net.svaroh.passly.feature.home.screen.SnackbarErrorType.DECRYPTION_FAILURE
import net.svaroh.passly.feature.home.screen.SnackbarErrorType.ENCRYPTION_FAILURE
import net.svaroh.passly.feature.home.screen.SnackbarErrorType.ERROR
import net.svaroh.passly.feature.home.screen.SnackbarErrorType.FAILED_TO_DELETE_PASSKEY
import net.svaroh.passly.feature.home.screen.SnackbarErrorType.FAILED_TO_DELETE_RESOURCE
import net.svaroh.passly.feature.home.screen.SnackbarErrorType.FAILED_TO_REFRESH_DATA
import net.svaroh.passly.feature.home.screen.SnackbarErrorType.FAILED_TO_TRUST_METADATA_KEY
import net.svaroh.passly.feature.home.screen.SnackbarErrorType.FAILED_TO_VERIFY_METADATA_KEYS
import net.svaroh.passly.feature.home.screen.SnackbarErrorType.FETCH_FAILURE
import net.svaroh.passly.feature.home.screen.SnackbarErrorType.NO_SHARED_KEY_ACCESS
import net.svaroh.passly.feature.home.screen.SnackbarErrorType.RESOURCE_SCHEMA_INVALID
import net.svaroh.passly.feature.home.screen.SnackbarErrorType.SECRET_SCHEMA_INVALID
import net.svaroh.passly.feature.home.screen.SnackbarErrorType.TOGGLE_FAVOURITE_FAILURE
import net.svaroh.passly.feature.home.screen.SnackbarSuccessType.FOLDER_CREATED
import net.svaroh.passly.feature.home.screen.SnackbarSuccessType.METADATA_KEY_IS_TRUSTED
import net.svaroh.passly.feature.home.screen.SnackbarSuccessType.PASSKEY_DELETED
import net.svaroh.passly.feature.home.screen.SnackbarSuccessType.RESOURCE_CREATED
import net.svaroh.passly.feature.home.screen.SnackbarSuccessType.RESOURCE_DELETED
import net.svaroh.passly.feature.home.screen.SnackbarSuccessType.RESOURCE_EDITED
import net.svaroh.passly.feature.home.screen.SnackbarSuccessType.RESOURCE_SHARED
import net.svaroh.passly.feature.home.screen.ToastType.WAIT_FOR_DATA_REFRESH_FINISH
import net.svaroh.passly.core.localization.R as LocalizationR

internal fun getToastMessage(
    context: Context,
    type: ToastType,
): String =
    when (type) {
        WAIT_FOR_DATA_REFRESH_FINISH -> context.getString(LocalizationR.string.home_please_wait_for_refresh)
    }

internal fun getSuccessMessage(
    context: Context,
    type: SnackbarSuccessType,
    additionalSuccessMessage: String? = null,
): String =
    when (type) {
        RESOURCE_EDITED ->
            context.getString(
                LocalizationR.string.common_message_resource_edited,
                additionalSuccessMessage.orEmpty().toSingleLine(),
            )
        RESOURCE_CREATED -> context.getString(LocalizationR.string.resource_form_create_success)
        RESOURCE_DELETED ->
            context.getString(
                LocalizationR.string.common_message_resource_deleted,
                additionalSuccessMessage.orEmpty().toSingleLine(),
            )
        PASSKEY_DELETED -> context.getString(LocalizationR.string.passkey_deleted)
        METADATA_KEY_IS_TRUSTED -> context.getString(LocalizationR.string.common_metadata_key_is_trusted)
        RESOURCE_SHARED -> context.getString(LocalizationR.string.common_message_resource_shared)
        FOLDER_CREATED ->
            context.getString(
                LocalizationR.string.common_message_folder_created,
                additionalSuccessMessage.orEmpty().toSingleLine(),
            )
    }

internal fun getErrorMessage(
    context: Context,
    type: SnackbarErrorType,
    additionalErrorMessage: String? = null,
): String =
    when (type) {
        DECRYPTION_FAILURE -> context.getString(LocalizationR.string.common_decryption_failure)
        FETCH_FAILURE -> context.getString(LocalizationR.string.common_fetch_failure)
        ERROR -> context.getString(LocalizationR.string.common_failure_format, additionalErrorMessage.orEmpty())
        FAILED_TO_DELETE_RESOURCE -> context.getString(LocalizationR.string.delete_failure)
        FAILED_TO_DELETE_PASSKEY -> context.getString(LocalizationR.string.passkey_failed_to_delete)
        ENCRYPTION_FAILURE -> context.getString(LocalizationR.string.common_encryption_failure)
        RESOURCE_SCHEMA_INVALID -> context.getString(LocalizationR.string.common_json_schema_resource_validation_error)
        SECRET_SCHEMA_INVALID -> context.getString(LocalizationR.string.common_json_schema_secret_validation_error)
        CANNOT_UPDATE_WITH_CURRENT_CONFIGURATION ->
            context.getString(
                LocalizationR.string.common_cannot_create_resource_with_current_config,
            )
        FAILED_TO_VERIFY_METADATA_KEYS -> context.getString(LocalizationR.string.common_metadata_key_verification_failure)
        FAILED_TO_TRUST_METADATA_KEY -> context.getString(LocalizationR.string.common_metadata_key_trust_failed)
        FAILED_TO_REFRESH_DATA -> context.getString(LocalizationR.string.common_data_refresh_error)
        NO_SHARED_KEY_ACCESS -> context.getString(LocalizationR.string.common_lack_shared_key_access)
        TOGGLE_FAVOURITE_FAILURE -> context.getString(LocalizationR.string.favourites_failure)
    }
