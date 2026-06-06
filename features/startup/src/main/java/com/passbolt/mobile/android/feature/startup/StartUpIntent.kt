package net.svaroh.passly.feature.startup

sealed interface StartUpIntent {
    data object AcknowledgeDeprecatedOsWarning : StartUpIntent

    data object HideDeprecatedOsWarning : StartUpIntent
}
