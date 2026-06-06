package net.svaroh.passly.feature.home.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.svaroh.passly.core.navigation.compose.HomeNavigation
import net.svaroh.passly.core.preferences.usecase.GetHomeDisplayViewPrefsUseCase
import net.svaroh.passly.mappers.HomeDisplayViewMapper
import org.koin.compose.koinInject

@Composable
fun HomeTabContent() {
    val filterPreferencesUseCase: GetHomeDisplayViewPrefsUseCase = koinInject()
    val homeDisplayMapper: HomeDisplayViewMapper = koinInject()
    val initialHomeDisplay =
        remember {
            val prefs = filterPreferencesUseCase.execute(Unit)
            homeDisplayMapper.map(prefs.userSetHomeView, prefs.lastUsedHomeView)
        }
    HomeNavigation(initialHomeDisplay)
}
