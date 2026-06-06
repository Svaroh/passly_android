package net.svaroh.passly.feature.main.mainscreen

import net.svaroh.passly.core.navigation.compose.BottomTab

sealed interface MainIntent {
    data object AppUpdateDownloaded : MainIntent

    data object GoToSettings : MainIntent

    data object CloseChromeNativeAutofill : MainIntent

    data object Resumed : MainIntent

    data class TabSelected(
        val tab: BottomTab,
    ) : MainIntent
}
