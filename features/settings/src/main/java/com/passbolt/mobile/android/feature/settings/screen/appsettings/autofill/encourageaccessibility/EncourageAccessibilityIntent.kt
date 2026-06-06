package net.svaroh.passly.feature.settings.screen.appsettings.autofill.encourageaccessibility

sealed interface EncourageAccessibilityIntent {
    data object RefreshState : EncourageAccessibilityIntent

    data object EnableAccessibilityService : EncourageAccessibilityIntent

    data object GrantOverlayPermission : EncourageAccessibilityIntent

    data object Close : EncourageAccessibilityIntent
}
