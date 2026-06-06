package net.svaroh.passly.feature.authentication.mfa.duo.duowebviewsheet

data class DuoState(
    val state: String?,
    val duoCode: String?,
)
