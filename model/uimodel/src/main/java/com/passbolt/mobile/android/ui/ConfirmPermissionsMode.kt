package net.svaroh.passly.ui

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

    @Serializable
    data class Share(
        val resourceId: String,
    ) : ConfirmPermissionsMode
}
