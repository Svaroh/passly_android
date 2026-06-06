package net.svaroh.passly.feature.settings.screen.appsettings.autofill.encourageaccessibility

data class EncourageAccessibilityState(
    val isAccessibilityServiceEnabled: Boolean = false,
    val isOverlayPermissionGranted: Boolean = false,
)
