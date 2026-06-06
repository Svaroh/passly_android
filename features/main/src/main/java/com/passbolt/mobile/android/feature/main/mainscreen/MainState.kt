package net.svaroh.passly.feature.main.mainscreen

import net.svaroh.passly.core.navigation.compose.BottomTab
import net.svaroh.passly.feature.main.mainscreen.bottomnavigation.MainBottomNavigationModel

data class MainState(
    val bottomNavigationModel: MainBottomNavigationModel? = null,
    val showChromeNativeAutofillDialog: Boolean = false,
    val selectedTab: BottomTab = BottomTab.HOME,
)
