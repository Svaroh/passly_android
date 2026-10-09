package net.svaroh.passly.feature.settings.screen

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf

fun Module.settingsModule() {
    viewModelOf(::SettingsViewModel)
}
