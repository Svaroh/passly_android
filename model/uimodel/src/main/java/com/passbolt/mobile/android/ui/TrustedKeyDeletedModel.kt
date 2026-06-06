package net.svaroh.passly.ui

data class TrustedKeyDeletedModel(
    val keyFingerprint: String,
    val signedUsername: String,
    val signedName: String,
    val modificationKind: MetadataKeyModification = MetadataKeyModification.DELETION,
)
