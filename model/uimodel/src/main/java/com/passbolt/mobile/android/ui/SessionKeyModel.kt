package net.svaroh.passly.ui

import java.time.ZonedDateTime

data class SessionKeyModel(
    val sessionKey: String,
    val modified: ZonedDateTime,
)
