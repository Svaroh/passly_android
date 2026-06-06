package net.svaroh.passly.feature.main.mainscreen

import net.svaroh.passly.common.datarefresh.DataRefreshTrackingFlow
import net.svaroh.passly.core.autofill.AutofillInformationProvider
import net.svaroh.passly.core.autofill.AutofillInformationProvider.ChromeNativeAutofillStatus.ENABLED
import net.svaroh.passly.core.compose.SideEffectViewModel
import net.svaroh.passly.core.inappreview.InAppReviewInteractor
import net.svaroh.passly.core.navigation.compose.AppNavigator
import net.svaroh.passly.core.navigation.deeplink.BrowserFirstLoginDeepLinkStore
import net.svaroh.passly.feature.main.mainscreen.MainIntent.AppUpdateDownloaded
import net.svaroh.passly.feature.main.mainscreen.MainIntent.CloseChromeNativeAutofill
import net.svaroh.passly.feature.main.mainscreen.MainIntent.GoToSettings
import net.svaroh.passly.feature.main.mainscreen.MainIntent.Resumed
import net.svaroh.passly.feature.main.mainscreen.MainIntent.TabSelected
import net.svaroh.passly.feature.main.mainscreen.MainSideEffect.CheckForAppUpdates
import net.svaroh.passly.feature.main.mainscreen.MainSideEffect.LaunchChromeNativeAutofillDeeplink
import net.svaroh.passly.feature.main.mainscreen.MainSideEffect.PerformFullDataRefresh
import net.svaroh.passly.feature.main.mainscreen.MainSideEffect.ShowSnackbar
import net.svaroh.passly.feature.main.mainscreen.MainSideEffect.TryLaunchReviewFlow
import net.svaroh.passly.feature.main.mainscreen.bottomnavigation.MainBottomNavigationModel
import net.svaroh.passly.feature.main.mainscreen.encouragements.EncouragementsInteractor
import net.svaroh.passly.feature.transferaccounttoanotherdevice.browserfirstlogin.BrowserFirstLoginQrParser
import net.svaroh.passly.feature.transferaccounttoanotherdevice.usecase.CompleteBrowserFirstLoginUseCase
import net.svaroh.passly.featureflags.usecase.GetFeatureFlagsUseCase
import timber.log.Timber

class MainViewModel(
    private val inAppReviewInteractor: InAppReviewInteractor,
    private val dataRefreshTrackingFlow: DataRefreshTrackingFlow,
    private val getFeatureFlagsUseCase: GetFeatureFlagsUseCase,
    private val encouragementsInteractor: EncouragementsInteractor,
    private val autofillInformationProvider: AutofillInformationProvider,
    private val appNavigator: AppNavigator,
    private val browserFirstLoginDeepLinkStore: BrowserFirstLoginDeepLinkStore,
    private val browserFirstLoginQrParser: BrowserFirstLoginQrParser,
    private val completeBrowserFirstLoginUseCase: CompleteBrowserFirstLoginUseCase,
) : SideEffectViewModel<MainState, MainSideEffect>(MainState()) {
    init {
        setupBottomNavigation()
        performFullDataRefresh()
        checkEncouragements()
        emitSideEffect(CheckForAppUpdates)
        checkReviewFlow()
        collectTabSwitchRequests()
        completePendingBrowserFirstLogin()
    }

    fun onIntent(intent: MainIntent) {
        when (intent) {
            AppUpdateDownloaded -> emitSideEffect(ShowSnackbar(SnackbarType.APP_UPDATE_DOWNLOADED))
            GoToSettings -> emitSideEffect(LaunchChromeNativeAutofillDeeplink)
            CloseChromeNativeAutofill -> updateViewState { copy(showChromeNativeAutofillDialog = false) }
            Resumed -> checkChromeNativeAutofillStatus()
            is TabSelected -> updateViewState { copy(selectedTab = intent.tab) }
        }
    }

    private fun setupBottomNavigation() {
        launch {
            val isTotpFeatureFlagEnabled = getFeatureFlagsUseCase.execute(Unit).featureFlags.isTotpAvailable
            updateViewState {
                copy(bottomNavigationModel = MainBottomNavigationModel(isOtpTabVisible = isTotpFeatureFlagEnabled))
            }
        }
    }

    private fun performFullDataRefresh() {
        emitSideEffect(PerformFullDataRefresh)
        launch {
            dataRefreshTrackingFlow.awaitIdle()
            setupBottomNavigation()
        }
    }

    private fun checkEncouragements() {
        if (encouragementsInteractor.shouldShowChromeNativeAutofillEncouragement()) {
            encouragementsInteractor.chromeNativeAutofillEncouragementShown()
            updateViewState { copy(showChromeNativeAutofillDialog = true) }
        }
    }

    private fun checkReviewFlow() {
        if (inAppReviewInteractor.shouldShowInAppReviewFlow()) {
            emitSideEffect(TryLaunchReviewFlow)
            inAppReviewInteractor.inAppReviewFlowShowed()
        }
    }

    private fun checkChromeNativeAutofillStatus() {
        if (viewState.value.showChromeNativeAutofillDialog &&
            autofillInformationProvider.getChromeNativeAutofillStatus() == ENABLED
        ) {
            updateViewState { copy(showChromeNativeAutofillDialog = false) }
            emitSideEffect(ShowSnackbar(SnackbarType.CHROME_NATIVE_AUTOFILL_SETUP_SUCCESS))
        }
    }

    private fun collectTabSwitchRequests() {
        launch {
            appNavigator.tabSwitchRequest.collect { tab ->
                updateViewState { copy(selectedTab = tab) }
            }
        }
    }

    private fun completePendingBrowserFirstLogin() {
        val deepLink = browserFirstLoginDeepLinkStore.consume() ?: return
        Timber.i("[BrowserFirstLogin] Consumed pending deep link")
        val page =
            browserFirstLoginQrParser.parse(deepLink)
                ?: run {
                    Timber.e("[BrowserFirstLogin] Could not parse pending deep link")
                    emitSideEffect(ShowSnackbar(SnackbarType.BROWSER_FIRST_LOGIN_FAILURE))
                    return
        }

        launch {
            Timber.i("[BrowserFirstLogin] Completing pending deep link")
            when (
                val result =
                    completeBrowserFirstLoginUseCase.execute(CompleteBrowserFirstLoginUseCase.Input(page))
            ) {
                CompleteBrowserFirstLoginUseCase.Output.Success ->
                    emitSideEffect(ShowSnackbar(SnackbarType.BROWSER_FIRST_LOGIN_SUCCESS))
                is CompleteBrowserFirstLoginUseCase.Output.DomainMismatch -> {
                    Timber.e(
                        "Browser first-login domain mismatch: QR=%s account=%s",
                        result.qrDomain,
                        result.accountDomain,
                    )
                    emitSideEffect(ShowSnackbar(SnackbarType.BROWSER_FIRST_LOGIN_FAILURE))
                }
                is CompleteBrowserFirstLoginUseCase.Output.Failure -> {
                    Timber.e("Browser first-login failed: %s", result.message)
                    emitSideEffect(ShowSnackbar(SnackbarType.BROWSER_FIRST_LOGIN_FAILURE))
                }
            }
        }
    }
}
