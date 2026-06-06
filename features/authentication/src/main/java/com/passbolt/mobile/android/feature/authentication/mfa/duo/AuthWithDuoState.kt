package net.svaroh.passly.feature.authentication.mfa.duo

data class AuthWithDuoState(
    val showProgress: Boolean = false,
    val hasOtherProvider: Boolean = false,
    val showDuoWebViewSheet: Boolean = false,
    val duoPromptUrl: String = "",
)
