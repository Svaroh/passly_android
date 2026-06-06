package net.svaroh.passly.feature.main.mainscreen

import PassboltTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.svaroh.passly.core.navigation.compose.AppNavigator
import net.svaroh.passly.core.navigation.compose.BottomTab.HOME
import net.svaroh.passly.core.navigation.compose.BottomTab.OTP
import net.svaroh.passly.core.navigation.compose.BottomTab.SETTINGS
import net.svaroh.passly.core.navigation.compose.OtpNavigation
import net.svaroh.passly.core.navigation.compose.SettingsNavigation
import net.svaroh.passly.core.navigation.compose.keys.HomeNavigationKey.Home
import net.svaroh.passly.core.navigation.compose.keys.OtpNavigationKey.Otp
import net.svaroh.passly.core.navigation.compose.keys.SettingsNavigationKey.SettingsMain
import net.svaroh.passly.feature.authentication.compose.AuthenticationHandler
import net.svaroh.passly.feature.home.navigation.HomeTabContent
import net.svaroh.passly.feature.main.mainscreen.MainIntent.CloseChromeNativeAutofill
import net.svaroh.passly.feature.main.mainscreen.MainIntent.GoToSettings
import net.svaroh.passly.feature.main.mainscreen.MainIntent.TabSelected
import net.svaroh.passly.feature.main.mainscreen.encouragements.chromenativeautofill.EncourageChromeNativeAutofillDialog
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = koinViewModel(),
    appNavigator: AppNavigator = koinInject(),
) {
    val state by mainViewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    AuthenticationHandler()

    MainScreenEffects(
        sideEffect = mainViewModel.sideEffect,
        onIntent = mainViewModel::onIntent,
        snackbarHostState = snackbarHostState,
    )

    val isBottomNavVisible by remember {
        appNavigator.currentBackStackItem
            .map { key -> key?.let { BOTTOM_NAV_ROOT_TYPES.any { it.isInstance(key) } } ?: true }
    }.collectAsStateWithLifecycle(initialValue = true)

    PassboltTheme {
        Scaffold(
            modifier = modifier,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                AnimatedVisibility(
                    visible = isBottomNavVisible,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it },
                ) {
                    MainBottomNavigationBar(
                        selectedTab = state.selectedTab,
                        onTabSelect = { mainViewModel.onIntent(TabSelected(it)) },
                        isOtpTabVisible = state.bottomNavigationModel?.isOtpTabVisible ?: false,
                    )
                }
            },
        ) { innerPadding ->
            Box(
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding),
            ) {
                when (state.selectedTab) {
                    HOME -> HomeTabContent()
                    OTP -> OtpNavigation()
                    SETTINGS -> SettingsNavigation()
                }
            }
        }

        if (state.showChromeNativeAutofillDialog) {
            EncourageChromeNativeAutofillDialog(
                onGoToChromeSettings = { mainViewModel.onIntent(GoToSettings) },
                onClose = { mainViewModel.onIntent(CloseChromeNativeAutofill) },
            )
        }
    }
}

private val BOTTOM_NAV_ROOT_TYPES =
    listOf(
        Home::class,
        Otp::class,
        SettingsMain::class,
    )
