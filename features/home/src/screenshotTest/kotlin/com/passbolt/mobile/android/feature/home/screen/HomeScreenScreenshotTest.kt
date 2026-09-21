package com.passbolt.mobile.android.feature.home.screen

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.android.tools.screenshot.PreviewTest
import com.passbolt.mobile.android.common.ExternalDeeplinkHandler
import com.passbolt.mobile.android.core.mvp.coroutinecontext.CoroutineLaunchContext
import com.passbolt.mobile.android.core.navigation.AppContext
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.ui.screenshot.PassboltEdgeToEdgePreviewWrapper
import com.passbolt.mobile.android.core.ui.screenshot.ensureScreenshotKoinStarted
import com.passbolt.mobile.android.domain.resources.resourceicon.ResourceIconProvider
import com.passbolt.mobile.android.ui.ResourceUiModel
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private const val SCREEN_WIDTH_DP = 360
private const val SCREEN_HEIGHT_DP = 800

private object ScreenshotCoroutineLaunchContext : CoroutineLaunchContext {
    override val ui: CoroutineContext = EmptyCoroutineContext
    override val default: CoroutineContext = EmptyCoroutineContext
    override val io: CoroutineContext = EmptyCoroutineContext
}

private object ScreenshotResourceHandlingStrategy : ResourceHandlingStrategy {
    override val appContext: AppContext = AppContext.APP

    override fun resourceItemClick(resourceModel: ResourceUiModel) {}

    override fun shouldShowResourceMoreMenu(): Boolean = true

    override fun shouldShowCloseButton(): Boolean = false

    override fun showSuggestedModel(): ShowSuggestedModel = ShowSuggestedModel.DoNotShow

    override fun resourcePostCreateAction(resourceId: String) {}

    override fun shouldShowFolderMoreMenu(): Boolean = false
}

private val screenshotKoin =
    ensureScreenshotKoinStarted(
        module {
            single<CoroutineLaunchContext> { ScreenshotCoroutineLaunchContext }
            single { ResourceIconProvider(get()) }
        },
    )

@Composable
private fun HomeScreenUnderTest(state: HomeState) {
    HomeScreen(
        state = state,
        onIntent = {},
        snackbarHostState = SnackbarHostState(),
        navigator = AppNavigator(ExternalDeeplinkHandler()),
        resourceHandlingStrategy = ScreenshotResourceHandlingStrategy,
    )
}

@PreviewTest
@Preview(showBackground = true, widthDp = SCREEN_WIDTH_DP, heightDp = SCREEN_HEIGHT_DP)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun HomeScreenScreenshot() {
    HomeScreenUnderTest(state = HomeState(canCreateResource = true))
}

@PreviewTest
@Preview(showBackground = true, widthDp = SCREEN_WIDTH_DP, heightDp = SCREEN_HEIGHT_DP, uiMode = UI_MODE_NIGHT_YES)
@PreviewWrapper(PassboltEdgeToEdgePreviewWrapper::class)
@Composable
fun HomeScreenDarkThemeScreenshot() {
    HomeScreenUnderTest(state = HomeState(canCreateResource = true))
}
