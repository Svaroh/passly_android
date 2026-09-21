package com.passbolt.mobile.android.feature.startup

sealed interface StartUpIntent {
    data object AcknowledgeDeprecatedOsWarning : StartUpIntent

    data object HideDeprecatedOsWarning : StartUpIntent
}
