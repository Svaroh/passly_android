package net.svaroh.passly.ui

data class CreateResourceMenuModel(
    val isPasswordEnabled: Boolean,
    val isTotpEnabled: Boolean,
    val isFolderEnabled: Boolean,
    val isNoteEnabled: Boolean,
)
