package com.passbolt.mobile.android.feature.main.mainscreen

import com.passbolt.mobile.android.common.datarefresh.DataRefreshTrackingFlow
import com.passbolt.mobile.android.core.autofill.AutofillInformationProvider
import com.passbolt.mobile.android.core.autofill.AutofillInformationProvider.ChromeNativeAutofillStatus.ENABLED
import com.passbolt.mobile.android.core.compose.SideEffectViewModel
import com.passbolt.mobile.android.core.inappreview.InAppReviewInteractor
import com.passbolt.mobile.android.core.navigation.compose.AppNavigator
import com.passbolt.mobile.android.core.navigation.deeplink.BrowserFirstLoginDeepLinkStore
import com.passbolt.mobile.android.feature.main.mainscreen.MainIntent.AppUpdateDownloaded
import com.passbolt.mobile.android.feature.main.mainscreen.MainIntent.CloseChromeNativeAutofill
import com.passbolt.mobile.android.feature.main.mainscreen.MainIntent.GoToSettings
import com.passbolt.mobile.android.feature.main.mainscreen.MainIntent.Resumed
import com.passbolt.mobile.android.feature.main.mainscreen.MainIntent.TabSelected
import com.passbolt.mobile.android.feature.main.mainscreen.MainSideEffect.CheckForAppUpdates
import com.passbolt.mobile.android.feature.main.mainscreen.MainSideEffect.LaunchChromeNativeAutofillDeeplink
import com.passbolt.mobile.android.feature.main.mainscreen.MainSideEffect.PerformFullDataRefresh
import com.passbolt.mobile.android.feature.main.mainscreen.MainSideEffect.ShowSnackbar
import com.passbolt.mobile.android.feature.main.mainscreen.MainSideEffect.TryLaunchReviewFlow
import com.passbolt.mobile.android.feature.main.mainscreen.bottomnavigation.MainBottomNavigationModel
import com.passbolt.mobile.android.feature.main.mainscreen.encouragements.EncouragementsInteractor
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.browserfirstlogin.BrowserFirstLoginQrParser
import com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.usecase.CompleteBrowserFirstLoginUseCase
import com.passbolt.mobile.android.featureflags.usecase.GetFeatureFlagsUseCase
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
