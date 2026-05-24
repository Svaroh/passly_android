package com.passbolt.mobile.android.feature.transferaccounttoanotherdevice.browserfirstlogin

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf

internal fun Module.browserFirstLoginScanModule() {
    viewModelOf(::BrowserFirstLoginScanViewModel)
}
