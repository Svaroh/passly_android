package net.svaroh.passly.ui

import java.util.UUID

data class SessionKeyIdentifier(
    val foreignModel: String,
    val foreignId: UUID,
)
