package com.passbolt.mobile.android.ui

import kotlinx.serialization.Serializable

@Serializable
sealed interface ConfirmPermissionsMode {
    @Serializable
    data class Create(
        val folderId: String,
    ) : ConfirmPermissionsMode

    @Serializable
    data class Edit(
        val resourceId: String,
    ) : ConfirmPermissionsMode
}
