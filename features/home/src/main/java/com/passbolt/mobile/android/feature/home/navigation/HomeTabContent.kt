package net.svaroh.passly.feature.home.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.svaroh.passly.core.navigation.compose.HomeNavigation
import net.svaroh.passly.domain.preferences.mapper.toHomeDisplayViewModel
import net.svaroh.passly.domain.preferences.usecase.GetHomeDisplayViewPreferencesUseCase
import org.koin.compose.koinInject

@Composable
fun HomeTabContent() {
    val getHomeDisplayViewPreferencesUseCase: GetHomeDisplayViewPreferencesUseCase = koinInject()
    val initialHomeDisplay =
        remember {
            val prefs = getHomeDisplayViewPreferencesUseCase.execute(Unit)
            prefs.userSetHomeView.toHomeDisplayViewModel(prefs.lastUsedHomeView)
        }
    HomeNavigation(initialHomeDisplay)
}
